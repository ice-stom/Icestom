package io.gitlab.icestom.icestom.entity;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.client.play.ClientVehicleMovePacket;
import net.minestom.server.network.packet.server.play.EntityHeadLookPacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class Boat extends Entity {

    public Boat(Key type) {
        if (!type.namespace().equals(Key.MINECRAFT_NAMESPACE)) {
            /* Should probably check that it is actually a boat but oh well, this should catch anyone failing to rewrite the key in a custom handler */
            throw new RuntimeException("Invalid boat type " + type);
        }

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
