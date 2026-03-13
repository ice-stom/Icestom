package io.gitlab.icestom.icestom.entity;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.SetPassengersPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

public class IceStomPlayer extends Player {
    public IceStomPlayer(@NotNull PlayerConnection playerConnection, GameProfile profile) {
        super(playerConnection, profile);
    }

    @Override
    public void updateNewViewer(@NotNull Player viewer) {
        super.updateNewViewer(viewer);

        viewer.sendPacket( new SetPassengersPacket(this.getEntityId(), this.getPassengers().stream().map(Entity::getEntityId).toList()));
    }
}
