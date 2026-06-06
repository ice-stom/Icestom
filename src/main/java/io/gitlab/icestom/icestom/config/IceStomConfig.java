package io.gitlab.icestom.icestom.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

public class IceStomConfig {

    public IceStomConfigSection icestom;
    public OpenBoatUtilsConfigSection openboatutils;
    public NetworkConfigSection network;
    public AuthConfigSection auth;
    public MinestomConfigSection minestom;
    public LuckPermsConfigSection luckperms;

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

    public static class IceStomConfigSection {
        public boolean something;
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

    public static class LuckPermsConfigSection {
        public String server;
        public String storage_method;
        public DataSection data;
        public String messaging_service;
        public RedisSection redis;
        public NatsSection nats;
        public RabbitMQSection rabbitmq;

        public static class DataSection {
            public String address;
            public String database;
            public String username;
            public String password;
            public String table_prefix;
            public String mongodb_collection_prefix;  // note: hyphens mapped to underscores
            public String mongodb_connection_uri;
        }
        public static class RedisSection {
            public boolean enabled;
            public String address;
            public String username;
            public String password;
        }
        public static class NatsSection {
            public boolean enabled;
            public String address;
            public String username;
            public String password;
        }
        public static class RabbitMQSection {
            public boolean enabled;
            public String address;
            public String vhost;
            public String username;
            public String password;
        }
    }

    public enum ForwardingMode {
        NONE,
        VELOCITY,
        BUNGEE
    }
}
