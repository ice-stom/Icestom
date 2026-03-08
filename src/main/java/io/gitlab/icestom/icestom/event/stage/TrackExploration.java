package io.gitlab.icestom.icestom.event.stage;

import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.checkpoint.TickMovement;
import io.gitlab.icestom.icestom.ui.ActionBarProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

import java.util.Map;

public class TrackExploration extends TrackInstance implements Stage<TrackInstance>, ActionBarProvider {
    public TrackExploration(Track track) {
        super(track);
    }

    @Override
    protected void onPlayerMovements(Map<Player, TickMovement> movements) {}

    @Override
    public TrackInstance getInstance() {
        return this;
    }

    @Override
    public Pos spawnLocation(Player player) {
        return Pos.ZERO;
    }

    @Override
    public Component getActionBar(Player player) {
        return Component.text("Exploring ").append(Component.text(track.getId(), NamedTextColor.GOLD));
    }
}
