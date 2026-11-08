package io.gitlab.icestom.icestom.stages.qualifying;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.ParticipantStoreHolder;
import io.gitlab.icestom.icestom.event.Stateful;
import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.event.event.Result;
import io.gitlab.icestom.icestom.event.lua.ParticipantStore;
import io.gitlab.icestom.icestom.event.stage.InvalidStageArgumentsException;
import io.gitlab.icestom.icestom.ordering.ForwardOrdering;
import io.gitlab.icestom.icestom.ordering.TowerHolder;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.TrackEventStage;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import io.gitlab.icestom.icestom.ui.TickCountdown;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class QualifyingStage extends TimeTrialingInstance implements TrackEventStage, TowerHolder<QualifyingStage.Qualifier>, ParticipantStoreHolder, Stateful<QualifyingStage.QualifyingState> {

    private final CompletableFuture<List<Result<EventParticipant>>> future = new CompletableFuture<>();
    private final String stageName;
    private final Rules rules;

    private final ParticipantStore participants = new ParticipantStore();
    private final Map<UUID, Qualifier> qualifiers = new HashMap<>();

    private final QualifyingTimingTower tower = new QualifyingTimingTower();

    private final TickCountdown sessionTimer = new TickCountdown();

    private QualifyingState state = QualifyingState.WAITING;

    public record Rules(int durationTicks, int lapLimit, LapRanking ranking, ResetMode resetMode) {}

    protected QualifyingStage(String stageName, TrackLibrary.Ticket ticket, Track track, Rules rules) {
        super(ticket, track);

        this.stageName = stageName;
        this.rules = rules;
    }

    public static CompletableFuture<QualifyingStage> create(Map<String, Object> options) {
        try {
            String name = string(options, "name");
            String trackId = string(options, "track");

            Rules rules = new Rules(
                    (int) Math.round(number(options, "duration") * 60 * 20),
                    (int) Math.floor(number(options, "laps")),
                    LapRanking.of(string(options, "ranking"), (int) Math.floor(number(options, "ranking_laps"))),
                    ResetMode.of(string(options, "reset"))
            );

            validate(rules);

            return TrackEventStage.withTrackTicket(trackId, (ticket, track) -> new QualifyingStage(name, ticket, track, rules));
        } catch (InvalidStageArgumentsException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static void validate(Rules rules) {
        if (rules.durationTicks() < 0) throw new InvalidStageArgumentsException("'duration' is < 0");
        if (rules.lapLimit() < 0) throw new InvalidStageArgumentsException("'laps' is < 0");

        if (rules.durationTicks() == 0 && rules.lapLimit() == 0) {
            throw new InvalidStageArgumentsException("'duration' and 'laps' can't both be 0");
        }

        if (rules.lapLimit() > 0 && rules.ranking().rank(Collections.nCopies(rules.lapLimit(), 0L)) == null) {
            throw new InvalidStageArgumentsException("'ranking_laps' needs more laps than 'laps' allows");
        }
    }

    private static String string(Map<String, Object> options, String key) {
        if (!(options.get(key) instanceof String value)) {
            throw new InvalidStageArgumentsException("'" + key + "' isn't a string");
        }

        return value;
    }

    private static double number(Map<String, Object> options, String key) {
        if (!(options.get(key) instanceof Number value)) {
            throw new InvalidStageArgumentsException("'" + key + "' isn't a number");
        }

        return value.doubleValue();
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (sessionTimer.tick()) showChequeredFlag();
        if (state == QualifyingState.CHEQUERED_FLAG && nobodyOnTimedLap()) endQualifying();
    }

    private boolean nobodyOnTimedLap() {
        return participants.getActivelyParticipatingPlayers().stream()
                .allMatch(player -> getTimedLap(player.getUuid()) == null);
    }

    private void startQualifying() {
        if (state != QualifyingState.WAITING) return;
        state = QualifyingState.RUNNING;

        if (rules.durationTicks() > 0) sessionTimer.start(rules.durationTicks());
    }

    private void showChequeredFlag() {
        if (state != QualifyingState.RUNNING) return;
        state = QualifyingState.CHEQUERED_FLAG;
    }

    private void endQualifying() {
        if (state != QualifyingState.CHEQUERED_FLAG) return;
        state = QualifyingState.END;

        List<Result<EventParticipant>> results = new ArrayList<>();

        for (ForwardOrdering<Qualifier>.Entry entry : tower) {
            results.add(entry.getItem().toResult());
        }

        future.complete(results);
    }

    @Override
    protected void completeLap(UUID player, TimedLap lap, Runnable startNextLap) {
        @Nullable Qualifier qualifier = getQualifier(player);

        if (qualifier != null) recordLap(qualifier, lap.getTime());

        if (qualifier != null && keepsLapping(qualifier)) {
            super.completeLap(player, lap, startNextLap);
            return;
        }

        endTimeTrial(player);

        if (rules.resetMode().returnsAfterLap()) returnToStart(player);
    }

    private boolean keepsLapping(Qualifier qualifier) {
        return state == QualifyingState.RUNNING && !qualifier.isDone() && !rules.resetMode().returnsAfterLap();
    }

    private void returnToStart(UUID uuid) {
        scheduleNextTick(_ -> {
            @Nullable Player player = getPlayerByUuid(uuid);

            if (player != null) resetPlayer(player);
        });
    }

    private void recordLap(Qualifier qualifier, long time) {
        qualifier.addLap(time);
        tower.update(qualifier.getTowerEntry());

        if (rules.lapLimit() > 0 && qualifiers.values().stream().allMatch(Qualifier::isDone)) showChequeredFlag();
    }

    private @Nullable Qualifier getQualifier(UUID player) {
        @Nullable EventParticipant participant = participants.getParticipantFromPlayer(player);

        return participant == null ? null : qualifiers.get(participant.getUuid());
    }

    @Override
    protected boolean shouldTrackPlayer(Player player) {
        if (!super.shouldTrackPlayer(player) || !participants.isPlayerActivelyParticipating(player)) return false;

        @Nullable Qualifier qualifier = getQualifier(player.getUuid());
        if (qualifier == null) return false;

        boolean canStartLaps = state == QualifyingState.RUNNING && !qualifier.isDone();
        boolean finishingLap = state == QualifyingState.CHEQUERED_FLAG && getTimedLap(player.getUuid()) != null;

        return canStartLaps || finishingLap;
    }

    @Override
    public boolean allowsManualReset() {
        return rules.resetMode().allowsManualReset();
    }

    @Override
    public String getStageName() {
        return stageName;
    }

    @Override
    public CompletableFuture<List<Result<EventParticipant>>> begin(List<Result<EventParticipant>> results) {
        MinecraftServer.getInstanceManager()
                .registerSharedInstance(this);

        initialize();

        for (Result<EventParticipant> result : results) {
            addQualifier(result.getParticipant());
        }

        teleportAllParticipants(results);

        return future;
    }

    private void addQualifier(EventParticipant participant) {
        participants.addParticipant(participant);

        Qualifier qualifier = new Qualifier(participant);
        qualifiers.put(participant.getUuid(), qualifier);
        qualifier.towerEntry = tower.add(qualifier);
    }

    @Override
    public void cleanup() {
        MinecraftServer.getInstanceManager()
                .unregisterInstance(this);

        getTicket().burn();
    }

    @Override
    public QualifyingTimingTower getTower() {
        return tower;
    }

    @Override
    public ParticipantStore getParticipants() {
        return participants;
    }

    public Rules getRules() {
        return rules;
    }

    public TickCountdown getSessionTimer() {
        return sessionTimer;
    }

    @Override
    public QualifyingState getState() {
        return state;
    }

    @Override
    public List<StateChange<QualifyingState>> getStageChanges() {
        return List.of(
                new StateChange<>("StartQualifying", QualifyingState.WAITING, QualifyingState.RUNNING, this::startQualifying),
                new StateChange<>("ShowChequeredFlag", QualifyingState.RUNNING, QualifyingState.CHEQUERED_FLAG, this::showChequeredFlag),
                new StateChange<>("EndQualifying", QualifyingState.CHEQUERED_FLAG, QualifyingState.END, this::endQualifying)
        );
    }

    public enum QualifyingState {
        WAITING,
        RUNNING,
        CHEQUERED_FLAG,
        END
    }

    public class Qualifier implements ForwardOrdering.Orderable<Qualifier> {
        private final EventParticipant participant;
        private final List<Long> lapTimes = new ArrayList<>();

        private @Nullable Long qualifyingTime;

        private ForwardOrdering<Qualifier>.Entry towerEntry;

        Qualifier(EventParticipant participant) {
            this.participant = participant;
        }

        void addLap(long time) {
            lapTimes.add(time);

            @Nullable Long ranked = rules.ranking().rank(lapTimes);

            // the tower only ever moves entries up, so a time must never get worse
            if (LapRanking.isFaster(ranked, qualifyingTime)) qualifyingTime = ranked;
        }

        boolean isDone() {
            return rules.lapLimit() > 0 && lapTimes.size() >= rules.lapLimit();
        }

        Result<EventParticipant> toResult() {
            Result<EventParticipant> result = new Result<>(participant);

            result.set(Key.key(IceStom.NAMESPACE, "completed_laps"), lapTimes.size());
            if (qualifyingTime != null) result.set(Key.key(IceStom.NAMESPACE, "qualifying_time"), qualifyingTime);

            return result;
        }

        @Override
        public boolean betterThan(Qualifier other) {
            return LapRanking.isFaster(qualifyingTime, other.qualifyingTime);
        }

        public ForwardOrdering<Qualifier>.Entry getTowerEntry() {
            return towerEntry;
        }

        public @Nullable Long getQualifyingTime() {
            return qualifyingTime;
        }

        public Component getName() {
            return participant.getName();
        }

        public List<Long> getLapTimes() {
            return Collections.unmodifiableList(lapTimes);
        }
    }
}
