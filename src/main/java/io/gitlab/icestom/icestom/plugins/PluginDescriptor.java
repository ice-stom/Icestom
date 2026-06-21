package io.gitlab.icestom.icestom.plugins;

import com.moandjiezana.toml.Toml;

import java.io.InputStream;

public class PluginDescriptor {

    public int api;
    public String id;
    public String version;
    public String author;
    public String entrypoint;

    public static PluginDescriptor fromInputStream(InputStream file) {
        return new Toml().read(file).to(PluginDescriptor.class);
    }
}
