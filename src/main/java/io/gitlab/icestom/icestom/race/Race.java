package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.GridBoatHolder;
import io.gitlab.icestom.icestom.event.stage.SingleInstanceStage;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.race.event.RaceCheckpointReachedEvent;
import io.gitlab.icestom.icestom.race.event.RaceLapCompletedEvent;
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
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Race extends TrackInstance implements SingleInstanceStage<TrackInstance>, ActionBarProvider {

    private static final ScoreboardHolder<RaceScoreboardProvider> scoreboardHolder = new ScoreboardHolder<>(RaceScoreboardProvider.class);

    private final int totalLaps;
    private final int totalPits;

    private final Leaderboard leaderboard;

    private final Map<UUID, RaceParticipant> participants = new LinkedHashMap<>();
    private final List<UUID> start_order = new ArrayList<>();

    private RaceState raceState = RaceState.GRID;
    private int countdown = 0;

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

            @Nullable Race.RaceParticipant participation = getParticipant(player);

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
                    @Nullable Race.RaceParticipant participation = getParticipant(player);

                    if (participation != null) {
                        Split split = new Split(
                                getWorldAge() * 50,
                                tick_delta,
                                checkpoint_index
                        );

                        participation.nextCheckpoint(split);

                        leaderboard.update(participation, split);
                        updated.set(true);
                    }
                });
            }
        }

        if (updated.get()) scoreboardHolder.getProviders().forEach(provider -> provider.dispatchRaceLeaderboard(this));
    }

    @Override
    protected boolean shouldTrackPlayer(Player player) {
        return getParticipants().containsKey(getParticipationId(player)) && raceState == RaceState.RACE;
    }

    public void startCountdown() {
        raceState = RaceState.COUNTDOWN;

        // trust
        countdown = 10 * 20 + 1;
    }

    public void gridPlayer(Player player) {
        Pos grid_location = getGridLocation(player);

        if (grid_location == null) return;

        Boat boat = createBoat(player, grid_location);

        GridBoatHolder holder = new GridBoatHolder(this, grid_location);

        holder.addPassenger(boat);
    }

    public void startRace() {
        raceState = RaceState.RACE;

        for (UUID uuid : start_order) {
            @Nullable RaceParticipant participant = getParticipant(uuid);

            if (participant != null) {
                Player player = participant.getCurrentPlayer();

                if (player.getVehicle() instanceof Boat boat && boat.getVehicle() instanceof GridBoatHolder holder) {
                    holder.remove();
                }
            }
        }
    }

    public UUID getParticipationId(Player player) {
        return player.getUuid();
    }

    @Override
    public void resetPlayer(Player player) {
        if (raceState == RaceState.GRID) {
            gridPlayer(player);
        } else {
            removeBoat(player);
            player.teleport(track.getSpawnLocation());
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    @Override
    public void consume(Player player) {
        scoreboardHolder.init(player);

        // TODO: participation

        if (raceState == RaceState.GRID) {
            @Nullable UUID participation_id = getParticipationId(player);

            if (participation_id != null) {
                RaceParticipant participant = participants.computeIfAbsent(participation_id, player_id -> {
                    start_order.add(player_id);
                    return new RaceParticipant(this, participation_id, player);
                });

                leaderboard.addParticipant(participant);
            }

        }

        super.consume(player);

        MinecraftServer.getSchedulerManager()
                .scheduleNextTick(() -> scoreboardHolder.getProviders().forEach(provider -> provider.dispatchRaceLeaderboard(this)));
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (countdown > 0) {
            countdown--;

            if (countdown == 0) {
                forEachAudience(Audience::clearTitle);
                startRace();
            } else if (countdown % 20 == 0) {
                forEachAudience(audience -> audience.showTitle(
                        Title.title(Component.empty(), Component.text(countdown / 20))
                ));
            }
        }
    }

    @Override
    public void drop(Player player) {
        scoreboardHolder.uninit(player);
        player.setGameMode(GameMode.SURVIVAL);
        super.drop(player);
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
    public @Nullable Component getActionBar(Player player) {
        @Nullable Race.RaceParticipant participation = getParticipant(player);

        if (participation != null) {
            TimedLap lap = participation.getCurrentLap();

            int pos = leaderboard.getSnapshot().getPosition(participation) + 1;

            Component text = Component.empty()
                    .append(Component.text("P")
                            .decorate(TextDecoration.BOLD)
                            .append(Component.text(pos, TextColor.color(0x4fd3ff))))
                    .append(Component.space())
                    .append(Component.text(Math.max(0, participation.getCompletedLaps()) + "/" + getTotalLaps()))
                    .append(Component.space())
                    .append(TextFormatter.getTime(lap.getCurrentTime(getWorldAge())).color(NamedTextColor.YELLOW));

            if (participation.getCompletedLaps() > 0) {
                text = text.append(Component.text(" - "))
                        .append(TextFormatter.getDelta(lap.getRecentSplit()));
            }

            return text;
        }

        return null;
    }

    public @Nullable Pos getGridLocation(Player player) {
        @Nullable Race.RaceParticipant participation = getParticipant(player.getUuid());

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

    public int getTotalLaps() { return totalLaps; }
    public int getTotalPits() { return totalPits; }
    public Leaderboard getLeaderboard() { return leaderboard; }

    public Map<UUID, RaceParticipant> getParticipants() { return participants; }
    public @Nullable Race.RaceParticipant getParticipant(UUID uuid) { return participants.get(uuid); }
    public @Nullable Race.RaceParticipant getParticipant(Player player) {
        @Nullable UUID participation_id = getParticipationId(player);
        if (participation_id == null) return null;
        return getParticipant(participation_id);
    }

    public enum RaceState {
        GRID,
        COUNTDOWN,
        RACE
    }

    public class RaceParticipant {
        private final List<Split> splits = new ArrayList<>();
        private final List<TimedLapResultSource> pastLaps = new ArrayList<>();

        private final UUID id;
        private final Race race;

        @NotNull private Player currentPlayer;

        private TimedLapResultSource flap = null;

        @NotNull private TimedLap currentLap;
        private int globalCheckpointIndex = -1;
        private int nextExpected = 0;

        private int completedLaps = 0;
        private final int completedPits = 0;

        RaceParticipant(Race race, UUID id, @NonNull Player currentPlayer) {
            this.id = id;
            this.race = race;
            this.currentPlayer = currentPlayer;
            currentLap = new TimedLap(
                    track,
                    null
            );
        }

        public void nextCheckpoint(Split split) {
            TimedLapResultSource previous_flap = flap;

            boolean completed = currentLap.advanceCheckpoint(split);

            int this_checkpoint = nextExpected;

            splits.add(split);

            nextExpected++;

            globalCheckpointIndex++;

            if (completed) {
                TimedLapResultSource result = newLap(split);
                nextExpected = track.wrapCheckpointIndex(1);

                MinecraftServer.getGlobalEventHandler()
                        .call(new RaceLapCompletedEvent(this, race, result, previous_flap));
            } else {
                MinecraftServer.getGlobalEventHandler()
                        .call(new RaceCheckpointReachedEvent(this, race, currentLap, this_checkpoint));
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

        public long deltaTo(RaceParticipant other) {
            int max_checkpoint = Math.min(getSplits().size(), other.getSplits().size()) - 1;

            if (max_checkpoint < 0) return 0;

            Split local = getSplits().get(max_checkpoint);
            Split foreign = other.getSplits().get(max_checkpoint);

            long delta = local.ms() - foreign.ms();

            if (delta < 0) {
                // when someone overtakes, the most recent shared checkpoint is when the overtaken driver was still in the lead.
                // its pretty much impossible to guess a deltas for a checkpoint that doesn't exist yet
                // can't really do much about this so just assume 0 for now.
                return 0;
            }

            return delta;
        }

        public int getNextExpected() {
            return track.wrapCheckpointIndex(nextExpected);
        }

        public @NotNull TimedLap getCurrentLap() { return currentLap; }

        public List<Split> getSplits() {
            return splits;
        }

        public int getGlobalCheckpointIndex() { return globalCheckpointIndex; }

        public int getCompletedLaps() { return completedLaps; }

        public int getCompletedPits() { return completedPits; }

        public @NotNull Player getCurrentPlayer() { return currentPlayer; }

        public void setCurrentPlayer(@NonNull Player currentPlayer) { this.currentPlayer = currentPlayer; }

        public UUID getId() {
            return id;
        }
    }
}
