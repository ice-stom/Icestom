package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.event.stage.Stage;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.timetrial.Split;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.checkpoint.TickMovement;
import io.gitlab.icestom.icestom.ui.ActionBarProvider;
import io.gitlab.icestom.icestom.util.TextFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Race extends TrackInstance implements Stage<TrackInstance>, ActionBarProvider {

    private final int totalLaps;
    private final int totalPits;

    private final Map<UUID, RaceParticipation> racers = new LinkedHashMap<>();

    public class RaceParticipation {
        private final List<Split> splits = new ArrayList<>();
        private final List<TimedLapResultSource> pastLaps = new ArrayList<>();

        private final Player player;

        private TimedLapResultSource flap = null;

        private TimedLap currentLap;
        private int globalCheckpointIndex = -1;
        private int nextExpected = 0;

        private int completedLaps = 0;
        private final int completedPits = 0;

        RaceParticipation(Player player) {
            this.player = player;
            currentLap = new TimedLap(
                    track,
                    null
            );
        }

        public void nextCheckpoint(Split split) {

            TimedLapResultSource previous_flap = flap;

            boolean completed = currentLap.nextCheckpoint(split);

            int this_checkpoint = nextExpected;

            nextExpected++;

            globalCheckpointIndex++;

            if (completed) {
                TimedLapResultSource lap = newLap(split);
                nextExpected = track.wrapCheckpointIndex(1);

                completedLaps++;

                Component text = TextFormatter.getTime(lap.getTime());

                if (previous_flap != null) {
                    text = text.append(Component.space()).append(TextFormatter.getDelta(lap.getTime() - previous_flap.getTime()));
                }

                player.sendMessage(text);
            } else {
                Component text = Component.text(String.format("%s (%s) ", this_checkpoint, globalCheckpointIndex));

                if (flap != null) {
                    text = text.append(TextFormatter.getDelta(currentLap.getSplitTime(this_checkpoint) - previous_flap.getSplitTime(this_checkpoint)));
                }

                player.sendMessage(text);
            }
        }

        private TimedLapResultSource newLap(Split split) {
            TimedLapResultSource lap = TimedLapResult.freeze(currentLap);

            pastLaps.add(lap);

            completedLaps++;

            if (flap == null || lap.getTime() < flap.getTime()) {
                flap = lap;
            }

            currentLap = new TimedLap(
                    track,
                    flap,
                    split
            );

            return lap;
        }

        public int getNextExpected() {
            return track.wrapCheckpointIndex(nextExpected);
        }

        public List<Split> getSplits() {
            return splits;
        }
    }

    public Race(Track track, int totalLaps, int totalPits) {
        super(track);
        this.totalLaps = totalLaps;
        this.totalPits = totalPits;
    }

    @Override
    protected void onPlayerMovements(Map<Player, TickMovement> movements) {
        Map<Integer, Map<Player, TickMovement>> grouped = new HashMap<>();

        for (Map.Entry<Player, TickMovement> entry : movements.entrySet()) {
            Player player = entry.getKey();
            TickMovement movement = entry.getValue();

            @Nullable RaceParticipation participation = racers.get(player.getUuid());

            if (participation != null) {
                grouped
                        .computeIfAbsent(participation.getNextExpected(), _ -> new HashMap<>())
                        .put(player, movement);
            }
        }

        for (Map.Entry<Integer, Map<Player, TickMovement>> integerMapEntry : grouped.entrySet()) {
            int checkpoint_index = integerMapEntry.getKey();

            for (Checkpoint checkpoint : track.getCheckpoints(checkpoint_index)) {
                Map<Player, Long> crosses = checkpoint.detectCrosses(integerMapEntry.getValue());

                crosses.forEach((player, tick_delta) -> {
                    @Nullable RaceParticipation participation = racers.get(player.getUuid());

                    if (participation != null) {
                        participation.nextCheckpoint(new Split(
                                getWorldAge() * 50,
                                tick_delta,
                                checkpoint_index
                        ));

                        // TODO: leaderboard
                    }
                });
            }
        }

    }

    public Map<UUID, RaceParticipation> getParticipants() {
        return racers;
    }

    @Override
    public void consume(Player player) {
        super.consume(player);
        racers.computeIfAbsent(player.getUuid(), _ -> new RaceParticipation(player));
    }

    @Override
    public TrackInstance getInstance() {
        return this;
    }

    @Override
    public Pos spawnLocation(Player player) {
        return track.getSpawnLocation();
    }

    @Override
    public Component getActionBar(Player player) {
        return Component.text("Racing ").append(Component.text(track.getId(), NamedTextColor.GOLD));
    }
}
