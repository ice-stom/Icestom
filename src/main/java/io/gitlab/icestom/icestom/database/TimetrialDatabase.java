package io.gitlab.icestom.icestom.database;

import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface TimetrialDatabase {
    void updateBestTime(UUID player, String track_id, TimedLapResultSource resultSource);
    @Nullable TimedLapResultSource getBestTime(UUID player, String track_id);
}