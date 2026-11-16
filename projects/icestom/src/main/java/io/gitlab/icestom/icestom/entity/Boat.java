package io.gitlab.icestom.icestom.entity;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.play.ClientVehicleMovePacket;
import net.minestom.server.network.packet.server.play.EntityHeadLookPacket;
import net.minestom.server.network.packet.server.play.EntityRotationPacket;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Boat extends Entity {

    private final Map<Integer, Pos> lastSentView = new HashMap<>();

    public Boat(Key type) {
        if (!type.namespace().equals(Key.MINECRAFT_NAMESPACE)) {
            /* Should probably check that it is actually a boat but oh well, this should catch anyone failing to rewrite the key in a custom handler */
            throw new RuntimeException("Invalid boat type " + type);
        }

        EntityType entityType = EntityType.fromKey(type);

        if (entityType == null) {
            throw new RuntimeException("Unknown boat type " + type);
        }

        super(entityType);

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

        lastSentView.keySet().removeIf(id -> getPassengers().stream().noneMatch(p -> p.getEntityId() == id));

        for (Entity passenger : getPassengers()) {
            if (!(passenger instanceof Player player)) continue;

            Pos view = player.getPosition();
            Pos last = lastSentView.put(player.getEntityId(), view);
            if (last != null && last.sameView(view)) continue;

            player.sendPacketsToViewers(
                    new EntityHeadLookPacket(player.getEntityId(), view.yaw()),
                    new EntityRotationPacket(player.getEntityId(), view.yaw(), view.pitch(), player.isOnGround())
            );
        }
    }
}
