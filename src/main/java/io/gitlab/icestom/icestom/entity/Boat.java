package io.gitlab.icestom.icestom.entity;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.play.ClientVehicleMovePacket;
import net.minestom.server.network.packet.server.play.EntityHeadLookPacket;
import org.jetbrains.annotations.NotNull;

public class Boat extends Entity {
    public Boat() {
        super(EntityType.OAK_BOAT);

        hasPhysics = false;

        eventNode().addListener(PlayerEntityInteractEvent.class, entityInteractEvent -> {
            if (getPassengers().size() >= 2) return;

            addPassenger(entityInteractEvent.getPlayer());
        });

        eventNode().addListener(PlayerPacketEvent.class, event -> {
            if (event.getPacket() instanceof ClientVehicleMovePacket clientVehicleMovePacket) {
                teleport(clientVehicleMovePacket.position());
            }
        });
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);
        player.sendPacket(this.getPassengersPacket());
    }

    @Override
    protected void movementTick() {}

    @Override
    public void tick(long time) {
        super.tick(time);

        float yaw = getPosition().yaw();

        for (Entity passenger : getPassengers()) {
            if (passenger instanceof Player player) {
                sendPacketToViewers(new EntityHeadLookPacket(player.getEntityId(), yaw));
            }
        }
    }
}
