package io.gitlab.icestom.icestom.stages;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.event.stage.EventStage;
import io.gitlab.icestom.icestom.event.stage.InvalidStageArgumentsException;
import io.gitlab.icestom.icestom.event.event.Result;
import io.gitlab.icestom.icestom.event.lua.ParticipantStore;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.race.RaceStage;
import io.gitlab.icestom.icestom.track.PlayerMovement;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.TrackEventStage;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class PodiumStage extends TrackInstance implements TrackEventStage {

    private final CompletableFuture<List<Result<EventParticipant>>> future = new CompletableFuture<>();

    private final ParticipantStore participantStore = new ParticipantStore();

    private final String stageName;

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
                .registerInstance(this);

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
        EventParticipant participant = participantStore.getParticipantFromPlayer(player);
        int position = participantStore.getIndexOfParticipant(participant);

        Pos podiumLocation = track.getLocations().get("icestom.podium_" + position);

        if (podiumLocation != null) {
            return podiumLocation;
        } else {
            return track.getLocations().getOrDefault("icestom.podium_all", track.getSpawnLocation());
        }
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
}
