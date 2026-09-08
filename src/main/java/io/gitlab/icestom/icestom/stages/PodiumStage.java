package io.gitlab.icestom.icestom.stages;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.EventParticipant;
import io.gitlab.icestom.icestom.event.EventStage;
import io.gitlab.icestom.icestom.event.InvalidStageArgumentsException;
import io.gitlab.icestom.icestom.event.Result;
import io.gitlab.icestom.icestom.event.lua.ParticipantStore;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.track.TickMovement;
import io.gitlab.icestom.icestom.track.Track;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class PodiumStage extends TrackInstance implements EventStage {

    private final CompletableFuture<List<Result<EventParticipant>>> future = new CompletableFuture<>();

    private final ParticipantStore participantStore = new ParticipantStore();

    private final String stageName;

    public PodiumStage(String stageName, Track track) {
        super(track);
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
    }

    @Override
    protected void onPlayerMovements(Map<Player, TickMovement> movements, Map<Player, Set<String>> inside_tags, Map<Player, Map<String, Long>> crossed_triggers) {

    }

    @Override
    protected boolean shouldTrackPlayer(Player player) {
        return false;
    }

    @Override
    public Pos spawnLocation(Player player) {
        EventParticipant participant = participantStore.getParticipantFromPlayer(player);
        int position = participantStore.getIndexofParticipant(participant);

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

        return IceStom.getInstance().getTrackLibrary()
                .loadTrack(track_id)
                .thenApply(Optional::get)
                .thenApply(track1 -> new PodiumStage(name, track1));
    }
}
