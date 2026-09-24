package io.gitlab.icestom.icestom.stages;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.*;
import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.event.event.Result;
import io.gitlab.icestom.icestom.event.lua.ParticipantStore;
import io.gitlab.icestom.icestom.event.stage.EventStage;
import io.gitlab.icestom.icestom.event.stage.InvalidStageArgumentsException;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PracticeStage extends TimeTrialingInstance implements EventStage, Stateful<PracticeStage.PracticeState>, ParticipantStoreHolder, LateJoinable {

    private final CompletableFuture<List<Result<EventParticipant>>> future = new CompletableFuture<>();
    private final String stageName;

    private final ParticipantStore participantStore = new ParticipantStore();

    private PracticeState state = PracticeState.PRACTICE;

    public PracticeStage(String stageName, TrackLibrary.Ticket ticket, Track track) {
        super(ticket, track);
        this.stageName = stageName;
    }

    public static CompletableFuture<PracticeStage> create(Map<String, Object> options) {
        if (!(options.get("name") instanceof String name)) {
            return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'name' isn't a string"));
        }

        if (!(options.get("track") instanceof String track_id)) {
            return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'track' isn't a string"));
        }

        return CompletableFuture.supplyAsync(() -> {
            Optional<TrackLibrary.Ticket> optionalTicket =
                    IceStom.getInstance()
                            .getTrackLibrary()
                            .loadTrack(track_id);

            if (optionalTicket.isEmpty()) {
                throw new InvalidStageArgumentsException(
                        "Track '" + track_id + "' doesn't exist"
                );
            }

            TrackLibrary.Ticket ticket = optionalTicket.get();

            Track track = ticket.getTrack().join();

            return new PracticeStage(name, ticket, track);
        });
    }

    private void endPractice() {
        if (state != PracticeState.PRACTICE) return;
        state = PracticeState.END;

        List<Result<EventParticipant>> results = new ArrayList<>();

        for (EventParticipant participant : participantStore.getParticipants()) {
            results.add(new Result<>(participant));
        }

        future.complete(results);
    }

    @Override
    public String getStageName() {
        return stageName;
    }

    @Override
    public CompletableFuture<List<Result<EventParticipant>>> begin(List<Result<EventParticipant>> results) {
        MinecraftServer.getInstanceManager()
                .registerSharedInstance(this);

        for (Result<EventParticipant> result : results) {
            participantStore.addParticipant(result.getParticipant());
        }

        teleportAllParticipants(results);

        return future;
    }

    @Override
    public void cleanup() {
        MinecraftServer.getInstanceManager()
                .unregisterInstance(this);

        getTicket().burn();
    }

    @Override
    public PracticeState getState() {
        return state;
    }

    @Override
    public List<StateChange<PracticeState>> getStageChanges() {
        return List.of(
                new StateChange<>("EndPractice", PracticeState.PRACTICE, PracticeState.END, this::endPractice)
        );
    }

    @Override
    public ParticipantStore getParticipants() {
        return participantStore;
    }

    @Override
    public void joinLate(EventParticipant participant) {
        participantStore.addParticipant(participant);

        for (UUID uuid : participant.getPlayers()) {
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);

            consume(player);
        }
    }

    public enum PracticeState {
        PRACTICE,
        END
    }
}