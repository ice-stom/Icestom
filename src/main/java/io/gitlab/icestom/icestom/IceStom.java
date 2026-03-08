package io.gitlab.icestom.icestom;

import io.gitlab.icestom.icestom.command.*;
import io.gitlab.icestom.icestom.database.TimetrialDatabase;
import io.gitlab.icestom.icestom.database.memory.MemoryTimetrialDatabase;
import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.instance.SpawnInstance;
import io.gitlab.icestom.icestom.track.format.MutableTrack;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.format.TrackFormat;
import io.gitlab.icestom.icestom.track.TrackLibrary;
import io.gitlab.icestom.icestom.track.checkpoint.*;
import net.hollowcube.polar.AnvilPolar;
import net.hollowcube.polar.PolarWorld;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class IceStom {

    public static final String NAMESPACE = "icestom";

    private static IceStom instance;

    private final MinecraftServer minecraftServer;

    private final TrackLibrary trackLibrary;
    private final TimeTrialManager timeTrialManager;

    private final SpawnInstance spawnInstance;

    private final TimetrialDatabase timetrialDatabase;

    IceStom() {
        System.setProperty("minestom.chunk-view-distance", "8");
        System.setProperty("minestom.entity-view-distance", "8");
        System.setProperty("minestom.dispatcher-threads", "2");

        minecraftServer = MinecraftServer.init();

        MinecraftServer.setBrandName(String.format("IceStom (%s)", MinecraftServer.getBrandName()));

        trackLibrary = new TrackLibrary();
        timeTrialManager = new TimeTrialManager();

        spawnInstance = new SpawnInstance();

        timetrialDatabase = new MemoryTimetrialDatabase();
    }

    @SuppressWarnings("UnstableApiUsage")
    public void start(String[] args) throws IOException {
        if (args.length == 2 && args[0].equals("--convert")) {
            Path path = Path.of(args[1]);

            System.out.println("Converting " + path + " to stomtrack.");

            PolarWorld world = AnvilPolar.anvilToPolar(path);

            String id = path.getFileName().toString();

            TrackFormat.saveTrack(TrackLibrary.TRACK_STORAGE_PATH.resolve(
                    id + "." + TrackFormat.FILE_EXTENTION).toFile(),
                    new Track(new MutableTrack(
                            id,
                            new Pos(-14.03, 17.00, 11.82, -205.65f, 0f),
                            Map.of(
                                    new LineCheckpoint(new Vec(-34.5, 17 ,-21.5), new Vec(-22.5, 17, -21.5), 3), 0,
                                    new LineCheckpoint(new Vec(-17.5, 17, -41.5), new Vec(-9.5, 17, -33.5), 3), 1,
                                    new LineCheckpoint(new Vec(22.5, 17, -53.5), new Vec(14.5, 17, -44.5), 3), 2,
                                    new LineCheckpoint(new Vec(40.5, 17, -1.5), new Vec(32.5, 17, -9.5), 3), 3,
                                    new LineCheckpoint(new Vec(0.5, 17, 18.5), new Vec(-0.5, 17, 7.5), 3), 4
                            )
                    ), world)
            );

            return;
        }

        trackLibrary.init();

        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new BoatCommand());
        commandManager.register(new TimeTrialCommand());
        commandManager.register(new DebugCommand());
        commandManager.register(new TrackCommand());
        commandManager.register(new SpawnCommand());

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        instanceManager.registerInstance(spawnInstance);

        spawnInstance.setup();

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(spawnInstance);
            player.setRespawnPoint(Pos.ZERO);
        });

        final Vec gravity_vec = new Vec(0, -0.04, 0);

        globalEventHandler.addListener(PlayerPacketOutEvent.class, playerPacketOutEvent -> {
            Player player = playerPacketOutEvent.getPlayer();
            Instance instanceContainer = player.getInstance();

            if (playerPacketOutEvent.getPacket() instanceof EntityVelocityPacket(int entityId, Vec velocity)) {
                Entity entity = instanceContainer.getEntityById(entityId);

                if (entity instanceof Boat boat) {
                    if (!boat.getPassengers().isEmpty() && velocity.samePoint(gravity_vec)) {
                        playerPacketOutEvent.setCancelled(true);
                    }
                }
            }
        });

        minecraftServer.start("0.0.0.0", 25565);
    }

    public TrackLibrary getTrackLibrary() { return trackLibrary; }
    public TimeTrialManager getTimeTrialManager() { return timeTrialManager; }
    public SpawnInstance getSpawnInstance() { return spawnInstance; }

    public TimetrialDatabase getTimetrialDatabase() { return timetrialDatabase; }

    public static void main(String[] args) throws IOException {
        instance = new IceStom();
        instance.start(args);
    }

    public static IceStom getInstance() {
        return instance;
    }
}