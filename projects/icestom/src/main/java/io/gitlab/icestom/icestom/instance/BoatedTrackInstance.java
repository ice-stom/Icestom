package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.event.TrackRegionEnterEvent;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

import java.util.Set;

public abstract class BoatedTrackInstance extends TrackInstance {

    private static final String TELEPORT_PREFIX = "icestom.teleport_";

    protected BoatedTrackInstance(TrackLibrary.Ticket ticket, Track track) {
        super(ticket, track);

        track.getRegions().values().stream()
                .flatMap(Set::stream)
                .filter(tag -> tag.startsWith(TELEPORT_PREFIX))
                .forEach(this::subscribeRegionId);

        eventNode().addListener(TrackRegionEnterEvent.class, event -> {
            if (!event.getTag().startsWith(TELEPORT_PREFIX)) return;

            Pos target = track.getLocations().get(event.getTag().substring(TELEPORT_PREFIX.length()));
            if (target == null) return;

            final Player player = event.getPlayer();
            if (player.getVehicle() instanceof Boat) createBoat(player, target);
            else player.teleport(target);
        });
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
