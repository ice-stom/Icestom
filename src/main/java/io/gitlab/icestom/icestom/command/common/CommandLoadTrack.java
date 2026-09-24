package io.gitlab.icestom.icestom.command.common;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CommandLoadTrack {

    private static final Logger log = LoggerFactory.getLogger(CommandLoadTrack.class);
    private static final TrackLibrary trackLibrary = IceStom.getInstance().getTrackLibrary();

    public static void loadTrack(CommandSender commandSender, String track_id, Consumer<TrackLibrary.Ticket> consumer) {
        @NotNull Optional<TrackLibrary.Ticket> optional = trackLibrary.loadTrack(track_id);

        if (optional.isEmpty()) {
            commandSender.sendMessage(Component.translatable("command.generic.unknown_track", Component.text(track_id)));
            return;
        }

        TrackLibrary.Ticket ticket = optional.get();

        if (ticket.getState() == TrackLibrary.Ticket.State.LOADING) {
            commandSender.sendMessage(Component.translatable("command.generic.loading_track", Component.text(track_id)));
        }
        CompletableFuture.runAsync(() -> {
            try {
                ticket.getTrack();
            } catch (Exception e) {
                commandSender.sendMessage(Component.translatable("command.generic.failed_to_load_track", Component.text(track_id)));
                log.warn("Failed to load track {}", track_id, e);
                return;
            }

            try {
                consumer.accept(ticket);
            } catch (Exception e) {
                log.error("Failed to start consumer for track {}", track_id, e);
            }
        });
    }
}
