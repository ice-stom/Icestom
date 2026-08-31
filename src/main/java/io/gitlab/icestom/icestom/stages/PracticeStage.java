package io.gitlab.icestom.icestom.stages;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.EventParticipant;
import io.gitlab.icestom.icestom.event.EventStage;
import io.gitlab.icestom.icestom.event.InvalidStageArgumentsException;
import io.gitlab.icestom.icestom.event.Result;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.track.Track;
import net.minestom.server.MinecraftServer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PracticeStage extends TimeTrialingInstance implements EventStage {

    private CompletableFuture<Void> shim = new CompletableFuture<>();

    public PracticeStage(Track track) {
        super(track);
    }

    public static CompletableFuture<PracticeStage> create(Map<String, Object> options) {
        if (!(options.get("track") instanceof String track_id)) {
            return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'track' isn't a string"));
        }

        return IceStom.getInstance().getTrackLibrary()
                .loadTrack(track_id)
                .thenApply(Optional::get)
                .thenApply(PracticeStage::new);
    }

    @Override
    public CompletableFuture<List<Result<EventParticipant>>> begin(List<Result<EventParticipant>> results) {
        MinecraftServer.getInstanceManager()
                .registerInstance(this);

        teleportAllParticipants(results);

        return CompletableFuture.supplyAsync(() -> {
            shim.join();

            return results;
        });
    }

    @Override
    public void cleanup() {
        MinecraftServer.getInstanceManager()
                .unregisterInstance(this);
    }
}