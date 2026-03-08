package io.gitlab.icestom.icestom.database.memory;

import io.gitlab.icestom.icestom.database.TimetrialDatabase;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MemoryTimetrialDatabase implements TimetrialDatabase {

    private final Map<PlayerTrackKey, TimedLapResultSource> bestTime = new HashMap<>();

    record PlayerTrackKey(
            Player player,
            String trackId
    ) {}

    @Override
    public void updateBestTime(Player player, String track_id, TimedLapResultSource resultSource) {
        bestTime.put(new PlayerTrackKey(player, track_id), resultSource);
    }

    @Override
    public @Nullable TimedLapResultSource getBestTime(Player player, String track_id) {
        return bestTime.get(new PlayerTrackKey(player, track_id));
    }
}
