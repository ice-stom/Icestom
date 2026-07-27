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
import io.gitlab.icestom.icestom.instance.DefaultSpawnInstance;
import io.gitlab.icestom.icestom.instance.SpawnInstance;
import io.gitlab.icestom.icestom.openboatutils.OpenBoatUtilsManager;
import io.gitlab.icestom.icestom.plugins.PluginManager;
import io.gitlab.icestom.icestom.race.RaceInstance;
import io.gitlab.icestom.icestom.timetrial.TimeTrialManager;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager;
import io.gitlab.icestom.icestom.ui.interfaces.impl.VanillaInterface;
import io.gitlab.icestom.icestom.ui.translation.TranslationManager;
import me.lucko.spark.minestom.SparkMinestom;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityStatuses;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.*;
import net.minestom.server.network.packet.server.play.EntityStatusPacket;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;
import net.minestom.server.network.packet.server.play.VehicleMovePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public class IceStom {

    public static final String NAMESPACE = "icestom";

    private static final Logger log = LoggerFactory.getLogger(IceStom.class);

    private static IceStom instance;

    private final MinecraftServer minecraftServer;

    private final PluginManager pluginManager;

    private final TranslationManager translationManager;
    private final TrackLibrary trackLibrary;
    private final TimeTrialManager timeTrialManager;
    private final EventManager eventManager;
    private final OpenBoatUtilsManager openBoatUtilsManager;

    private final Instance spawnInstance;
    private Supplier<SpawnInstance> spawnProvider = DefaultSpawnInstance::new;

    private final TimetrialDatabase timetrialDatabase;

    private final PerfHud perfHud = new PerfHud();

    private SparkMinestom spark;

    IceStom() throws PluginManager.PluginLoadException, IOException {
        instance = this;

        log.info("Hello from IceStom!");

        IceStomConfig config = IceStomConfig.loadConfig();

        System.setProperty("minestom.chunk-view-distance", String.valueOf(config.minestom.chunk_view_distance));
        System.setProperty("minestom.entity-view-distance", String.valueOf(config.minestom.entity_view_distance));
        System.setProperty("minestom.dispatcher-threads", String.valueOf(config.minestom.dispatcher_threads));

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
                    yield new SQLiteTimetrialDatabase(Path.of("db"));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to init SQLite: " + e);
                }
            }
            default -> throw new RuntimeException("Unknown database type: " + config.database.type);
        };

        pluginManager = new PluginManager(Path.of("plugins"));
        pluginManager.loadPlugins();

        spawnInstance = (Instance) spawnProvider.get();
    }

    @SuppressWarnings("UnstableApiUsage")
    public void start() {
        spark = SparkMinestom.builder(Path.of("spark"))
                .commands(true)
                .permissionHandler((_, _) -> true)
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

        ((SpawnInstance) spawnInstance).init();

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addChild(openBoatUtilsManager.eventNode());
        globalEventHandler.addChild(perfHud.eventNode());
        globalEventHandler.addChild(InterfaceManager.EVENT_NODE);
        globalEventHandler.addChild(pluginManager.eventNode());

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

                if (instanceContainer == null) return; // we can get unlucky when people leave

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

    public void setSpawnProvider(Supplier<SpawnInstance> spawnProvider) {
        this.spawnProvider = spawnProvider;
    }

    public TrackLibrary getTrackLibrary() { return trackLibrary; }

    public TimeTrialManager getTimeTrialManager() { return timeTrialManager; }

    public EventManager getEventManager() { return eventManager; }

    public TranslationManager getTranslationManager() { return translationManager; }

    public SpawnInstance getSpawnInstance() { return (SpawnInstance) spawnInstance; }

    public TimetrialDatabase getTimetrialDatabase() { return timetrialDatabase; }

    static void main(String[] args) throws IOException, PluginManager.PluginLoadException {
        instance = new IceStom();

        instance.start();
    }

    public static IceStom getInstance() {
        return instance;
    }
}