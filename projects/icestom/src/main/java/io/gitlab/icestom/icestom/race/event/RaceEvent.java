package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.RaceStage;
import io.gitlab.icestom.icestom.track.event.TrackInstanceEvent;
import org.jetbrains.annotations.NotNull;

public interface RaceEvent extends TrackInstanceEvent {
    @NotNull RaceStage getInstance();
}
