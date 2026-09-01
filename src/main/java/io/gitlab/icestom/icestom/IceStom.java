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
import io.gitlab.icestom.icestom.event.StageRegistry;
import io.gitlab.icestom.icestom.instance.PlayerHolder;
import io.gitlab.icestom.icestom.instance.DefaultSpawnInstance;
import io.gitlab.icestom.icestom.instance.SpawnInstance;
import io.gitlab.icestom.icestom.openboatutils.OpenBoatUtilsManager;
import io.gitlab.icestom.icestom.plugins.PluginManager;
import io.gitlab.icestom.icestom.race.RaceStage;
import io.gitlab.icestom.icestom.stages.PodiumStage;
import io.gitlab.icestom.icestom.stages.PracticeStage;
import io.gitlab.icestom.icestom.timetrial.TimeTrialManager;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager;
import io.gitlab.icestom.icestom.ui.interfaces.impl.VanillaInterface;
import io.gitlab.icestom.icestom.ui.translation.TranslationManager;
import me.lucko.spark.minestom.SparkMinestom;
import net.kyori.adventure.key.Key;
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

import static io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager.getHolder;

public class IceStom {

    public static final String NAMESPACE = "icestom";

    private static final Logger log = LoggerFactory.getLogger(IceStom.class);

    private static IceStom instance;

    private final InterfaceManager.InterfaceHolder interfaceHolder;

    private final MinecraftServer minecraftServer;

    private final PluginManager pluginManager;

    private final TranslationManager translationManager;
    private final TrackLibrary trackLibrary;
    private final StageRegistry stageRegistry;
    private final TimeTrialManager timeTrialManager;
    private final OpenBoatUtilsManager openBoatUtilsManager;
    private final EventManager eventManager;

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

        spark = SparkMinestom.builder(Path.of("spark"))
                .commands(true)
                .permissionHandler((_, _) -> true)
                .enable();

        MinecraftServer.setCompressionThreshold(config.minestom.compression_threshold);
        MinecraftServer.setBrandName(String.format("IceStom (%s)", MinecraftServer.getBrandName()));
        MinecraftServer.getConnectionManager().setPlayerProvider(IceStomPlayer::new);

        trackLibrary = new TrackLibrary();
        trackLibrary.init();

        stageRegistry = new StageRegistry();
        stageRegistry.register(Key.key(NAMESPACE, "practice"), PracticeStage.class, PracticeStage::create);
        stageRegistry.register(Key.key(NAMESPACE, "race"), RaceStage.class, RaceStage::create);
        stageRegistry.register(Key.key(NAMESPACE, "podium"), PodiumStage.class, PodiumStage::create);

        translationManager = new TranslationManager(getClass());
        timeTrialManager = new TimeTrialManager();
        openBoatUtilsManager = new OpenBoatUtilsManager();

        eventManager = new EventManager(Path.of("events"));

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

        spawnInstance = (Instance) spawnProvider.get();

        InterfaceManager.register(TimeTrialingInstance.class, new VanillaInterface());
        InterfaceManager.register(RaceStage.class, new VanillaInterface());
        InterfaceManager.register(IceStom.class, new VanillaInterface());

        interfaceHolder = getHolder(IceStom.class, this);

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        pluginManager = new PluginManager(Path.of("plugins"));
        pluginManager.loadPlugins();

        globalEventHandler.addChild(pluginManager.eventNode());

        globalEventHandler.addChild(openBoatUtilsManager.eventNode());
        globalEventHandler.addChild(perfHud.eventNode());
        globalEventHandler.addChild(InterfaceManager.EVENT_NODE);

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            final IceStomPlayer player = (IceStomPlayer) event.getPlayer();

            player.setGameMode(GameMode.ADVENTURE);
            player.sendPacket(new EntityStatusPacket(
                    player.getEntityId(),
                    (byte) (EntityStatuses.Player.PERMISSION_LEVEL_0 + 2))
            );

            interfaceHolder.startWatching(player);

            if (!player.hasPermission("icestom.perfhud")) return;

            perfHud.addViewer(player);
        });

        globalEventHandler.addListener(PlayerDisconnectEvent.class,playerDisconnectEvent -> {
            final Player player = playerDisconnectEvent.getPlayer();

            interfaceHolder.stopWatching(player);

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

        pluginManager.startPlugins();

    }

    @SuppressWarnings("UnstableApiUsage")
    public void startStandard() {
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

        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();

            event.setSpawningInstance(spawnInstance);
            player.setRespawnPoint(((SpawnInstance) spawnInstance).spawnLocation(player));
        });

        IceStomConfig config = IceStomConfig.getConfig();

        log.info("Starting IceStom server on {}:{}", config.network.bind, config.network.port);

        minecraftServer.start(config.network.bind, config.network.port);
    }

    public void setSpawnProvider(Supplier<SpawnInstance> spawnProvider) {
        this.spawnProvider = spawnProvider;
    }

    public TrackLibrary getTrackLibrary() { return trackLibrary; }

    public TimeTrialManager getTimeTrialManager() { return timeTrialManager; }

    public StageRegistry getStageRegistry() { return stageRegistry; }

    public TranslationManager getTranslationManager() { return translationManager; }

    public SpawnInstance getSpawnInstance() { return (SpawnInstance) spawnInstance; }

    public TimetrialDatabase getTimetrialDatabase() { return timetrialDatabase; }

    public EventManager getEventManager() {
        return eventManager;
    }

    static void main(String[] args) throws IOException, PluginManager.PluginLoadException {
        instance = new IceStom();

        instance.startStandard();
    }

    public static IceStom getInstance() {
        return instance;
    }
}