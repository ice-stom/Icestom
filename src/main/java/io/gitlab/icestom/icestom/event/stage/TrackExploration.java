package io.gitlab.icestom.icestom.event.stage;

import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.checkpoint.TickMovement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class TrackExploration extends TrackInstance implements SingleInstanceStage<TrackInstance> {
    public TrackExploration(Track track) {
        super(track);
    }

    @Override
    protected void onPlayerMovements(Map<Player, TickMovement> movements) {}

    @Override
    protected boolean shouldTrackPlayer(Player player) {
        return false;
    }

    @Override
    public TrackInstance getInstance() {
        return this;
    }

    @Override
    public Pos spawnLocation(Player player) {
        return track.getSpawnLocation();
    }
}
