package io.gitlab.icestom.icestom.timetrial.lap;

import io.gitlab.icestom.icestom.timetrial.Split;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.util.TextFormatter;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TimedLap implements TimedLapResultSource {

    private final Track track;
    @Nullable private final TimedLapResultSource best_previous_result;

    private final List<Split> splits = new ArrayList<>();

    private long msStart;
    private int lastReachedCheckpoint = -1;

    private long recentSplit = 0;

    public TimedLap(Track track, @Nullable TimedLapResultSource bestPreviousResult, Split split) {

        this.track = track;
        this.best_previous_result = bestPreviousResult;

        advanceCheckpoint(split);
    }

    public TimedLap(Track track, @Nullable TimedLapResultSource bestPreviousResult) {
        this.track = track;
        this.best_previous_result = bestPreviousResult;
    }

    public boolean advanceCheckpoint(Split split) {
        int this_checkpoint_index = track.wrapCheckpointIndex(lastReachedCheckpoint + 1);

        if (split.checkpoint_no() != this_checkpoint_index) throw new RuntimeException("Wrong checkpoint number sent to timed lap");

        if (lastReachedCheckpoint == -1) {
            msStart = split.ms();
        }

        Split local_split = split.offset(msStart);

        splits.add(local_split);

        if (this_checkpoint_index == getFinalCheckpointIndex() && lastReachedCheckpoint != -1) {
            lastReachedCheckpoint = -1;
            return true;
        }

        lastReachedCheckpoint++;

        if (best_previous_result != null) {
            Split best_previous = best_previous_result.splits().get(split.checkpoint_no());

            recentSplit = local_split.ms() - best_previous.ms();
        }

        return false;
    }

    public int getFinalCheckpointIndex() {
        if (!track.isLooped()) {
            return track.getCheckpoints().size() - 1;
        }

        return 0;
    }

    public long getCurrentTime(long worldAge) {
        if (splits.isEmpty()) return 0;

        return worldAge * 50 - getMsStart();
    }

    public Component getActionBar(long worldAge) {
        long time = worldAge * 50 - getMsStart();

        float t_seconds = (float) Math.ceil((float) time / 50) * 50 / 1000;

        Component text = Component.text(String.format("%.2f", t_seconds));

        if (best_previous_result != null) {
            text = text
                    .append(Component.space())
                    .append(TextFormatter.getDelta(recentSplit));
        }

        return text;
    }

    public long getMsStart() { return msStart; }
    public int getLastReachedCheckpoint() { return lastReachedCheckpoint; }

    public long getRecentSplit() { return recentSplit; }

    @Override
    public List<Split> splits() { return splits; }
}
