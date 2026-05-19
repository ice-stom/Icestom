package io.gitlab.icestom.icestom.ui.translation;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.ui.theme.Theme;
import io.gitlab.icestom.icestom.ui.theme.ThemeResolver;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslator;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TranslationManager extends MiniMessageTranslator {

    private static final Logger log = LoggerFactory.getLogger(TranslationManager.class);

    private final Map<Locale, Map<String, String>> locales = new HashMap<>();
    private final Map<Theme, TranslatableComponentRenderer<Locale>> componentRenderers = new HashMap<>();

    public TranslationManager(Class<?> rootClass) {
        loadTranslationsFromClassPath(rootClass);
    }

    public Component render(@NotNull Component component, @NotNull Locale locale, @NotNull Theme theme) {
        TranslatableComponentRenderer<Locale> renderer = componentRenderers.computeIfAbsent(theme, theme1 -> {
            ThemedTranslator translator = new TranslationManager.ThemedTranslator(this, theme1);

            return TranslatableComponentRenderer.usingTranslationSource(translator);
        });

        return renderer.render(component, locale);
    }

    private void loadTranslationsFromClassPath(Class<?> rootClass) {
        try {
            URL url = rootClass.getClassLoader().getResource("lang");
            if (url == null) {
                log.error("Failed to load translations: Bad lang resource uri");
                return;
            }

            URI uri = url.toURI();

            if (uri.getScheme().equals("jar")) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                    scanAndLoad(fs.getPath("lang"));
                }
            } else {
                scanAndLoad(Path.of(uri));
            }

        } catch (URISyntaxException | IOException e) {
            log.error("Failed to load translations from classpath: {}", e.getMessage());
        }
    }

    private void loadProperties(Reader reader, Locale locale) throws IOException {
        Properties props = new Properties();
        props.load(reader);

        Map<String, String> map = locales.computeIfAbsent(locale, _ -> new HashMap<>());
        for (String key : props.stringPropertyNames()) {
            map.put(key, props.getProperty(key));
        }
    }

    private void scanAndLoad(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            stream.filter(p -> p.toString().endsWith(".properties"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString()
                                .replace(".properties", "");

                        Locale locale = Locale.forLanguageTag(fileName);
                        if (locale.getLanguage().isEmpty()) {
                            log.warn("Could not parse locale: {}", fileName);
                            return;
                        }

                        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                            loadProperties(reader, locale);
                            log.info("Loaded translations for {}", locale.toLanguageTag());
                        } catch (IOException e) {
                            log.error("Failed to load translations for locale {} at {}: {}", locale, path, e.getMessage());
                        }
                    });
        }
    }

    @Override
    protected @Nullable String getMiniMessageString(@NotNull String key, @NotNull Locale locale) {
        Map<String, String> map = locales.get(locale);
        if (map != null) {
            String value = map.get(key);
            if (value != null) return value;
        }

        Locale english = Locale.of("en", "US");
        if (!english.equals(locale)) {
            map = locales.get(english);
            if (map != null) return map.get(key);
        }

        return null;
    }

    @Override
    public @NotNull Key name() {
        return Key.key(IceStom.NAMESPACE, "fallback");
    }

    public static class ThemedTranslator extends MiniMessageTranslator {
        private final TranslationManager parent;
        private final Theme theme;

        public ThemedTranslator(TranslationManager parent, Theme theme) {
            super(MiniMessage.builder()
                    .tags(TagResolver.builder()
                            .resolver(StandardTags.defaults())
                            .resolver(ThemeResolver.themeResolver(theme))
                            .build())
                    .build());
            this.parent = parent;
            this.theme = theme;
        }

        @Override
        protected @Nullable String getMiniMessageString(@NotNull String key, @NotNull Locale locale) {
            return parent.getMiniMessageString(key, locale);
        }

        @Override
        public @NotNull Key name() {
            return theme.getId();
        }
    }
}
