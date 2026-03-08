package io.gitlab.icestom.icestom.trial;

import io.gitlab.icestom.icestom.instance.TimeTrialingInstance;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.util.TextFormatter;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TimeTrial implements TimetrialResultSource {

    private final Player player;
    private final Track track;
    @Nullable private final TimetrialResultSource best_previous_result;

    private final List<Split> splits = new ArrayList<>();

    private final long msStart;
    private int checkpoint = -1;

    public record Split(
            long ms,
            int checkpoint_no
    ) {
        public Split(long ms, long tick_delta, int checkpoint_no) {
            this(
                    ms + tick_delta,
                    checkpoint_no
            );
        }

        Split(Split split, long offset) {
            this(
                    split.ms() - offset,
                    split.checkpoint_no()
            );
        }

        int getOverallTick() {
            return (int) Math.floor((double) ms / 50);
        }
    }

    public TimeTrial(Player player, Track track, @Nullable TimetrialResultSource bestPreviousResult, Split split) {
        if (!(player.getInstance() instanceof TimeTrialingInstance)) throw new RuntimeException("Can't start a timetrial in a non-timetrialing instance");

        this.player = player;
        this.track = track;
        this.best_previous_result = bestPreviousResult;

        msStart = split.ms();

        nextCheckpoint(split);
    }

    public void nextCheckpoint(Split split) {
        int next = track.wrapCheckpointIndex(checkpoint + 1);

        if (split.checkpoint_no != next) throw new RuntimeException("Wrong checkpoint number sent to timetrial");

        Split local_tick = new Split(split, msStart);

        splits.add(local_tick);

        if (next == 0 && checkpoint != -1) {
            checkpoint = -1;
            return;
        }

        checkpoint++;

        player.sendMessage(Component.text(local_tick.checkpoint_no).append(Component.text(" ")).append(Component.text(local_tick.ms())));

        Component message = Component.empty()
                .append(Component.text("Checkpoint "))
                .append(Component.text(checkpoint))
                .append(Component.text(": "))
                .append(TextFormatter.getTime(local_tick.ms));

        if (best_previous_result != null) {
            Split best_previous = best_previous_result.getSplits().get(split.checkpoint_no());

            long delta = local_tick.ms() - best_previous.ms();

            message = message
                    .append(Component.space())
                    .append(TextFormatter.getDelta(delta));
        }

        player.sendMessage(message);
    }

    public long getMsStart() { return msStart; }
    public int getCheckpoint() { return checkpoint; }

    @Override
    public List<Split> getSplits() { return splits; }
}
