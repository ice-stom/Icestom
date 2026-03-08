package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.entity.Boat;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.play.ClientTeleportConfirmPacket;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class BoatInstance extends IceStomInstance {
    public BoatInstance(Key key) {
        super(UUID.randomUUID(), DimensionType.OVERWORLD, key);
    }

    public void removeBoat(Player player) {
        Entity vehicle = player.getVehicle();

        if (vehicle != null) {
            vehicle.removePassenger(player);

            boolean noPlayersRemaining = vehicle.getPassengers().stream()
                    .noneMatch(entity -> entity instanceof Player);

            if (noPlayersRemaining) {
                vehicle.getPassengers().forEach(Entity::remove);
                vehicle.remove();
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public void createBoat(Player player, Pos pos) {
        EventListener<@NotNull PlayerPacketEvent> listener = EventListener.builder(PlayerPacketEvent.class)
                .filter(e -> e.getPlayer() == player && e.getPacket() instanceof ClientTeleportConfirmPacket)
                .handler(_ -> {
                    Boat boat = new Boat();

                    boat.setInstance(this, pos);

                    boat.addPassenger(player);
                })
                .expireCount(1)
                .build();

        eventNode().addListener(listener);

        player.teleport(pos.withDirection(player.getPosition().direction()));
    }
}
