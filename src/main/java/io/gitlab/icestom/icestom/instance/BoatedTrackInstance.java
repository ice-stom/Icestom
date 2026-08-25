package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.track.Track;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

public abstract class BoatedTrackInstance extends TrackInstance {
    public BoatedTrackInstance(Track track) {
        super(track);
    }

    @Override
    public void resetPlayer(Player player) {
        Pos spawn = track.getSpawnLocation();
        float pitch = player.getPosition().pitch();

        removeBoat(player);

        super.resetPlayer(player);

        createBoat(player, spawn);
        player.teleport(player.getPosition().withPitch(pitch));
    }
}
