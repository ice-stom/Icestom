package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.stage.EventStage;
import io.gitlab.icestom.icestom.event.stage.InvalidStageArgumentsException;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public interface TrackEventStage extends EventStage {
    Track getTrack();

    static <S extends TrackEventStage> CompletableFuture<S> withTrackTicket(String track_id, BiFunction<TrackLibrary.Ticket, Track, S> fn) {
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

            return fn.apply(ticket, track);
        });
    };
}
