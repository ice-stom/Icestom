package io.gitlab.icestom.icestom.entity;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.SetPassengersPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IceStomPlayer extends Player {

    private Integer openBoatUtilsVersion = null;

    public IceStomPlayer(@NotNull PlayerConnection playerConnection, GameProfile profile) {
        super(playerConnection, profile);
    }

    public void setOpenBoatUtilsVersion(Integer openBoatUtilsVersion) {
        this.openBoatUtilsVersion = openBoatUtilsVersion;
    }

    public @Nullable Integer getOpenBoatUtilsVersion() {
        return openBoatUtilsVersion;
    }

    @Override
    public void updateNewViewer(@NotNull Player viewer) {
        super.updateNewViewer(viewer);

        viewer.sendPacket( new SetPassengersPacket(this.getEntityId(), this.getPassengers().stream().map(Entity::getEntityId).toList()));
    }
}
