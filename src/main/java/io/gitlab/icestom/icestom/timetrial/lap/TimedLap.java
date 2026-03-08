package io.gitlab.icestom.icestom.timetrial.lap;

import io.gitlab.icestom.icestom.timetrial.Split;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.ui.ActionBarProvider;
import io.gitlab.icestom.icestom.util.TextFormatter;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TimedLap implements TimedLapResultSource, ActionBarProvider {

    private final Track track;
    @Nullable private final TimedLapResultSource best_previous_result;

    private final List<Split> splits = new ArrayList<>();

    private long msStart;
    private int checkpoint = -1;

    private long recent_split = 0;

    public TimedLap(Track track, @Nullable TimedLapResultSource bestPreviousResult, Split split) {

        this.track = track;
        this.best_previous_result = bestPreviousResult;

        nextCheckpoint(split);
    }

    public TimedLap(Track track, @Nullable TimedLapResultSource bestPreviousResult) {
        this.track = track;
        this.best_previous_result = bestPreviousResult;
    }

    public boolean nextCheckpoint(Split split) {
        int next = track.wrapCheckpointIndex(checkpoint + 1);

        if (split.checkpoint_no() != next) throw new RuntimeException("Wrong checkpoint number sent to timetrial");

        if (checkpoint == -1) {
            msStart = split.ms();
        }

        Split local_tick = split.offset(msStart);

        splits.add(local_tick);

        if (next == 0 && checkpoint != -1) {
            checkpoint = -1;
            return true;
        }

        checkpoint++;

        if (best_previous_result != null) {
            Split best_previous = best_previous_result.getSplits().get(split.checkpoint_no());

            recent_split = local_tick.ms() - best_previous.ms();
        }

        return false;
    }

    @Override
    public Component getActionBar(Player player) {
        long age = player.getInstance().getWorldAge();
        long time = age * 50 - getMsStart();

        Component text = Component.text(String.format("%.2f", (float) Math.ceil((float) time / 50) * 50 / 1000));

        if (best_previous_result != null) {
            text = text
                    .append(Component.space())
                    .append(TextFormatter.getDelta(recent_split));
        }

        return text;
    }

    public long getMsStart() { return msStart; }
    public int getCheckpoint() { return checkpoint; }

    @Override
    public List<Split> getSplits() { return splits; }
}
