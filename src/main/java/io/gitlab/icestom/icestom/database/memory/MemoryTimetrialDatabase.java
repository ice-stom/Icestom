package io.gitlab.icestom.icestom.database.memory;

import io.gitlab.icestom.icestom.database.TimetrialDatabase;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemoryTimetrialDatabase implements TimetrialDatabase {

    private final Map<PlayerTrackKey, TimedLapResultSource> bestTime = new HashMap<>();

    record PlayerTrackKey(
            UUID player,
            String trackId
    ) {}

    @Override
    public void updateBestTime(UUID player, String track_id, TimedLapResultSource resultSource) {
        bestTime.put(new PlayerTrackKey(player, track_id), resultSource);
    }

    @Override
    public @Nullable TimedLapResultSource getBestTime(UUID player, String track_id) {
        return bestTime.get(new PlayerTrackKey(player, track_id));
    }
}
