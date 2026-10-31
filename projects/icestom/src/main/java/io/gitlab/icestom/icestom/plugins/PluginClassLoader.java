package io.gitlab.icestom.icestom.plugins;

import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {

    private final String pluginId;

    public PluginClassLoader(String pluginId, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.pluginId = pluginId;
    }

    public String getPluginId() {
        return pluginId;
    }

    @Override
    public String toString() {
        return "PluginClassLoader[" + pluginId + "]";
    }
}
