package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.RaceInstance;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public interface RaceParticipantEvent extends RaceEvent, PlayerEvent {
    @NotNull RaceInstance.RaceParticipant getParticipant();

    @Override
    default @NotNull Player getPlayer() {
        return getParticipant().getCurrentPlayer();
    }
}
