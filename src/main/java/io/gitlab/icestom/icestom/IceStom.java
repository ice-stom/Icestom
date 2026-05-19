package io.gitlab.icestom.icestom;

import io.gitlab.icestom.icestom.command.*;
import io.gitlab.icestom.icestom.config.IceStomConfig;
import io.gitlab.icestom.icestom.database.TimetrialDatabase;
import io.gitlab.icestom.icestom.database.memory.MemoryTimetrialDatabase;
import io.gitlab.icestom.icestom.debug.PerfHud;
import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.event.Event;
import io.gitlab.icestom.icestom.event.EventManager;
import io.gitlab.icestom.icestom.event.stage.Stage;
import io.gitlab.icestom.icestom.instance.PlayerHolder;
import io.gitlab.icestom.icestom.instance.SpawnInstance;
import io.gitlab.icestom.icestom.openboatutils.TransactionPayload;
import io.gitlab.icestom.icestom.timetrial.TimeTrialManager;
import io.gitlab.icestom.icestom.track.format.MutableTrack;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.format.TrackFormat;
import io.gitlab.icestom.icestom.track.TrackLibrary;
import io.gitlab.icestom.icestom.track.checkpoint.*;
import io.gitlab.icestom.icestom.ui.translation.TranslationManager;
import io.gitlab.icestom.icestom.ui.InterfaceManager;
import io.gitlab.icestom.icestom.openboatutils.OBUSettingsPackets;
import me.lucko.spark.minestom.SparkMinestom;
import net.hollowcube.polar.AnvilPolar;
import net.hollowcube.polar.PolarWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.*;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.network.packet.client.common.ClientPluginMessagePacket;
import net.minestom.server.network.packet.client.common.ClientPongPacket;
import net.minestom.server.network.packet.server.common.PingPacket;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class IceStom {

    public static final String NAMESPACE = "icestom";

    // https://github.com/o7Moon/OpenBoatUtils/wiki/Version-IDs
    private static final short MIN_OPENBOATUTILS_VERSION = 19;

    private static final Logger log = LoggerFactory.getLogger(IceStom.class);

    private static IceStom instance;

    private final IceStomConfig config;

    private final MinecraftServer minecraftServer;

    private final TranslationManager translationManager;
    private final TrackLibrary trackLibrary;
    private final TimeTrialManager timeTrialManager;
    private final EventManager eventManager;
    private final InterfaceManager playerScoreboardManager;

    private final SpawnInstance spawnInstance;

    private final TimetrialDatabase timetrialDatabase;

    private final PerfHud perfHud = new PerfHud();

    IceStom() {

        config = IceStomConfig.loadConfig();

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

        translationManager = new TranslationManager(getClass());

        trackLibrary = new TrackLibrary();
        timeTrialManager = new TimeTrialManager();
        eventManager = new EventManager();
        playerScoreboardManager = new InterfaceManager();

        spawnInstance = new SpawnInstance();

        timetrialDatabase = new MemoryTimetrialDatabase();
    }

    @SuppressWarnings("UnstableApiUsage")
    public void start() throws IOException {
        Path directory = Path.of("spark");
        SparkMinestom.builder(directory)
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

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        instanceManager.registerInstance(spawnInstance);

        spawnInstance.setup();

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();

            @Nullable Event active_event = eventManager.getEvent(player);

            if (active_event != null) {
                @Nullable Stage<?> current_stage = active_event.getCurrentStage();

                if (current_stage != null) {
                    event.setSpawningInstance(current_stage.getInstance(player));

                    EventListener<@NotNull PlayerSpawnEvent> listener = EventListener.builder(PlayerSpawnEvent.class)
                            .filter(e -> e.getPlayer() == player)
                            .handler(_ -> {
                                current_stage.getInstance(player).consume(player);
                            })
                            .expireCount(1)
                            .build();

                    globalEventHandler.addListener(listener);

                    return;
                }
            }

            event.setSpawningInstance(spawnInstance);
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, playerSpawnEvent -> {
            perfHud.addViewer(playerSpawnEvent.getPlayer());
        });

        globalEventHandler.addListener(ServerTickMonitorEvent.class, perfHud::onTick);

        final PluginMessagePacket join_setting_packet = new OBUSettingsPackets.TransactionPacket(new TransactionPayload(List.of(
                new OBUSettingsPackets.InterpolationCompatPacket(config.openboatutils.interpolation_compatibility),
                new OBUSettingsPackets.SetResetOnWorldLoad(false),
                new OBUSettingsPackets.ResendVersionPacket()
        ))).toPacket(OBUSettingsPackets.getChannel());

        globalEventHandler.addListener(PlayerPluginMessageEvent.class, playerPluginMessageEvent -> {
            final Player player = playerPluginMessageEvent.getPlayer();

            if (playerPluginMessageEvent.getIdentifier().equals(OBUSettingsPackets.getChannel())) {
                try {
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(playerPluginMessageEvent.getMessage()));
                    short packetId = in.readShort();

                    if (packetId == 0) {
                        if (((IceStomPlayer) player).getOpenBoatUtilsVersion() != null) return;

                        int version = in.readInt();

                        if (version < MIN_OPENBOATUTILS_VERSION) {
                            player.sendMessage(Component.translatable("message.openboatutils.outdated_version_global"));
                            return;
                        }

                        boolean unstable = in.readBoolean();

                        if (unstable) {
                            if (config.openboatutils.block_unstable) {
                                player.kick(Component.translatable("message.openboatutils.block_unstable"));
                                return;
                            }

                            player.sendMessage(Component.translatable("message.openboatutils.warning_unstable"));
                        }

                        ((IceStomPlayer) player).setOpenBoatUtilsVersion(version);
                        player.sendPacket(join_setting_packet);

                        int random = ThreadLocalRandom.current().nextInt(0xFF);

                        player.sendPacket(new PingPacket(random));

                        // if we get the ping packet before the plugin message we know something is up
                        EventListener<@NotNull PlayerPacketEvent> listener = EventListener.builder(PlayerPacketEvent.class)
                                .filter(e -> e.getPlayer() == player && (
                                        (e.getPacket() instanceof ClientPluginMessagePacket pluginMessagePacket && pluginMessagePacket.channel().equals(OBUSettingsPackets.getChannel())) ||
                                        e.getPacket() instanceof ClientPongPacket
                                ))
                                .handler(event -> {
                                    if (event.getPacket() instanceof ClientPluginMessagePacket pluginMessagePacket) {
                                        DataInputStream in2 = new DataInputStream(new ByteArrayInputStream(pluginMessagePacket.data()));

                                        try {
                                            if (in2.readShort() == 0) {
                                                return;
                                            };
                                        } catch (IOException ignored) {}
                                    };
                                    if (!(event.getPacket() instanceof ClientPongPacket(int id))) {
                                        log.error("Transaction failed with {} instead of a pong", event.getPacket());
                                        return;
                                    };

                                    if (id != random) {
                                        player.kick(Component.translatable("message.openboatutils.failed_transaction"));
                                        return;
                                    };
                                })
                                .expireCount(1)
                                .build();

                        player.eventNode().addListener(listener);
                    }
                } catch (IOException e) {
                    log.error("Failed OpenBoatUtils Handshake: {}", String.valueOf(e));
                }
            }
        });

        globalEventHandler.addListener(PlayerDisconnectEvent.class,playerDisconnectEvent -> {
            final Player player = playerDisconnectEvent.getPlayer();
            if (player.getInstance() instanceof PlayerHolder playerHolder) {
                playerHolder.drop(player);
            }
        });

        globalEventHandler.addListener(PlayerPacketOutEvent.class, playerPacketOutEvent -> {
            Player player = playerPacketOutEvent.getPlayer();
            Instance instanceContainer = player.getInstance();

            if (playerPacketOutEvent.getPacket() instanceof EntityVelocityPacket(int entityId, Vec _)) {
                Entity entity = instanceContainer.getEntityById(entityId);

                if (entity instanceof Boat) {
                    playerPacketOutEvent.setCancelled(true);
                }
            }
        });

        log.info("Starting IceStom server on {}:{}", config.network.bind, config.network.port);

        minecraftServer.start(config.network.bind, config.network.port);
    }

    public TrackLibrary getTrackLibrary() { return trackLibrary; }

    public TimeTrialManager getTimeTrialManager() { return timeTrialManager; }

    public EventManager getEventManager() { return eventManager; }

    public TranslationManager getTranslationManager() { return translationManager; }

    public InterfaceManager getPlayerLeaderboardManager() { return playerScoreboardManager; }

    public SpawnInstance getSpawnInstance() { return spawnInstance; }

    public TimetrialDatabase getTimetrialDatabase() { return timetrialDatabase; }

    static void main(String[] args) throws IOException {
        if (args.length == 2 && args[0].equals("--convert")) {
            Path path = Path.of(args[1]);

            System.out.println("Converting " + path + " to stomtrack.");

            PolarWorld world = AnvilPolar.anvilToPolar(path);

            String id = path.getFileName().toString();

            TrackFormat.saveTrack(TrackLibrary.TRACK_STORAGE_PATH.resolve(
                            id + "." + TrackFormat.FILE_EXTENSION).toFile(),
                    new Track(new MutableTrack(
                            id,
                            Component.text("Track named " + id, NamedTextColor.RED),
                            true,
                            new Pos(-14.03, 17.00, 11.82, -205.65f, 0f),
                            Map.of(
                                    new LineCheckpoint(new Vec(-34.5, 17 ,-21.5), new Vec(-22.5, 17, -21.5), 3), 0,
                                    new LineCheckpoint(new Vec(-17.5, 17, -41.5), new Vec(-9.5, 17, -33.5), 3), 1,
                                    new LineCheckpoint(new Vec(22.5, 17, -53.5), new Vec(14.5, 17, -44.5), 3), 2,
                                    new LineCheckpoint(new Vec(40.5, 17, -1.5), new Vec(32.5, 17, -9.5), 3), 3,
                                    new LineCheckpoint(new Vec(0.5, 17, 18.5), new Vec(-0.5, 17, 7.5), 3), 4
                            ),
                            List.of(
                                    new Pos(-30.5, 17.00, -18.5, -180, 0),
                                    new Pos(-26.5, 17.00, -17.5, -180, 0),
                                    new Pos(-30.5, 17.00, -16.5, -180, 0),
                                    new Pos(-26.5, 17.00, -15.5, -180, 0),
                                    new Pos(-30.5, 17.00, -14.5, -180, 0),
                                    new Pos(-26.5, 17.00, -13.5, -180, 0),
                                    new Pos(-30.5, 17.00, -12.5, -180, 0),
                                    new Pos(-26.5, 17.00, -11.5, -180, 0)
                            ),
                            List.of()
                    ), world)
            );

            TrackFormat.saveTrack(TrackLibrary.TRACK_STORAGE_PATH.resolve(
                            id + "_obu." + TrackFormat.FILE_EXTENSION).toFile(),
                    new Track(new MutableTrack(
                            id + "_obu",
                            Component.text("Track named " + id + "_obu", NamedTextColor.RED),
                            true,
                            new Pos(81.5, 12.00, -47.5, 15, 0),
                            Map.of(
                                    new LineCheckpoint(new Vec(59.5, 9.00, 25.5), new Vec(51.5, 9.00, 19.5), 3), 0,
                                    new LineCheckpoint(new Vec( 57.5, 9.00, 31.5), new Vec(49.5, 9.00, 27.5), 3), 1,
                                    new LineCheckpoint(new Vec(28.5, 9.00, 75.5), new Vec(22.5, 9.00, 69.5), 3), 2,
                                    new LineCheckpoint(new Vec(-8.5, 9.00, 102.5), new Vec(-12.5, 9.00, 93.5), 3), 3,
                                    new LineCheckpoint(new Vec(-52.5, 9.00, 69.5), new Vec(-43.5, 9.00, 54.5), 3), 4
                            ),
                            List.of(
                                    new Pos(-30.5, 17.00, -18.5, -180, 0),
                                    new Pos(-26.5, 17.00, -17.5, -180, 0),
                                    new Pos(-30.5, 17.00, -16.5, -180, 0),
                                    new Pos(-26.5, 17.00, -15.5, -180, 0),
                                    new Pos(-30.5, 17.00, -14.5, -180, 0),
                                    new Pos(-26.5, 17.00, -13.5, -180, 0),
                                    new Pos(-30.5, 17.00, -12.5, -180, 0),
                                    new Pos(-26.5, 17.00, -11.5, -180, 0)
                            ),
                            List.of(
                                    new OBUSettingsPackets.DefaultSlipperinessPacket(0.98f),
                                    new OBUSettingsPackets.StepHeightPacket(1.1f),
                                    new OBUSettingsPackets.StepWhileFallingPacket(true),
                                    new OBUSettingsPackets.AirControlPacket(true)
                            )
                    ), world)
            );

            return;
        }

        instance = new IceStom();
        instance.start();
    }

    public static IceStom getInstance() {
        return instance;
    }
}