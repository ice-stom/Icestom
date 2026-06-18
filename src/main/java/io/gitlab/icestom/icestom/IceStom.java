package io.gitlab.icestom.icestom;

import io.gitlab.icestom.icestom.command.*;
import io.gitlab.icestom.icestom.config.IceStomConfig;
import io.gitlab.icestom.icestom.database.TimetrialDatabase;
import io.gitlab.icestom.icestom.database.memory.MemoryTimetrialDatabase;
import io.gitlab.icestom.icestom.database.sqlite.SQLiteTimetrialDatabase;
import io.gitlab.icestom.icestom.debug.PerfHud;
import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.event.EventManager;
import io.gitlab.icestom.icestom.instance.PlayerHolder;
import io.gitlab.icestom.icestom.instance.SpawnInstance;
import io.gitlab.icestom.icestom.openboatutils.OpenBoatUtilsManager;
import io.gitlab.icestom.icestom.race.RaceInstance;
import io.gitlab.icestom.icestom.timetrial.TimeTrialManager;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.track.format.MutableTrack;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.format.StomtrackFormat;
import io.gitlab.icestom.icestom.track.TrackLibrary;
import io.gitlab.icestom.icestom.track.format.TrackEnvironmentData;
import io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager;
import io.gitlab.icestom.icestom.ui.interfaces.impl.VanillaInterface;
import io.gitlab.icestom.icestom.ui.translation.TranslationManager;
import me.lucko.luckperms.common.config.generic.adapter.EnvironmentVariableConfigAdapter;
import me.lucko.luckperms.common.config.generic.adapter.MultiConfigurationAdapter;
import me.lucko.luckperms.common.config.generic.adapter.SystemPropertyConfigAdapter;
import me.lucko.luckperms.minestom.CommandRegistry;
import me.lucko.luckperms.minestom.LuckPermsMinestom;
import me.lucko.spark.minestom.SparkMinestom;
import net.hollowcube.polar.AnvilPolar;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityStatuses;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.*;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.network.packet.server.play.EntityStatusPacket;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;
import net.minestom.server.network.packet.server.play.VehicleMovePacket;
import net.minestom.server.world.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class IceStom {

    public static final String NAMESPACE = "icestom";

    private static final Logger log = LoggerFactory.getLogger(IceStom.class);

    private static IceStom instance;

    private final MinecraftServer minecraftServer;

    private final TranslationManager translationManager;
    private final TrackLibrary trackLibrary;
    private final TimeTrialManager timeTrialManager;
    private final EventManager eventManager;
    private final OpenBoatUtilsManager openBoatUtilsManager;

    private final SpawnInstance spawnInstance;

    private final TimetrialDatabase timetrialDatabase;

    private final PerfHud perfHud = new PerfHud();

    private SparkMinestom spark;
    private LuckPerms luckPerms;

    IceStom() {
        instance = this;

        log.info("Hello from IceStom!");

        IceStomConfig config = IceStomConfig.loadConfig();

        // Minestom
        System.setProperty("minestom.chunk-view-distance", String.valueOf(config.minestom.chunk_view_distance));
        System.setProperty("minestom.entity-view-distance", String.valueOf(config.minestom.entity_view_distance));
        System.setProperty("minestom.dispatcher-threads", String.valueOf(config.minestom.dispatcher_threads));

        // Luckperms
        System.setProperty("org.slf4j.simpleLogger.log.me.lucko.luckperms.minestom", "off");
        System.setProperty("luckperms.server", String.valueOf(config.luckperms.server));
        System.setProperty("luckperms.storage-method", String.valueOf(config.luckperms.storage_method));
        System.setProperty("luckperms.data.address", String.valueOf(config.luckperms.data.address));
        System.setProperty("luckperms.data.database", String.valueOf(config.luckperms.data.database));
        System.setProperty("luckperms.data.username", String.valueOf(config.luckperms.data.username));
        System.setProperty("luckperms.data.password", String.valueOf(config.luckperms.data.password));
        System.setProperty("luckperms.data.table-prefix", String.valueOf(config.luckperms.data.table_prefix));
        System.setProperty("luckperms.data.mongodb-collection-prefix", String.valueOf(config.luckperms.data.mongodb_collection_prefix));
        System.setProperty("luckperms.data.mongodb-connection-uri", String.valueOf(config.luckperms.data.mongodb_connection_uri));
        System.setProperty("luckperms.messaging-service", String.valueOf(config.luckperms.messaging_service));
        System.setProperty("luckperms.redis.enabled", String.valueOf(config.luckperms.redis.enabled));
        System.setProperty("luckperms.redis.address", String.valueOf(config.luckperms.redis.address));
        System.setProperty("luckperms.redis.username", String.valueOf(config.luckperms.redis.username));
        System.setProperty("luckperms.redis.password", String.valueOf(config.luckperms.redis.password));
        System.setProperty("luckperms.nats.enabled", String.valueOf(config.luckperms.nats.enabled));
        System.setProperty("luckperms.nats.address", String.valueOf(config.luckperms.nats.address));
        System.setProperty("luckperms.nats.username", String.valueOf(config.luckperms.nats.username));
        System.setProperty("luckperms.nats.password", String.valueOf(config.luckperms.nats.password));
        System.setProperty("luckperms.rabbitmq.enabled", String.valueOf(config.luckperms.rabbitmq.enabled));
        System.setProperty("luckperms.rabbitmq.address", String.valueOf(config.luckperms.rabbitmq.address));
        System.setProperty("luckperms.rabbitmq.vhost", String.valueOf(config.luckperms.rabbitmq.vhost));
        System.setProperty("luckperms.rabbitmq.username", String.valueOf(config.luckperms.rabbitmq.username));
        System.setProperty("luckperms.rabbitmq.password", String.valueOf(config.luckperms.rabbitmq.password));

        Auth auth = switch (config.auth.forwarding) {
            case NONE -> config.auth.online_mode ? new Auth.Online() : new Auth.Offline();
            case VELOCITY -> new Auth.Velocity(config.auth.velocity_secret);
            case BUNGEE -> new Auth.Bungee(Set.copyOf(config.auth.bungeeguard_secrets));
        };

        minecraftServer = MinecraftServer.init(auth);

        MinecraftServer.setCompressionThreshold(config.minestom.compression_threshold);
        MinecraftServer.setBrandName(String.format("IceStom (%s)", MinecraftServer.getBrandName()));

        trackLibrary = new TrackLibrary();

        translationManager = new TranslationManager(getClass());
        timeTrialManager = new TimeTrialManager();
        eventManager = new EventManager();
        openBoatUtilsManager = new OpenBoatUtilsManager();

        timetrialDatabase = switch (config.database.type) {
            case "memory" -> new MemoryTimetrialDatabase();
            case "sqlite" -> {
                try {
                    yield new SQLiteTimetrialDatabase("db.sqlite");
                } catch (Exception e) {
                    throw new RuntimeException("Failed to init SQLite: " + e);
                }
            }
            default -> throw new RuntimeException("Unknown database type: " + config.database.type);
        };

        spawnInstance = new SpawnInstance();
    }

    @SuppressWarnings("UnstableApiUsage")
    public void start() {
        spark = SparkMinestom.builder(Path.of("spark"))
                .commands(true)
                .permissionHandler((_, _) -> true)
                .enable();

        luckPerms = LuckPermsMinestom.builder(Path.of("luckperms"))
                .commandRegistry(CommandRegistry.minestom())
                .configurationAdapter(plugin -> new MultiConfigurationAdapter(plugin,
                        new EnvironmentVariableConfigAdapter(plugin),
                        new SystemPropertyConfigAdapter(plugin)
                ))
                //.permissionSuggestions("test.permission", "test.other")
                .enable();


        MinecraftServer.getConnectionManager().setPlayerProvider(IceStomPlayer::new);

        trackLibrary.init();

        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new BoatCommand());
        commandManager.register(new TimeTrialCommand());
        commandManager.register(new DebugCommand());
        commandManager.register(new TrackCommand());
        commandManager.register(new SpawnCommand());
        commandManager.register(new EventCommand());
        commandManager.register(new ResetCommand());
        commandManager.register(new GamemodeCommand());

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        instanceManager.registerInstance(spawnInstance);

        spawnInstance.init();

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addChild(openBoatUtilsManager.eventNode());
        globalEventHandler.addChild(perfHud.eventNode());
        globalEventHandler.addChild(InterfaceManager.EVENT_NODE);

        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(spawnInstance);
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            final IceStomPlayer player = (IceStomPlayer) event.getPlayer();

            player.setGameMode(GameMode.ADVENTURE);
            player.sendPacket(new EntityStatusPacket(
                    player.getEntityId(),
                    (byte) (EntityStatuses.Player.PERMISSION_LEVEL_0 + 2))
            );

            if (!player.hasPermission("icestom.perfhud")) return;

            perfHud.addViewer(player);
        });

        globalEventHandler.addListener(PlayerDisconnectEvent.class,playerDisconnectEvent -> {
            final Player player = playerDisconnectEvent.getPlayer();
            if (player.getInstance() instanceof PlayerHolder playerHolder) {
                playerHolder.drop(player);
            }
        });

        globalEventHandler.addListener(PlayerPacketOutEvent.class, playerPacketOutEvent -> {
            Player player = playerPacketOutEvent.getPlayer();

            if (playerPacketOutEvent.getPacket() instanceof EntityVelocityPacket(int entityId, Vec _)) {
                Instance instanceContainer = player.getInstance();
                Entity entity = instanceContainer.getEntityById(entityId);

                if (entity instanceof Boat) {
                    playerPacketOutEvent.setCancelled(true);
                }
            }

            if (playerPacketOutEvent.getPacket() instanceof VehicleMovePacket) {
                playerPacketOutEvent.setCancelled(true);
            }
        });

        InterfaceManager.register(TimeTrialingInstance.class, new VanillaInterface());
        InterfaceManager.register(RaceInstance.class, new VanillaInterface());

        IceStomConfig config = IceStomConfig.getConfig();

        log.info("Starting IceStom server on {}:{}", config.network.bind, config.network.port);

        minecraftServer.start(config.network.bind, config.network.port);
    }

    public TrackLibrary getTrackLibrary() { return trackLibrary; }

    public TimeTrialManager getTimeTrialManager() { return timeTrialManager; }

    public EventManager getEventManager() { return eventManager; }

    public TranslationManager getTranslationManager() { return translationManager; }

    public SpawnInstance getSpawnInstance() { return spawnInstance; }

    public TimetrialDatabase getTimetrialDatabase() { return timetrialDatabase; }

    public LuckPerms getLuckPerms() { return luckPerms; }

    static void main(String[] args) throws IOException, StomtrackFormat.TrackSaveException {
        instance = new IceStom();

        if (args.length == 2 && args[0].equals("--convert")) {
            Path path = Path.of(args[1]);

            System.out.println("Converting " + path + " to stomtrack.");

            PolarWorld world = AnvilPolar.anvilToPolar(path);

            MinecraftServer.getInstanceManager().createInstanceContainer();
            InstanceContainer temp = new InstanceContainer(UUID.randomUUID(), DimensionType.OVERWORLD);
            temp.setChunkSupplier(LightingChunk::new);
            temp.setChunkLoader(new PolarLoader(world));

            List<CompletableFuture<Chunk>> futures = world.chunks().stream()
                    .map(chunk -> temp.loadChunk(chunk.x(), chunk.z()))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        LightingChunk.relight(temp, temp.getChunks());
                    }).join();

            String id = path.getFileName().toString();

            StomtrackFormat.saveStomtrack(TrackLibrary.TRACK_STORAGE_PATH.resolve(
                            id + "." + StomtrackFormat.FILE_EXTENSION).toFile(),
                    world,
                    List.of(new Track(new MutableTrack(
                            id,
                            Component.text(id, NamedTextColor.RED),
                            true,
                            new Pos(0, 0, 0, 0, 0),
                            Map.of(),
                            List.of(),
                            List.of()
                    ), world, new TrackEnvironmentData(0, -64, 384, false, "OVERWORLD"), id))
            );

            return;
        }
        instance.start();
    }

    public static IceStom getInstance() {
        return instance;
    }
}