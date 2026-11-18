package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.event.TrackRegionEnterEvent;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class BoatedTrackInstance extends TrackInstance {

    private static final Logger log = LoggerFactory.getLogger(BoatedTrackInstance.class);

    private static final String TELEPORT_PREFIX = "icestom.teleport_";

    protected final Map<UUID, TickLocation> lastPosition = new HashMap<>();
    private final Map<UUID, Long> teleportedAtTick = new HashMap<>();

    protected BoatedTrackInstance(TrackLibrary.Ticket ticket, Track track) {
        super(ticket, track);

        track.getRegions().values().stream()
                .flatMap(Set::stream)
                .filter(tag -> tag.startsWith(TELEPORT_PREFIX))
                .distinct()
                .forEach(tag -> {
                    if (!track.getLocations().containsKey(getTeleportTarget(tag))) {
                        log.warn("Teleport region '{}' on track {} has no location named '{}'", tag, track.getId(), getTeleportTarget(tag));
                    }

                    subscribeRegionId(tag);
                });

        eventNode().addListener(TrackRegionEnterEvent.class, event -> {
            if (!event.getTag().startsWith(TELEPORT_PREFIX)) return;

            Pos target = track.getLocations().get(getTeleportTarget(event.getTag()));
            if (target == null) return;

            final Player player = event.getPlayer();
            if (player.getVehicle() instanceof Boat) createBoat(player, target);
            else player.teleport(target);
        });
    }

    private static String getTeleportTarget(String tag) {
        return tag.substring(TELEPORT_PREFIX.length());
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

    @Override
    public CompletableFuture<Boat> createBoat(Player player, Pos pos) {
        teleportedAtTick.put(player.getUuid(), getPlayerTick(player.getUuid()));
        lastPosition.remove(player.getUuid());

        return super.createBoat(player, pos);
    }

    protected boolean isBeforeTeleport(TickLocation movement) {
        return movement.tick() <= teleportedAtTick.getOrDefault(movement.player(), -1L);
    }
}
