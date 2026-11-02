package io.gitlab.icestom.icestom.stages;

import io.gitlab.icestom.icestom.event.Stateful;
import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.event.stage.InvalidStageArgumentsException;
import io.gitlab.icestom.icestom.event.event.Result;
import io.gitlab.icestom.icestom.event.lua.ParticipantStore;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.TrackEventStage;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PodiumStage extends TrackInstance implements TrackEventStage, Stateful<PodiumStage.PodiumState> {

    private final CompletableFuture<List<Result<EventParticipant>>> future = new CompletableFuture<>();

    private final ParticipantStore participantStore = new ParticipantStore();

    private final String stageName;

    private PodiumState state = PodiumState.PODIUM;

    public PodiumStage(String stageName, TrackLibrary.Ticket ticket, Track track) {
        super(ticket, track);
        this.stageName = stageName;
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
    protected void handleMovements(List<TickLocation> movements) {}

    @Override
    protected boolean shouldTrackPlayer(Player player) {
        return false;
    }

    @Override
    public Pos spawnLocation(Player player) {
        EventParticipant participant = participantStore.getParticipantFromPlayer(player.getUuid());
        int position = participantStore.getIndexOfParticipant(participant);

        Pos podiumLocation = track.getLocations().get("icestom.podium_" + position);

        if (podiumLocation != null) {
            return podiumLocation;
        } else {
            return track.getLocations().getOrDefault("icestom.podium_all", track.getSpawnLocation());
        }
    }

    @Override
    public PodiumState getState() {
        return state;
    }

    @Override
    public List<StateChange<PodiumState>> getStageChanges() {
        return List.of(
                new StateChange<>("End", PodiumState.PODIUM, PodiumState.END, this::end)
        );
    }

    private void end() {
        if (state != PodiumState.PODIUM) return;
        state = PodiumState.END;

        List<Result<EventParticipant>> results = new ArrayList<>();

        for (EventParticipant participant : participantStore.getParticipants()) {
            results.add(new Result<>(participant));
        }

        future.complete(results);
    }

    public static CompletableFuture<PodiumStage> create(Map<String, Object> options) {
        if (!(options.get("name") instanceof String name)) {
            return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'name' isn't a string"));
        }

        if (!(options.get("track") instanceof String track_id)) {
            return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'track' isn't a string"));
        }

        return TrackEventStage.withTrackTicket(track_id, (ticket, track) -> new PodiumStage(name, ticket, track));
    }

    public enum PodiumState {
        PODIUM,
        END
    }
}
