package io.gitlab.icestom.icestom.event.stage;

import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.TickMovement;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

import java.util.Map;
import java.util.Set;

public class TrackExploration extends TrackInstance implements SingleInstanceStage<TrackInstance> {
    public TrackExploration(Track track) {
        super(track);
    }

    @Override
    protected void onPlayerMovements(Map<Player, TickMovement> movements, Map<Player, Set<String>> inside_tags, Map<Player, Map<String, Long>> crossed_triggers) {}

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
