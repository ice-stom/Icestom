package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.GridBoatHolder;
import io.gitlab.icestom.icestom.event.stage.SingleInstanceStage;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.leaderboard.LeaderboardParticipant;
import io.gitlab.icestom.icestom.race.event.RaceCheckpointReachedEvent;
import io.gitlab.icestom.icestom.race.event.RaceLapCompletedEvent;
import io.gitlab.icestom.icestom.race.event.RaceLapTimerEvent;
import io.gitlab.icestom.icestom.race.event.RaceLeaderboardUpdateEvent;
import io.gitlab.icestom.icestom.timetrial.Split;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.gitlab.icestom.icestom.track.TickMovement;
import io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager.getHolder;

public class RaceInstance extends TrackInstance implements SingleInstanceStage<TrackInstance> {
    private final InterfaceManager.InterfaceHolder interfaceHolder = getHolder(RaceInstance.class, this);

    private final int totalLaps;
    private final int totalPits;

    private final RaceLeaderboard<RaceLeaderboardRow> raceLeaderboard;

    private final Map<UUID, RaceParticipant> participants = new LinkedHashMap<>();
    private final List<UUID> startOrder = new ArrayList<>();

    private RaceState raceState = RaceState.GRID;
    private int countdown = 0;

    public RaceInstance(Track track, int totalLaps, int totalPits) {
        super(track);
        this.totalLaps = totalLaps;
        this.totalPits = totalPits;

        this.raceLeaderboard = new RaceLeaderboard<>(participant -> new RaceLeaderboardRow(
                participant,
                0,
                null,
                -1,
                -1,
                false,
                false
        ), this);
    }

    @Override
    protected void onPlayerMovements(Map<Player, TickMovement> movements, Map<Player, Set<String>> inside_tags, Map<Player, Map<String, Long>> crossed_triggers) {
        Map<Integer, Map<Player, TickMovement>> grouped = new HashMap<>();

        for (Map.Entry<Player, TickMovement> entry : movements.entrySet()) {
            Player player = entry.getKey();
            TickMovement movement = entry.getValue();

            @Nullable RaceInstance.RaceParticipant participation = getParticipant(player);

            if (participation != null) {
                grouped
                        .computeIfAbsent(participation.getNextExpected(), _ -> new HashMap<>())
                        .put(player, movement);
            }
        }

        AtomicBoolean updated = new AtomicBoolean(false);

        for (Map.Entry<Integer, Map<Player, TickMovement>> integerMapEntry : grouped.entrySet()) {
            int checkpoint_index = integerMapEntry.getKey();

            for (CrossCollider checkpoint : track.getCheckpoints(checkpoint_index)) {
                Map<Player, Long> crosses = checkpoint.detectCrosses(integerMapEntry.getValue());

                crosses.forEach((player, tick_delta) -> {
                    @Nullable RaceInstance.RaceParticipant participation = getParticipant(player);

                    if (participation != null) {
                        Split split = new Split(
                                getWorldAge() * 50,
                                tick_delta,
                                checkpoint_index
                        );

                        participation.nextCheckpoint(split);

                        raceLeaderboard.update(participation, split);
                        updated.set(true);
                    }
                });
            }
        }

        if (updated.get()) {
            MinecraftServer.getGlobalEventHandler()
                    .call(new RaceLeaderboardUpdateEvent(this));
        }
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

        for (UUID uuid : startOrder) {
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
        interfaceHolder.startWatching(player);

        if (raceState == RaceState.GRID) {
            @Nullable UUID participation_id = getParticipationId(player);

            if (participation_id != null) {
                RaceParticipant participant = participants.computeIfAbsent(participation_id, player_id -> {
                    startOrder.add(player_id);
                    return new RaceParticipant(this, participation_id, player);
                });

                raceLeaderboard.addParticipant(participant);
            }

        }


        super.consume(player);

        MinecraftServer.getSchedulerManager()
                .execute(() -> MinecraftServer.getGlobalEventHandler()
                        .call(new RaceLeaderboardUpdateEvent(this)));
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        for (RaceParticipant participant : participants.values()) {
            MinecraftServer.getGlobalEventHandler()
                    .call(new RaceLapTimerEvent(this, participant));
        }

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
        interfaceHolder.stopWatching(player);

        MinecraftServer.getGlobalEventHandler()
                .call(new RaceLeaderboardUpdateEvent(this));

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

    public @Nullable Pos getGridLocation(Player player) {
        @Nullable RaceInstance.RaceParticipant participation = getParticipant(player.getUuid());

        if (participation != null) {
            int starting_pos = startOrder.indexOf(player.getUuid());

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
    public RaceLeaderboard<RaceLeaderboardRow> getLeaderboard() { return raceLeaderboard; }

    public Map<UUID, RaceParticipant> getParticipants() { return participants; }
    public @Nullable RaceInstance.RaceParticipant getParticipant(UUID uuid) { return participants.get(uuid); }
    public @Nullable RaceInstance.RaceParticipant getParticipant(Player player) {
        @Nullable UUID participation_id = getParticipationId(player);
        if (participation_id == null) return null;
        return getParticipant(participation_id);
    }

    public enum RaceState {
        GRID,
        COUNTDOWN,
        RACE
    }

    public class RaceParticipant implements LeaderboardParticipant<RaceParticipant> {
        private final List<Split> splits = new ArrayList<>();
        private final List<TimedLapResultSource> pastLaps = new ArrayList<>();

        private final UUID id;
        private final RaceInstance race;

        @NotNull private Player currentPlayer;

        private TimedLapResultSource flap = null;

        @NotNull private TimedLap currentLap;
        private int globalCheckpointIndex = -1;
        private int nextExpected = 0;

        private int completedLaps = 0;
        private final int completedPits = 0;

        RaceParticipant(RaceInstance raceInstance, UUID id, @NotNull Player currentPlayer) {
            this.id = id;
            this.race = raceInstance;
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

        public void setCurrentPlayer(@NotNull Player currentPlayer) { this.currentPlayer = currentPlayer; }

        public UUID getId() {
            return id;
        }
    }
}
