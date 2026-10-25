package io.gitlab.icestom.icestom.track.event;

import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.track.PlayerMovement;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jspecify.annotations.NonNull;

public class TrackTriggerPassEvent implements TrackColliderEvent, PlayerMovementEvent, PlayerEvent {

    private final String tag;
    private final TrackInstance instance;
    private final PlayerMovement playerMovement;
    private final Player player;

    public TrackTriggerPassEvent(String tag, TrackInstance instance, PlayerMovement playerMovement, Player player) {
        this.tag = tag;
        this.instance = instance;
        this.playerMovement = playerMovement;
        this.player = player;
    }

    @Override
    public String getTag() {
        return tag;
    }

    public @NonNull TrackInstance getInstance() {
        return instance;
    }

    @Override
    public PlayerMovement getPlayerMovement() {
        return playerMovement;
    }

    @Override
    public @NonNull Player getPlayer() {
        return player;
    }
}