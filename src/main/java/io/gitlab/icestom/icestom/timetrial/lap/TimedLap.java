package io.gitlab.icestom.icestom.timetrial.lap;

import io.gitlab.icestom.icestom.timetrial.Split;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.util.TextFormatter;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TimedLap implements TimedLapResultSource {

    private final Track track;
    @Nullable private final TimedLapResultSource bestPreviousResult;

    private final UUID uuid =UUID.randomUUID();

    private final List<Split> splits = new ArrayList<>();
    private final Map<Integer, Pos> ticks = new HashMap<>();

    private long msStart;
    private int lastReachedCheckpoint = -1;

    private long recentSplit = 0;

    public TimedLap(Track track, @Nullable TimedLapResultSource bestPreviousResult, Split split) {

        this.track = track;
        this.bestPreviousResult = bestPreviousResult;

        advanceCheckpoint(split);
    }

    public TimedLap(Track track, @Nullable TimedLapResultSource bestPreviousResult) {
        this.track = track;
        this.bestPreviousResult = bestPreviousResult;
    }

    public boolean advanceCheckpoint(Split split) {
        int this_checkpoint_index = track.wrapCheckpointIndex(lastReachedCheckpoint + 1);

        if (split.checkpoint_no() != this_checkpoint_index)
            throw new RuntimeException("Wrong checkpoint number sent to timed lap");

        if (lastReachedCheckpoint == -1) {
            msStart = split.ms();
        }

        Split local_split = split.offset(msStart);
        splits.add(local_split);

        boolean is_last = track.isLastCheckpoint(this_checkpoint_index) && lastReachedCheckpoint != -1;

        int globalIndex = ++lastReachedCheckpoint;

        if (bestPreviousResult != null) {
            if (bestPreviousResult.splits().size() <= globalIndex) {
                recentSplit = 0;
            } else {
                Split best_previous = bestPreviousResult.splits().get(globalIndex);
                recentSplit = local_split.ms() - best_previous.ms();
            }
        }

        return is_last;
    }

    public long getCurrentTime(long tick) {
        if (splits.isEmpty()) return 0;

        return tick * 50 - getMsStart();
    }

    public Component getActionBar(long tick) {
        long time = Math.max(0, tick * 50 - getMsStart());

        Component text = TextFormatter.getTimeRounded(time);

        if (bestPreviousResult != null) {
            if (lastReachedCheckpoint != 0) {
                text = text
                        .append(Component.space())
                        .append(TextFormatter.getDelta(recentSplit));
            } else {
                text = text
                        .append(Component.space());
            }
        }

        return text;
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    public long getMsStart() { return msStart; }
    public int getLastReachedCheckpoint() { return lastReachedCheckpoint; }

    public long getRecentSplit() { return recentSplit; }

    @Override
    public List<Split> splits() { return splits; }

    @Override
    public Map<Integer, Pos> ticks() { return ticks; }

    public @Nullable TimedLapResultSource getBestPreviousResult() {
        return bestPreviousResult;
    }
}
