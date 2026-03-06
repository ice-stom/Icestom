package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.track.Track;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.InstanceTickEvent;

@SuppressWarnings("UnstableApiUsage")
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
        Entity vehicle = player.getVehicle();

        if (vehicle != null) {
            vehicle.removePassenger(player);

            vehicle.getPassengers().forEach(Entity::remove);
            vehicle.remove();
        }

        putPlayerInBoat(player, track.getSpawnLocation());
    }

    public void consume(Player player) {
        player.setInstance(this, track.getSpawnLocation());
        resetPlayer(player);
    }
}
