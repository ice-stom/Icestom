package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.race.RaceStage;
import org.jetbrains.annotations.NotNull;

public class RaceCompletedEvent implements RaceEvent, RaceParticipantEvent {

    private final EventParticipant eventParticipant;
    private final RaceStage instance;

    public RaceCompletedEvent(EventParticipant eventParticipant, RaceStage instance) {
        this.eventParticipant = eventParticipant;
        this.instance = instance;
    }

    @Override
    public EventParticipant getParticipant() {
        return eventParticipant;
    }

    @Override
    public @NotNull RaceStage getInstance() {
        return instance;
    }
}
