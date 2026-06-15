package io.gitlab.icestom.icestom.database.memory;

import io.gitlab.icestom.icestom.database.TimetrialDatabase;
import io.gitlab.icestom.icestom.timetrial.lap.TimeTrialResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class MemoryTimetrialDatabase implements TimetrialDatabase {

    private final Map<PlayerTrackKey, TimeTrialResult> bestAttempt = new HashMap<>();
    private final Map<UUID, TimeTrialResult> attempts = new HashMap<>();

    record PlayerTrackKey(
            UUID player,
            String trackId
    ) {}

    @Override
    public @NonNull UUID newAttempt(TimeTrialResult result) {
        UUID id = UUID.randomUUID();
        PlayerTrackKey key = new PlayerTrackKey(result.player(), result.track());

        bestAttempt.merge(key, result, (existing, incoming) -> incoming.getTime() < existing.getTime() ? incoming : existing);
        attempts.put(id, result);

        return id;
    }

    @Override
    public TimeTrialResult getAttempt(UUID id) {
        return attempts.get(id);
    }

    @Override
    public @Nullable TimeTrialResult getBestAttempt(UUID player, String track_id) {
        return bestAttempt.get(new PlayerTrackKey(player, track_id));
    }

    @Override
    public @NotNull List<TimeTrialResult> getBestAttempts(String track_id, int number) {
        return bestAttempt.entrySet()
                .stream()
                .filter(entry -> entry.getKey().trackId().equals(track_id))
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparingLong(TimedLapResultSource::getTime))
                .limit(number)
                .toList();
    }
}
