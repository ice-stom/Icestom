package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.stage.EventStage;
import io.gitlab.icestom.icestom.event.stage.InvalidStageArgumentsException;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public interface TrackEventStage extends EventStage {
    Logger log = LoggerFactory.getLogger(TrackEventStage.class);

    Track getTrack();

    static <S extends TrackEventStage> CompletableFuture<S> withTrackTicket(String track_id, BiFunction<TrackLibrary.Ticket, Track, S> fn) {
        return withTrackTicket(track_id, fn, false);
    }

    static <S extends TrackEventStage> CompletableFuture<S> withTrackTicket(String track_id, BiFunction<TrackLibrary.Ticket, Track, S> fn, boolean allowExclusive) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<TrackLibrary.Ticket> optionalTicket = IceStom.getInstance()
                    .getTrackLibrary()
                    .loadTrack(track_id).join();

            if (optionalTicket.isEmpty()) {
                throw new InvalidStageArgumentsException(
                        "Track '" + track_id + "' doesn't exist"
                );
            }

            TrackLibrary.Ticket ticket = optionalTicket.get();
            Track track = ticket.getTrackMeta().join();

            if (allowExclusive) {
                String request_exclusive = track.getTags().get("icestom.request_exclusive");

                if (request_exclusive != null && request_exclusive.equals("true")) {
                    ticket.burn();

                    Optional<TrackLibrary.Ticket> optionalExclusiveTicket = IceStom.getInstance()
                            .getTrackLibrary()
                            .loadTrackExclusive(track_id, UUID.randomUUID()).join();

                    if (optionalExclusiveTicket.isEmpty()) {
                        throw new InvalidStageArgumentsException(
                                "Track '" + track_id + "' doesn't exist exclusively"
                        );
                    }

                    TrackLibrary.Ticket exclusiveTicket = optionalExclusiveTicket.get();
                    Track exclusiveTrack = exclusiveTicket.getTrackMeta().join();

                    exclusiveTicket.promote();
                    exclusiveTicket.readyForTeleport().join();

                    try {
                        return fn.apply(exclusiveTicket, exclusiveTrack);
                    } catch (Throwable e) {
                        log.error("Failed to apply ticket to stage", e);
                        exclusiveTicket.burn();

                        throw e;
                    }
                }
            }

            ticket.promote();

            ticket.readyForTeleport().join();

            try {
                return fn.apply(ticket, track);
            } catch (Throwable e) {
                log.error("Failed to apply ticket to stage", e);
                ticket.burn();

                throw e;
            }
        });
    };
}
