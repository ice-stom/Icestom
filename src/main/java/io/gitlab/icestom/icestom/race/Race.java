package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.GridBoatHolder;
import io.gitlab.icestom.icestom.event.stage.Stage;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.race.scoreboard.RaceScoreboardProvider;
import io.gitlab.icestom.icestom.timetrial.Split;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.checkpoint.TickMovement;
import io.gitlab.icestom.icestom.ui.ActionBarProvider;
import io.gitlab.icestom.icestom.ui.scoreboard.ScoreboardHolder;
import io.gitlab.icestom.icestom.util.TextFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Race extends TrackInstance implements Stage<TrackInstance>, ActionBarProvider {

    private final ScoreboardHolder<RaceScoreboardProvider> scoreboardHolder = new ScoreboardHolder<>(RaceScoreboardProvider.class);

    private final int totalLaps;
    private final int totalPits;

    private final Leaderboard leaderboard;

    private final Map<UUID, RaceParticipation> racers = new LinkedHashMap<>();
    private final List<UUID> start_order = new ArrayList<>();

    public Race(Track track, int totalLaps, int totalPits) {
        super(track);
        this.totalLaps = totalLaps;
        this.totalPits = totalPits;

        this.leaderboard = new Leaderboard(this);
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

        AtomicBoolean updated = new AtomicBoolean(false);

        for (Map.Entry<Integer, Map<Player, TickMovement>> integerMapEntry : grouped.entrySet()) {
            int checkpoint_index = integerMapEntry.getKey();

            for (Checkpoint checkpoint : track.getCheckpoints(checkpoint_index)) {
                Map<Player, Long> crosses = checkpoint.detectCrosses(integerMapEntry.getValue());

                crosses.forEach((player, tick_delta) -> {
                    @Nullable RaceParticipation participation = racers.get(player.getUuid());

                    if (participation != null) {
                        Split split = new Split(
                                getWorldAge() * 50,
                                tick_delta,
                                checkpoint_index
                        );

                        participation.nextCheckpoint(split);

                        leaderboard.update(player.getUuid(), split);
                        updated.set(true);
                    }
                });
            }
        }

        if (updated.get()) scoreboardHolder.getProviders().forEach(provider -> provider.dispatchRaceLeaderboard(this));
    }

    public int getTotalLaps() { return totalLaps; }
    public int getTotalPits() { return totalPits; }
    public Leaderboard getLeaderboard() { return leaderboard; }

    public Map<UUID, RaceParticipation> getParticipants() { return racers; }
    public @Nullable RaceParticipation getParticipant(UUID uuid) { return racers.get(uuid); }

    @Override
    public void resetPlayer(Player player) {
        gridPlayer(player);
    }

    @Override
    public void consume(Player player) {
        scoreboardHolder.init(player);

        // TODO: participation

        racers.computeIfAbsent(player.getUuid(), player_id -> {
            start_order.add(player_id);
            return new RaceParticipation(player);
        });
        leaderboard.addPlayer(player);

        super.consume(player);

        MinecraftServer.getSchedulerManager()
                .scheduleNextTick(() -> scoreboardHolder.getProviders().forEach(provider -> provider.dispatchRaceLeaderboard(this)));
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

    public @Nullable Pos getGridLocation(Player player) {
        @Nullable RaceParticipation participation = getParticipant(player.getUuid());

        if (participation != null) {
            int starting_pos = start_order.indexOf(player.getUuid());

            if (starting_pos == -1) return null;

            List<Pos> grid_locations = track.getGridLocations();

            if (starting_pos >= grid_locations.size()) {
                return track.getSpawnLocation();
            }

            return track.getGridLocations().get(starting_pos);
        }

        return null;
    }

    public void gridPlayer(Player player) {
        Pos grid_location = getGridLocation(player);

        if (grid_location == null) return;

        Boat boat = createBoat(player, grid_location);

        GridBoatHolder holder = new GridBoatHolder(this, grid_location);

        holder.addPassenger(boat);
    }

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

            splits.add(split);

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

        public long deltaTo(RaceParticipation other) {
            int max_checkpoint = Math.min(getSplits().size(), other.getSplits().size()) - 1;

            if (max_checkpoint < 0) return 0;

            Split local = getSplits().get(max_checkpoint);
            Split foreign = other.getSplits().get(max_checkpoint);

            long delta = local.ms() - foreign.ms();

            if (delta < 0) {
                // when someone overtakes, the most recent shared checkpoint is when the overtaken driver was still in the lead
                // its pretty much impossible to guess a deltas for a checkpoint that doesn't exist yet
                // can't really do much about this so just assume 0 for now.
                return 0;
            }

            return delta;
        }

        public int getNextExpected() {
            return track.wrapCheckpointIndex(nextExpected);
        }

        public List<Split> getSplits() {
            return splits;
        }

        public int getGlobalCheckpointIndex() { return globalCheckpointIndex; }

        public int getCompletedLaps() { return completedLaps; }

        public int getCompletedPits() { return completedPits; }
    }
}
