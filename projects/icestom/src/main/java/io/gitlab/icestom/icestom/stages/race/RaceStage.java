package io.gitlab.icestom.icestom.stages.race;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.GridBoatHolder;
import io.gitlab.icestom.icestom.event.*;
import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.event.event.Result;
import io.gitlab.icestom.icestom.event.lua.ParticipantStore;
import io.gitlab.icestom.icestom.event.stage.InvalidStageArgumentsException;
import io.gitlab.icestom.icestom.instance.BoatedTrackInstance;
import io.gitlab.icestom.icestom.ordering.ForwardOrdering;
import io.gitlab.icestom.icestom.ordering.TowerHolder;
import io.gitlab.icestom.icestom.stages.race.event.RaceLapTimerEvent;
import io.gitlab.icestom.icestom.stages.race.event.RaceLeaderboardUpdateEvent;
import io.gitlab.icestom.icestom.timetrial.Split;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import io.gitlab.icestom.icestom.track.PlayerMovement;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.TrackEventStage;
import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import io.gitlab.icestom.icestom.ui.TickCountdown;
import io.gitlab.icestom.icestom.ui.event.GenericErrorMessageEvent;
import io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager.getHolder;

public class RaceStage extends BoatedTrackInstance implements TrackEventStage, TowerHolder<RaceStage.RaceParticipant>, ParticipantStoreHolder, Stateful<RaceStage.RaceState> {

    private final InterfaceManager.InterfaceHolder interfaceHolder;

    private final ParticipantStore participants = new ParticipantStore();
    private final List<EventParticipant> startOrder = new ArrayList<>();

    private final Map<UUID, RaceParticipant> racers = new HashMap<>();
    private final Map<RaceParticipant, UUID> racersLookup = new HashMap<>();

    private final RaceTimingTower tower = new RaceTimingTower();
    private final Map<RaceParticipant, ForwardOrdering<RaceParticipant>.Entry> towerEntryCache = new HashMap<>();

    private final int totalLaps;
    private final int totalPits;

    private final TickCountdown startingCountdown = new TickCountdown();
    private final TickCountdown chequeredFlag = new TickCountdown();

    private RaceState raceState = RaceState.GRID;

    private final CompletableFuture<List<Result<EventParticipant>>> future = new CompletableFuture<>();

    private final String name;

    private final Map<UUID, TickLocation> lastPosition = new HashMap<>();

    protected RaceStage(String stageName, TrackLibrary.Ticket ticket, Track track, int totalLaps, int totalPits) {
        super(ticket, track);

        this.name = stageName;

        this.totalLaps = totalLaps;
        this.totalPits = totalPits;

        subscribeRegionId("icestom.reset");
        subscribeTriggerId("icestom.reset");

        eventNode().addListener(PlayerStartSneakingEvent.class, event -> {
            Player player = event.getPlayer();

            if (!(player.getVehicle() instanceof Boat)) return;

            @Nullable EventParticipant participation = participants.getParticipantFromActivePlayer(player);

            if (participation != null) {
                RaceParticipant raceParticipant = racers.get(participation.getUuid());

                if (raceParticipant.isFinished()) {
                    removeBoat(player);
                }
            }
        });

        interfaceHolder = getHolder(RaceStage.class, this);
    }

    public static CompletableFuture<RaceStage> create(Map<String, Object> options) {
        if (!(options.get("name") instanceof String name)) {
            return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'name' isn't a string"));
        }

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

        return TrackEventStage.withTrackTicket(track_id, (ticket, track) -> new RaceStage(name, ticket, track, laps, pits));
    }

    @Override
    protected void handleMovements(List<TickLocation> movements) {
        if (movements.isEmpty()) return;

        Map<Integer, LinkedHashMap<UUID, ArrayDeque<TickLocation>>> pending = new HashMap<>();
        ArrayDeque<Integer> checkpointQueue = new ArrayDeque<>();

        Map<UUID, RaceParticipant> playerRacers = new HashMap<>();

        for (TickLocation movement : movements) {
            UUID player = movement.player();

            EventParticipant participant = getParticipants().getParticipantFromPlayer(player);

            if (participant == null) continue;
            if (!participant.getCurrentPlayer().getUuid().equals(player)) continue;

            RaceParticipant racer = getRacer(participant.getUuid());

            playerRacers.put(player, racer);

            int next_no = 0;
            @Nullable TimedLap lap = racer.getCurrentLap();
            if (lap != null) next_no = track.wrapCheckpointIndex(lap.getLastReachedCheckpoint() + 1);

            pending.computeIfAbsent(next_no, _ -> new LinkedHashMap<>())
                    .computeIfAbsent(player, _ -> new ArrayDeque<>())
                    .add(movement);

            checkpointQueue.add(next_no);
        }

        Integer checkpoint;
        while ((checkpoint = checkpointQueue.poll()) != null) {
            LinkedHashMap<UUID, ArrayDeque<TickLocation>> bucket = pending.get(checkpoint);
            if (bucket == null || bucket.isEmpty()) continue;

            LinkedHashMap<UUID, TickLocation> candidates = new LinkedHashMap<>();
            for (Map.Entry<UUID, ArrayDeque<TickLocation>> e : bucket.entrySet()) {
                TickLocation head = e.getValue().peek();
                if (head != null) candidates.put(e.getKey(), head);
            }

            if (candidates.isEmpty()) {
                pending.remove(checkpoint);
                continue;
            }

            List<PlayerMovement> moves = new ArrayList<>();

            List<TickLocation> now_locations = new ArrayList<>(candidates.values());
            List<TickLocation> old_locations = new ArrayList<>(now_locations.size());
            for (TickLocation now : now_locations) {
                @Nullable TickLocation old = lastPosition.getOrDefault(now.player(), null);

                old_locations.add(old);

                moves.add(new PlayerMovement(now.tick(), old, now));
            }

            dispatchTrackEvents(moves);

            Map<TickLocation, Long> hits = new HashMap<>();
            for (CrossCollider trackCheckpoint : track.getCheckpoints(checkpoint)) {
                for (Map.Entry<TickLocation, Long> hit : trackCheckpoint.detectCrosses(old_locations, now_locations).entrySet()) {
                    hits.putIfAbsent(hit.getKey(), hit.getValue());
                }
            }

            for (Map.Entry<UUID, TickLocation> entry : candidates.entrySet()) {
                UUID player = entry.getKey();
                TickLocation location = entry.getValue();

                ArrayDeque<TickLocation> playerQueue = bucket.get(player);
                playerQueue.poll();

                lastPosition.put(player, location);

                Long tick_delta = hits.get(location);

                if (tick_delta == null) {
                    if (playerQueue.isEmpty()) bucket.remove(player);
                    continue;
                }

                RaceParticipant racer = playerRacers.get(player);

                bucket.remove(player);

                racer.nextCheckpoint(new Split(
                        location.tick() * 50,
                        tick_delta,
                        checkpoint
                ));

                tower.update(racer.getTowerEntry());

//                MinecraftServer.getGlobalEventHandler()
//                        .call(new TimeTrialCheckpointAdvancedEvent(this, lap, Objects.requireNonNull(getPlayerByUuid(player))));

                int next_checkpoint = track.wrapCheckpointIndex(checkpoint + 1);

                if (!playerQueue.isEmpty()) {
                    LinkedHashMap<UUID, ArrayDeque<TickLocation>> nextBucket =
                            pending.computeIfAbsent(next_checkpoint, _ -> new LinkedHashMap<>());
                    nextBucket.put(player, playerQueue);
                    checkpointQueue.add(next_checkpoint);
                }
            }

            if (bucket.isEmpty()) {
                pending.remove(checkpoint);
            } else {
                checkpointQueue.add(checkpoint);
            }
        }

        MinecraftServer.getGlobalEventHandler()
                .call(new RaceLeaderboardUpdateEvent(this));
    }
//
//    @Override
//    protected void handleMovements(Map<Player, PlayerMovement> movements, Map<Player, Set<String>> inside_regions, Map<Player, Map<String, Long>> crossed_triggers) {
//        Map<Integer, Map<Player, PlayerMovement>> grouped = new HashMap<>();
//
//        for (Map.Entry<Player, PlayerMovement> entry : movements.entrySet()) {
//            Player player = entry.getKey();
//            PlayerMovement movement = entry.update();
//
//            Set<String> playerRegions = inside_regions.get(player);
//            Map<String, Long> playerTriggers = crossed_triggers.get(player);
//
//            @Nullable EventParticipant participation = participants.getParticipantFromActivePlayer(player);
//
//            if (participation != null) {
//                RaceParticipant raceParticipant = racers.get(participation.getUuid());
//
//                if (raceParticipant == null || raceParticipant.isFinished()) continue;
//
//                grouped.computeIfAbsent(raceParticipant.getNextExpected(), _ -> new HashMap<>())
//                        .put(player, movement);
//
//                TimedLap timedLap = raceParticipant.getCurrentLap();
//
//                TrackInstance.tickResetRegions(this, player, playerRegions, playerTriggers, timedLap);
//            }
//        }
//
//        AtomicBoolean updated = new AtomicBoolean(false);
//
//        for (Map.Entry<Integer, Map<Player, PlayerMovement>> integerMapEntry : grouped.entrySet()) {
//            int checkpoint_index = integerMapEntry.getKey();
//
//            for (CrossCollider checkpoint : track.getCheckpoints(checkpoint_index)) {
//                Map<Player, Long> crosses = checkpoint.detectCrosses(integerMapEntry.update());
//
//                crosses.forEach((player, tick_delta) -> {
//                    @Nullable EventParticipant participation = participants.getParticipantFromActivePlayer(player);
//
//                    if (participation != null) {
//                        RaceParticipant raceParticipant = racers.get(participation.getUuid());
//
//                        if (raceParticipant == null || raceParticipant.isFinished()) return;
//
//                        Split split = new Split(
//                                getWorldAge() * 50,
//                                tick_delta,
//                                checkpoint_index
//                        );
//
//                        TimedLap last = raceParticipant.getCurrentLap();
//
//                        @Nullable TimedLap finished = raceParticipant.nextCheckpoint(split);
//
//                        MinecraftServer.getGlobalEventHandler()
//                                .call(new TimedLapCheckpointAdvancedEvent(last, player));
//
//                        if (finished != null) {
//                            MinecraftServer.getGlobalEventHandler()
//                                    .call(new RaceTimedLapCompletedEvent(participation, finished, this));
//
//                            if (raceParticipant.isFinished()) {
//                                MinecraftServer.getGlobalEventHandler()
//                                        .call(new RaceCompletedEvent(participation, this));
//
//                                if (raceState == RaceState.RACE) finishRace();
//                            }
//                        }
//
//                        raceLeaderboard.update(raceParticipant, split);
//                        updated.set(true);
//                    }
//                });
//            }
//        }
//
//        if (updated.get()) {
//            MinecraftServer.getGlobalEventHandler()
//                    .call(new RaceLeaderboardUpdateEvent(this));
//        }
//    }

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

        if (startingCountdown.tick()) startRace();
        if (chequeredFlag.tick()) endRace();
    }

    private void startCountdown() {
        if (raceState != RaceState.GRID) return;
        raceState = RaceState.COUNTDOWN;

        // trust
        startingCountdown.start(10 * 20);
    }

    private void startRace() {
        if (raceState != RaceState.COUNTDOWN) return;
        raceState = RaceState.RACE;

        for (Player player : participants.getActivelyParticipatingPlayers()) {
            if (player.getVehicle() instanceof Boat boat && boat.getVehicle() instanceof GridBoatHolder holder) {
                holder.remove();
            }
        }
    }

    private void finishRace() {
        if (raceState != RaceState.RACE) return;
        raceState = RaceState.CHEQUERED_FLAG;

        chequeredFlag.start(60 * 20);
    }

    private void endRace() {
        if (raceState != RaceState.CHEQUERED_FLAG) return;
        raceState = RaceState.END;

        List<Result<EventParticipant>> results = new ArrayList<>();

        for (ForwardOrdering<RaceParticipant>.Entry entry : tower) {
            RaceParticipant racer = entry.getItem();

            EventParticipant participant = participants.getParticipantFromId(getParticipantId(
                    racer
            ));

            Result<EventParticipant> result = new Result<>(participant);

            // TODO: figure out how i'm gonna put tables n shit in here for splits / lap times

            result.set(Key.key(IceStom.NAMESPACE, "completed_laps"), racer.getCompletedLaps());
            result.set(Key.key(IceStom.NAMESPACE, "completed_pits"), racer.getCompletedPitCount());

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
    public String getStageName() {
        return name;
    }

    @Override
    public CompletableFuture<List<Result<EventParticipant>>> begin(List<Result<EventParticipant>> results) {

        for (Result<EventParticipant> result : results) {
            EventParticipant participant = result.getParticipant();

            participants.addParticipant(participant);

            // TODO: UNFUCK, shouldn't really hardcode the name
            RaceParticipant racer = racers.computeIfAbsent(participant.getUuid(), _ -> new RaceParticipant(participant.getCurrentPlayer().getName()));
            racersLookup.put(racer, participant.getUuid());

            startOrder.add(participant);
            towerEntryCache.put(racer, tower.add(racer));
        }

        MinecraftServer.getInstanceManager()
                .registerSharedInstance(this);

        TrackEventStage.super.teleportAllParticipants(results);

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

        createBoat(player, grid_location).thenAccept(boat -> {
            GridBoatHolder holder = new GridBoatHolder(this, grid_location);

            holder.addPassenger(boat);
        });

        return true;
    }

    public RaceTimingTower getTower() {
        return tower;
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
        if (raceState == RaceState.GRID) {
            EventParticipant participant = participants.getParticipantFromActivePlayer(player);
            Pos grid_location = getGridLocation(participant);

            if (grid_location != null) {
                return grid_location;
            }
        }

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
        super.consume(player);
        interfaceHolder.startWatching(player);
    }

    @Override
    public void drop(Player player) {
        super.drop(player);
        interfaceHolder.stopWatching(player);
    }

    @Override
    public void cleanup() {
        MinecraftServer.getInstanceManager()
                .unregisterInstance(this);

        getTicket().burn();
    }

    @Override
    public RaceState getState() {
        return raceState;
    }

    @Override
    public List<StateChange<RaceState>> getStageChanges() {
        return List.of(
                new StateChange<>("StartCountdown", RaceState.GRID, RaceState.COUNTDOWN, this::startCountdown),
                new StateChange<>("FinishRace", RaceState.RACE, RaceState.CHEQUERED_FLAG, this::finishRace)
        );
    }

    public List<EventParticipant> getStartOrder() {
        return startOrder;
    }

    public TickCountdown getStartingCountdown() {
        return startingCountdown;
    }

    public TickCountdown getChequeredFlagCountdown() {
        return chequeredFlag;
    }

    public enum RaceState {
        GRID,
        COUNTDOWN,
        RACE,
        CHEQUERED_FLAG,
        END
    }

    public class RaceParticipant implements ForwardOrdering.Orderable<RaceParticipant> {
        private final List<Split> raceSplits = new ArrayList<>();
        private final List<TimedLapResultSource> completedLaps = new ArrayList<>();

        private TimedLapResultSource flap = null;

        private TimedLap currentLap;
        private int globalCheckpointIndex = -1;
        private int nextExpected = 0;

        private int completedLapCount = 0;
        private int completedPitCount = 0;

        private boolean finished = false;

        private Component name;

        private long deltaToAhead = 0;

        RaceParticipant(Component name) {
            this.name = name;
            currentLap = new TimedLap(
                    track,
                    null
            );
        }

        public ForwardOrdering<RaceParticipant>.Entry getTowerEntry() {
            return towerEntryCache.get(this);
        };

        public Component getName() {
            return name;
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

        public long getDeltaToAhead() {
            return deltaToAhead;
        }

        @Override
        public boolean betterThan(RaceParticipant other) {

            if (raceState == RaceState.GRID || raceState == RaceState.COUNTDOWN) {
                int this_start = startOrder.indexOf(participants.getParticipantFromId(getParticipantId(this)));
                int other_start = startOrder.indexOf(participants.getParticipantFromId(getParticipantId(other)));

                if (this_start == -1) return false;

                return this_start < other_start;
            }

            int mine = getRaceSplits().size();
            int theirs = other.getRaceSplits().size();

            int shared = Math.min(mine, theirs) - 1;
            long delta = shared < 0 ? 0 : getRaceSplits().get(shared).ms() - other.getRaceSplits().get(shared).ms();

            if (mine > theirs) {
                deltaToAhead = 0;
                return true;
            };

            deltaToAhead = delta;

/*            if (mine > theirs) return Math.min(delta, -1);
            if (mine < theirs) return Math.max(delta, 0);*/

            return delta <= 0;
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