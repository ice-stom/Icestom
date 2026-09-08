package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.event.event.EventParticipantEvent;
import io.gitlab.icestom.icestom.race.RaceStage;

public interface RaceParticipantEvent extends RaceEvent, EventParticipantEvent {
    default RaceStage.RaceParticipant getRacer() {
        return getInstance().getRacer(getParticipant().getUuid());
    }
}
