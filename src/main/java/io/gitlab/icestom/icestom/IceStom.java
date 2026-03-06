package io.gitlab.icestom.icestom;

import io.gitlab.icestom.icestom.command.BoatCommand;
import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.instance.TestInstance;
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

public class IceStom {

    private static IceStom instance;

    private final MinecraftServer minecraftServer;

    IceStom() {
        System.setProperty("minestom.chunk-view-distance", "8");
        System.setProperty("minestom.entity-view-distance", "8");
        System.setProperty("minestom.dispatcher-threads", "2");

        minecraftServer = MinecraftServer.init();
    }

    @SuppressWarnings("UnstableApiUsage")
    public void start() {
        CommandManager commandManager = MinecraftServer.getCommandManager();

        commandManager.register(new BoatCommand());

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        TestInstance testInstance = new TestInstance();

        instanceManager.registerInstance(testInstance);

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(testInstance);
            player.setRespawnPoint(new Pos(0, 42, 0));
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

    public static void main(String[] args) {
        instance = new IceStom();
        instance.start();
    }

    public static IceStom getInstance() {
        return instance;
    }
}