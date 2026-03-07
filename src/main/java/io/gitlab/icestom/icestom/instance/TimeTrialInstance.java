package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.track.Track;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.InstanceTickEvent;

public class TimeTrialInstance extends TrackInstance {
    public TimeTrialInstance(Track track) {
        super(track);
    }

    @Override
    protected void tick(InstanceTickEvent event) {
        super.tick(event);

        for (Player player : getPlayers()) {
            if (player.getPosition().y() < -64) {
                resetPlayer(player);
            }
        }
    }

    public void resetPlayer(Player player) {
        Pos spawn = track.getSpawnLocation();
        Entity vehicle = player.getVehicle();

        if (vehicle != null) {
            vehicle.removePassenger(player);

            boolean noPlayersRemaining = vehicle.getPassengers().stream()
                    .noneMatch(entity -> entity instanceof Player);

            if (noPlayersRemaining) {
                vehicle.getPassengers().forEach(Entity::remove);
                vehicle.remove();
            }
        }

        createBoat(player, spawn);
    }

    public void consume(Player player) {
        if (player.getInstance() == this) {
            resetPlayer(player);
        } else {
            player.setInstance(this, track.getSpawnLocation())
                    .thenRun(() -> resetPlayer(player));
        }
    }
}
