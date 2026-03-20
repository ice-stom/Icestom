package io.gitlab.icestom.icestom.database;

import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface TimetrialDatabase {
    void updateBestTime(Player player, String track_id, TimedLapResultSource resultSource);
    @Nullable TimedLapResultSource getBestTime(Player player, String track_id);
}