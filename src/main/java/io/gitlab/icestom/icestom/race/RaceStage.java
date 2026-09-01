package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.GridBoatHolder;
import io.gitlab.icestom.icestom.event.*;
import io.gitlab.icestom.icestom.event.lua.ParticipantStore;
import io.gitlab.icestom.icestom.instance.BoatedTrackInstance;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.leaderboard.LeaderboardParticipant;
import io.gitlab.icestom.icestom.race.event.RaceCompletedEvent;
import io.gitlab.icestom.icestom.race.event.RaceLapTimerEvent;
import io.gitlab.icestom.icestom.race.event.RaceLeaderboardUpdateEvent;
import io.gitlab.icestom.icestom.race.event.RaceTimedLapCompletedEvent;
import io.gitlab.icestom.icestom.timetrial.Split;
import io.gitlab.icestom.icestom.timetrial.event.TimedLapCheckpointAdvancedEvent;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import io.gitlab.icestom.icestom.track.TickMovement;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.gitlab.icestom.icestom.ui.event.GenericErrorMessageEvent;
import io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager.getHolder;

public class RaceStage extends BoatedTrackInstance implements EventStage, ParticipantStoreHolder {

    private final InterfaceManager.InterfaceHolder interfaceHolder = getHolder(RaceStage.class, this);

    private final ParticipantStore participants = new ParticipantStore();
    private final List<EventParticipant> startOrder = new ArrayList<>();

    private final Map<UUID, RaceParticipant> racers = new HashMap<>();
    private final Map<RaceParticipant, UUID> racersLookup = new HashMap<>();

    private final RaceLeaderboard<RaceLeaderboardRow> raceLeaderboard;

    private final int totalLaps;
    private final int totalPits;

    private int countdown = 0;
    private int chequeredFlagTicks = 0;

    private RaceState raceState = RaceState.GRID;

    private final CompletableFuture<List<Result<EventParticipant>>> future = new CompletableFuture<>();

    public RaceStage(Track track, int totalLaps, int totalPits) {
        super(track);

        this.totalLaps = totalLaps;
        this.totalPits = totalPits;

        this.raceLeaderboard = new RaceLeaderboard<>(participant -> new RaceLeaderboardRow(participant, 0), this);

        subscribeRegionId("icestom.reset");
        subscribeTriggerId("icestom.reset");
    }

    public static CompletableFuture<RaceStage> create(Map<String, Object> options) {
        if (!(options.get("track") instanceof String track_id)) {
            return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'track' isn't a string"));
        }

        if (!(options.get("laps") instanceof Double dLaps)) {
            return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'laps' isn't a number"));
        }

        if (!(options.get("pits") instanceof Double dPits)) {
            return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'pits' isn't a number"));
        }

        int laps = (int) Math.floor(dLaps);
        int pits = (int) Math.floor(dPits);

        if (laps <= 0) return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'laps' is <= 0"));
        if (pits < 0) return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'pits' is <= 0"));

        return IceStom.getInstance().getTrackLibrary()
                .loadTrack(track_id)
                .thenApply(Optional::get)
                .thenApply(track1 -> new RaceStage(track1, laps, pits));
    }

    @Override
    protected void onPlayerMovements(Map<Player, TickMovement> movements, Map<Player, Set<String>> inside_regions, Map<Player, Map<String, Long>> crossed_triggers) {
        Map<Integer, Map<Player, TickMovement>> grouped = new HashMap<>();

        for (Map.Entry<Player, TickMovement> entry : movements.entrySet()) {
            Player player = entry.getKey();
            TickMovement movement = entry.getValue();

            Set<String> playerRegions = inside_regions.get(player);
            Map<String, Long> playerTriggers = crossed_triggers.get(player);

            @Nullable EventParticipant participation = participants.getParticipantFromActivePlayer(player);

            if (participation != null) {
                RaceParticipant raceParticipant = racers.get(participation.getUuid());

                if (raceParticipant == null || raceParticipant.isFinished()) continue;

                grouped.computeIfAbsent(raceParticipant.getNextExpected(), _ -> new HashMap<>())
                        .put(player, movement);

                TimedLap timedLap = raceParticipant.getCurrentLap();

                TrackInstance.tickResetRegions(this, player, playerRegions, playerTriggers, timedLap);
            }
        }

        AtomicBoolean updated = new AtomicBoolean(false);

        for (Map.Entry<Integer, Map<Player, TickMovement>> integerMapEntry : grouped.entrySet()) {
            int checkpoint_index = integerMapEntry.getKey();

            for (CrossCollider checkpoint : track.getCheckpoints(checkpoint_index)) {
                Map<Player, Long> crosses = checkpoint.detectCrosses(integerMapEntry.getValue());

                crosses.forEach((player, tick_delta) -> {
                    @Nullable EventParticipant participation = participants.getParticipantFromActivePlayer(player);

                    if (participation != null) {
                        RaceParticipant raceParticipant = racers.get(participation.getUuid());

                        if (raceParticipant == null || raceParticipant.isFinished()) return;

                        Split split = new Split(
                                getWorldAge() * 50,
                                tick_delta,
                                checkpoint_index
                        );

                        TimedLap last = raceParticipant.getCurrentLap();

                        @Nullable TimedLap finished = raceParticipant.nextCheckpoint(split);

                        MinecraftServer.getGlobalEventHandler()
                                .call(new TimedLapCheckpointAdvancedEvent(last, player));

                        if (finished != null) {
                            MinecraftServer.getGlobalEventHandler()
                                    .call(new RaceTimedLapCompletedEvent(participation, finished, this));

                            if (raceParticipant.isFinished()) {
                                MinecraftServer.getGlobalEventHandler()
                                        .call(new RaceCompletedEvent(participation, this));

                                if (raceState == RaceState.RACE) finishRace();
                            }
                        }

                        raceLeaderboard.update(raceParticipant, split);
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
    public void tick(long time) {
        super.tick(time);

        for (EventParticipant participant : participants.getParticipants()) {
            RaceParticipant raceParticipant = racers.get(participant.getUuid());

            if (!raceParticipant.isFinished()) {
                MinecraftServer.getGlobalEventHandler()
                        .call(new RaceLapTimerEvent(participant, raceParticipant.getCurrentLap(), this));
            }
        }

        if (countdown > 0) {
            countdown--;

            if (countdown == 0) {
                forEachAudience(Audience::clearTitle);
                startRace();
            } else if (countdown % 20 == 0) {
                // TODO: replace this with UI hooks
                forEachAudience(audience -> audience.showTitle(
                        Title.title(Component.text(countdown / 20), Component.empty())
                ));
            }
        }

        if (chequeredFlagTicks > 0) {
            chequeredFlagTicks--;

            if (chequeredFlagTicks == 0) {
                endRace();
            } else if (chequeredFlagTicks % 20 == 0) {
                // TODO: replace this with UI hooks
                forEachAudience(audience -> {
                    audience.sendMessage(Component.text("Race end in " + (chequeredFlagTicks / 20)));
                });
            }
        }
    }

    public void startCountdown() {
        if (raceState != RaceState.GRID) return;
        raceState = RaceState.COUNTDOWN;

        // trust
        countdown = 10 * 20 + 1;
    }

    public void startRace() {
        if (raceState != RaceState.COUNTDOWN) return;
        raceState = RaceState.RACE;

        for (Player player : participants.getActivelyParticipatingPlayers()) {
            if (player.getVehicle() instanceof Boat boat && boat.getVehicle() instanceof GridBoatHolder holder) {
                holder.remove();
            }
        }
    }

    public void finishRace() {
        if (raceState != RaceState.RACE) return;
        raceState = RaceState.CHEQUERED_FLAG;

        chequeredFlagTicks = 10 * 20;
    }

    public void endRace() {
        if (raceState != RaceState.CHEQUERED_FLAG) return;
        raceState = RaceState.END;

        List<Result<EventParticipant>> results = new ArrayList<>();

        for (RaceLeaderboardRow row : raceLeaderboard.getSnapshot().getRows()) {
            RaceParticipant racer = row.getParticipant();
            EventParticipant participant = participants.getParticipantFromId(getParticipantId(
                    racer
            ));

            Result<EventParticipant> result = new Result<>(participant);

            // TODO: figure out how i'm gonna put tables n shit in here for splits / lap times

            result.set(Key.key(IceStom.NAMESPACE, "completed_laps"), racer.getCompletedLaps());
            result.set(Key.key(IceStom.NAMESPACE, "completed_pits"), racer.getCompletedLaps());

            results.add(result);
        }

        future.complete(results);
    }

    @Override
    public ParticipantStore getParticipants() {
        return participants;
    }

    public UUID getParticipantId(RaceParticipant raceParticipant) {
        return racersLookup.get(raceParticipant);
    }

    @Override
    public CompletableFuture<List<Result<EventParticipant>>> begin(List<Result<EventParticipant>> results) {

        for (Result<EventParticipant> result : results) {
            EventParticipant participant = result.getParticipant();

            participants.addParticipant(participant);
            RaceParticipant racer = racers.computeIfAbsent(participant.getUuid(), _ -> new RaceParticipant());
            racersLookup.put(racer, participant.getUuid());

            startOrder.add(participant);
            raceLeaderboard.addParticipant(racer);
        }

        MinecraftServer.getInstanceManager()
                .registerInstance(this);

        EventStage.super.teleportAllParticipants(results);

        return future;
    }

    @Override
    protected boolean shouldTrackPlayer(Player player) {
        return (raceState == RaceState.RACE || raceState == RaceState.CHEQUERED_FLAG) && participants.isPlayerActivelyParticipating(player);
    }

    public @Nullable Pos getGridLocation(@NotNull EventParticipant participation) {
        int starting_pos = startOrder.indexOf(participation);

        if (starting_pos == -1) return null;

        List<Pos> grid_locations = track.getGridLocations();

        if (starting_pos >= grid_locations.size()) {
            int over = starting_pos - grid_locations.size(); // this will be extremely funny - crazylegs
            return track.getSpawnLocation().add(0, over, 0);
        }

        return track.getGridLocations().get(starting_pos);
    }

    public boolean gridPlayer(Player player) {
        EventParticipant participant = participants.getParticipantFromActivePlayer(player);

        Pos grid_location = getGridLocation(participant);

        if (grid_location == null) return false;

        Boat boat = createBoat(player, grid_location);

        GridBoatHolder holder = new GridBoatHolder(this, grid_location);

        holder.addPassenger(boat);

        return true;
    }

    public RaceLeaderboard<RaceLeaderboardRow> getRaceLeaderboard() {
        return raceLeaderboard;
    }

    public int getTotalLaps() {
        return totalLaps;
    }

    public int getTotalPits() {
        return totalPits;
    }

    public RaceParticipant getRacer(UUID uuid) {
        return racers.get(uuid);
    }

    @Override
    public Pos spawnLocation(Player player) {
        return track.getSpawnLocation();
    }

    @Override
    public void resetPlayer(Player player) {
        EventParticipant participant = participants.getParticipantFromActivePlayer(player);

        if (raceState == RaceState.GRID && participant != null) {
            if (!gridPlayer(player)) {
                MinecraftServer.getGlobalEventHandler()
                        .call(new GenericErrorMessageEvent(player, Component.translatable("message.race.error.failed_to_grid")));
            }
        } else {
            removeBoat(player);
            player.teleport(track.getSpawnLocation());
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    @Override
    public void consume(Player player) {
        interfaceHolder.startWatching(player);
        super.consume(player);
    }

    @Override
    public void drop(Player player) {
        interfaceHolder.stopWatching(player);
        super.drop(player);
    }

    @Override
    public void cleanup() {
        MinecraftServer.getInstanceManager()
                .unregisterInstance(this);
    }

    public enum RaceState {
        GRID,
        COUNTDOWN,
        RACE,
        CHEQUERED_FLAG,
        END
    }

    public class RaceParticipant implements LeaderboardParticipant<RaceParticipant> {
        private final List<Split> raceSplits = new ArrayList<>();
        private final List<TimedLapResultSource> completedLaps = new ArrayList<>();

        private TimedLapResultSource flap = null;

        private TimedLap currentLap;
        private int globalCheckpointIndex = -1;
        private int nextExpected = 0;

        private int completedLapCount = 0;
        private final int completedPitCount = 0;

        private boolean finished = false;

        RaceParticipant() {
            currentLap = new TimedLap(
                    track,
                    null
            );
        }

        public @Nullable TimedLap nextCheckpoint(Split split) {
            if (finished) return null;

            boolean completed = currentLap.advanceCheckpoint(split);

            raceSplits.add(split);

            nextExpected++;
            globalCheckpointIndex++;

            if (!completed) return null;

            TimedLap old = newLap(split);
            nextExpected = track.wrapCheckpointIndex(1);

            return old;
        }

        private TimedLap newLap(Split split) {
            TimedLapResultSource lap = TimedLapResult.freeze(currentLap);

            completedLaps.add(lap);

            completedLapCount++;

            if (flap == null || lap.getTime() < flap.getTime()) {
                flap = lap;
            }

            TimedLap completedLap = currentLap;

            if (completedLapCount < totalLaps && raceState != RaceState.CHEQUERED_FLAG) {
                currentLap = new TimedLap(
                        track,
                        flap,
                        split
                );
            } else {
                finished = true;
            }

            return completedLap;
        }

        public long deltaTo(RaceParticipant other) {
            int max_checkpoint = Math.min(getRaceSplits().size(), other.getRaceSplits().size()) - 1;

            if (max_checkpoint < 0) return 0;

            Split local = getRaceSplits().get(max_checkpoint);
            Split foreign = other.getRaceSplits().get(max_checkpoint);

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

        public List<Split> getRaceSplits() {
            return raceSplits;
        }

        public int getGlobalCheckpointIndex() { return globalCheckpointIndex; }

        public List<TimedLapResultSource> getCompletedLaps() { return completedLaps; }

        public int getCompletedLapCount() { return completedLapCount; }

        public int getCompletedPitCount() { return completedPitCount; }

        public boolean isFinished() { return finished; }
    }
}