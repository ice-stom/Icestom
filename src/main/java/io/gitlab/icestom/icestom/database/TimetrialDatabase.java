package io.gitlab.icestom.icestom.database;

import io.gitlab.icestom.icestom.timetrial.lap.TimeTrialResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface TimetrialDatabase {
    @NotNull UUID newAttempt(TimeTrialResult result);
    @Nullable TimeTrialResult getAttempt(UUID attempt);
    @Nullable TimeTrialResult getBestAttempt(UUID player, String track_id);
    @NotNull List<TimeTrialResult> getBestAttempts(String track_id, int number);
}