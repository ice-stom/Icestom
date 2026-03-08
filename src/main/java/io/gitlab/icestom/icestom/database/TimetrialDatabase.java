package io.gitlab.icestom.icestom.database;

import io.gitlab.icestom.icestom.timetrial.TimetrialResultSource;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface TimetrialDatabase {
    void updateBestTime(Player player, String track_id, TimetrialResultSource resultSource);
    @Nullable TimetrialResultSource getBestTime(Player player, String track_id);
}
