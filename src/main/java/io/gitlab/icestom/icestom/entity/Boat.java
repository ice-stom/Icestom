package io.gitlab.icestom.icestom.entity;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import org.jetbrains.annotations.NotNull;

public class Boat extends Entity {
    public Boat() {
        super(EntityType.OAK_BOAT);

        eventNode().addListener(PlayerEntityInteractEvent.class, entityInteractEvent -> {
            if (getPassengers().size() >= 2) return;

            addPassenger(entityInteractEvent.getPlayer());
        });
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);
        player.sendPacket(this.getPassengersPacket());
    }
}
