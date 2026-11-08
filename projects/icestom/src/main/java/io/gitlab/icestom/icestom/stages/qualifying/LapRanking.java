package io.gitlab.icestom.icestom.stages.qualifying;

import io.gitlab.icestom.icestom.event.stage.InvalidStageArgumentsException;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface LapRanking {

    @Nullable Long rank(List<Long> lapTimes);

    static LapRanking of(String name, int laps) {
        return switch (name) {
            case "fastest" -> new NthFastestLap(1);
            case "nth_fastest" -> new NthFastestLap(laps);
            case "consecutive" -> new BestConsecutiveLaps(laps);
            case "best_laps" -> new BestLaps(laps);
            default -> throw new InvalidStageArgumentsException(
                    "'ranking' must be one of: fastest, nth_fastest, consecutive or best_laps");
        };
    }

    static boolean isFaster(@Nullable Long time, @Nullable Long other) {
        return time != null && (other == null || time < other);
    }

    record NthFastestLap(int n) implements LapRanking {
        @Override
        public @Nullable Long rank(List<Long> lapTimes) {
            if (lapTimes.size() < n) return null;

            return lapTimes.stream().sorted().skip(n - 1).findFirst().orElseThrow();
        }
    }

    record BestLaps(int count) implements LapRanking {
        @Override
        public @Nullable Long rank(List<Long> lapTimes) {
            if (lapTimes.size() < count) return null;

            return lapTimes.stream().sorted().limit(count).mapToLong(Long::longValue).sum();
        }
    }

    record BestConsecutiveLaps(int count) implements LapRanking {
        @Override
        public @Nullable Long rank(List<Long> lapTimes) {
            if (lapTimes.size() < count) return null;

            long window = lapTimes.subList(0, count).stream().mapToLong(Long::longValue).sum();
            long best = window;

            for (int i = count; i < lapTimes.size(); i++) {
                window += lapTimes.get(i) - lapTimes.get(i - count);
                best = Math.min(best, window);
            }

            return best;
        }
    }
}
