package io.gitlab.icestom.icestom.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class IceStomConfig {

    public NetworkConfig network;
    public AuthConfig auth;
    public MinestomConfig minestom;

    private static Path config_file = Path.of("config.toml");
    private static FileConfig config = FileConfig.builder(config_file)
            .defaultResource("/config.toml")
            .build();

    private static final IceStomConfig instance = new IceStomConfig();
    private static boolean loaded = false;

    public static @NotNull IceStomConfig loadConfig() {
        config.load();

        ObjectDeserializer deserializer = ObjectDeserializer.builder()
                .build();

        deserializer.deserializeFields(config, instance);

        loaded = true;

        return instance;
    }

    public static @NotNull IceStomConfig getConfig() {
        if (!loaded) throw new RuntimeException("Attempted to use config before load!");

        return instance;
    }

    public static class NetworkConfig {
        public String bind;
        public int port;
    }

    public static class AuthConfig {
        public boolean online_mode;
        public ForwardingMode forwarding;
        public String velocity_secret;
        public List<String> bungeeguard_secrets;
    }

    public static class MinestomConfig {
        public int compression_threshold;
        public int chunk_view_distance;
        public int entity_view_distance;
        public int dispatcher_threads;
    }

    public enum ForwardingMode {
        NONE,
        VELOCITY,
        BUNGEE
    }
}
