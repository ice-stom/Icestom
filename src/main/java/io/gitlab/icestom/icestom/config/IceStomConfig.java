package io.gitlab.icestom.icestom.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class IceStomConfig {

    public IceStomConfigSection icestom;
    public DatabaseConfigSection database;
    public OpenBoatUtilsConfigSection openboatutils;
    public NetworkConfigSection network;
    public AuthConfigSection auth;
    public MinestomConfigSection minestom;

    public Map<String, String> library;

    private static final Path config_file = Path.of("config.toml");
    private static final FileConfig config = FileConfig.builder(config_file)
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

    public static class IceStomConfigSection {
        public int leaderboard_rows;
        public String reset_item;
    }

    public static class DatabaseConfigSection {
        public String type;
        public String address;
        public String database;
        public String username;
        public String password;
        public String table_prefix;
    }

    public static class OpenBoatUtilsConfigSection {
        public boolean block_unstable;
        public boolean interpolation_compatibility;
    }

    public static class NetworkConfigSection {
        public String bind;
        public int port;
    }

    public static class AuthConfigSection {
        public boolean online_mode;
        public ForwardingMode forwarding;
        public String velocity_secret;
        public List<String> bungeeguard_secrets;
    }

    public static class MinestomConfigSection {
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
