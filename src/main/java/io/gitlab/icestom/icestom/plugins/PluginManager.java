package io.gitlab.icestom.icestom.plugins;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventHandler;
import net.minestom.server.event.EventNode;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class PluginManager implements EventHandler<Event> {

    private static final int API_VERSION = 0;
    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    private final Path pluginFolder;

    private final Map<String, IceStomPlugin> loadedPlugins = new LinkedHashMap<>();
    private final EventNode<Event> eventNode = EventNode.all("plugin_manager");

    public PluginManager(Path pluginFolder) {
        this.pluginFolder = pluginFolder;
    }

    public void loadPlugins() throws IOException, PluginLoadException {
        Files.createDirectories(pluginFolder);

        String devPath = System.getProperty("io.gitlab.icestom.development");

        List<PluginSource> sources = (devPath != null && !devPath.isBlank())
                ? List.of(devSource(Path.of(devPath)))
                : discoverJarSources();

        Map<String, PluginSource> byId = indexSources(sources);

        log.info("Loading {} plugins!", byId.size());

        for (PluginSource source : byId.values()) {
            try {
                IceStomPlugin loaded = instantiate(source);
                loadedPlugins.put(source.descriptor.id, loaded);
                log.info("Loaded plugin '{}' v{}", source.descriptor.id, source.descriptor.version);
            } catch (Throwable t) {
                log.error("Plugin '{}' threw during load ", source.descriptor.id, t);
            }
        }

        for (PluginSource source : byId.values()) {
            IceStomPlugin loaded = loadedPlugins.get(source.descriptor.id);
            if (loaded == null) continue;
            enable(source.descriptor.id, loaded);
        }
    }

    private List<PluginSource> discoverJarSources() throws IOException {
        List<PluginSource> sources = new ArrayList<>();

        try (Stream<Path> files = Files.list(pluginFolder)) {
            for (Path jar : files.filter(path -> path.toString().endsWith(".jar")).toList()) {
                try {
                    sources.add(jarSource(jar));
                } catch (PluginLoadException e) {
                    log.error("Failed to read plugin descriptor from {}: {}", jar, e.getMessage());
                }
            }
        }

        return sources;
    }

    private Map<String, PluginSource> indexSources(List<PluginSource> sources) throws PluginLoadException {
        Map<String, PluginSource> byId = new LinkedHashMap<>();

        for (PluginSource source : sources) {
            try (InputStream in = source.openDescriptor()) {
                source.descriptor = PluginDescriptor.fromInputStream(in);
            } catch (IOException e) {
                throw new PluginLoadException("Failed to read plugin.toml from " + source.describe() + ": " + e);
            }

            if (source.descriptor.api != API_VERSION) {
                throw new PluginLoadException("Plugin " + source.descriptor.id + " supports api version "
                        + source.descriptor.api + " but server supports " + API_VERSION);
            }

            if (byId.containsKey(source.descriptor.id)) {
                log.warn("Duplicated plugin id '{}', not loading 2nd plugin", source.descriptor.id);
                continue;
            }

            byId.put(source.descriptor.id, source);
        }

        return byId;
    }

    private IceStomPlugin instantiate(PluginSource source) throws PluginLoadException {
        PluginDescriptor descriptor = source.descriptor;
        ClassLoader parentClassLoader = PluginManager.class.getClassLoader();

        try {
            PluginClassLoader classLoader =
                    new PluginClassLoader(descriptor.id, source.classpath(), parentClassLoader);

            Class<?> mainClass = Class.forName(descriptor.entrypoint, true, classLoader);

            if (!IceStomPlugin.class.isAssignableFrom(mainClass)) {
                throw new PluginLoadException(descriptor.id + "'s Entrypoint " + descriptor.entrypoint
                        + " does not implement IceStomPlugin");
            }

            Constructor<?> ctor = mainClass.getDeclaredConstructor();
            ctor.setAccessible(true);

            return (IceStomPlugin) ctor.newInstance();

        } catch (ReflectiveOperationException e) {
            throw new PluginLoadException("Failed to instantiate plugin '" + descriptor.id + "': " + e);
        }
    }

    private void enable(String id, IceStomPlugin plugin) {
        try {
            EventNode<Event> pluginEventNode = EventNode.all("plugin_" + id);
            eventNode.addChild(pluginEventNode);

            log.info("Enabling plugin '{}'", id);
            plugin.onEnable(pluginEventNode);
        } catch (Throwable t) {
            log.error("Plugin '{}' threw during onEnable()", id, t);
        }
    }

    private PluginSource jarSource(Path jarPath) throws PluginLoadException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            if (jar.getJarEntry("plugin.toml") == null) {
                throw new PluginLoadException("No plugin.toml found in " + jarPath);
            }
        } catch (IOException e) {
            throw new PluginLoadException("Could not open jar " + jarPath + " " + e);
        }

        return new PluginSource(
                "jar " + jarPath,
                () -> new URL[] { toUrl(jarPath) },
                () -> openFromJar(jarPath)
        );
    }

    private PluginSource devSource(Path devPath) throws PluginLoadException {
        if (!Files.isDirectory(devPath)) {
            throw new PluginLoadException("-io.gitlab.icestom.development=" + devPath + " is not a directory");
        }

        Path resourcesDir = siblingResourcesDir(devPath);

        Path descriptorFile = devPath.resolve("plugin.toml");
        if (!Files.isRegularFile(descriptorFile) && resourcesDir != null) {
            descriptorFile = resourcesDir.resolve("plugin.toml");
        }
        if (!Files.isRegularFile(descriptorFile)) {
            throw new PluginLoadException("Could not find plugin.toml in " + devPath
                    + (resourcesDir != null ? " or " + resourcesDir : ""));
        }
        final Path resolvedDescriptorFile = descriptorFile;

        List<URL> classpath = new ArrayList<>();
        classpath.add(toUrl(devPath));
        if (resourcesDir != null) {
            classpath.add(toUrl(resourcesDir));
        }

        log.info("Loading dev plugin from {}", devPath);

        return new PluginSource(
                "dev path " + devPath,
                () -> classpath.toArray(URL[]::new),
                () -> Files.newInputStream(resolvedDescriptorFile)
        );
    }

    private static Path siblingResourcesDir(Path devPath) {
        Path classesRoot = devPath.getParent() != null ? devPath.getParent().getParent() : null;
        if (classesRoot == null || classesRoot.getParent() == null) return null;

        Path candidate = classesRoot.getParent().resolve("resources").resolve("main");
        return Files.isDirectory(candidate) ? candidate : null;
    }

    private static InputStream openFromJar(Path jarPath) throws IOException {
        JarFile jar = new JarFile(jarPath.toFile());
        try {
            JarEntry entry = jar.getJarEntry("plugin.toml");
            if (entry == null) {
                throw new IOException("No " + "plugin.toml" + " found in " + jarPath);
            }

            InputStream entryStream = jar.getInputStream(entry);

            return new InputStream() {
                @Override public int read() throws IOException { return entryStream.read(); }
                @Override public int read(byte @NonNull [] b, int off, int len) throws IOException { return entryStream.read(b, off, len); }
                @Override public void close() throws IOException {
                    try (jar) { entryStream.close(); }
                }
            };
        } catch (IOException | RuntimeException e) {
            jar.close();
            throw e;
        }
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public @NonNull EventNode<Event> eventNode() {
        return eventNode;
    }

    private static final class PluginSource {
        interface DescriptorOpener {
            InputStream open() throws IOException;
        }

        interface ClasspathSupplier {
            URL[] urls();
        }

        final String description;
        final ClasspathSupplier classpathSupplier;
        final DescriptorOpener descriptorOpener;

        PluginDescriptor descriptor;

        PluginSource(String description, ClasspathSupplier classpathSupplier, DescriptorOpener descriptorOpener) {
            this.description = description;
            this.classpathSupplier = classpathSupplier;
            this.descriptorOpener = descriptorOpener;
        }

        InputStream openDescriptor() throws IOException {
            return descriptorOpener.open();
        }

        URL[] classpath() {
            return classpathSupplier.urls();
        }

        String describe() {
            return description;
        }
    }

    public static class PluginLoadException extends Exception {
        PluginLoadException(String message) {
            super(message);
        }
    }
}