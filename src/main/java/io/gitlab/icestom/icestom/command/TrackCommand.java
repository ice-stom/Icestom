package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.command.common.CommandLoadTrack;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TrackCommand extends Command {

    private static final Logger log = LoggerFactory.getLogger(TrackCommand.class);
    private final TrackLibrary trackLibrary = IceStom.getInstance().getTrackLibrary();

    public TrackCommand() {
        super("track", "t");

        addSubcommand(new TrackInfoCommand());
    }

    public class TrackInfoCommand extends Command {
        public TrackInfoCommand() {
            super("info");
            var trackArgument = ArgumentType.String("track")
                    .setSuggestionCallback(this::suggestionCallback);

            addSyntax((commandSender, commandContext) -> {
                final String track_id = commandContext.get(trackArgument);

                CommandLoadTrack.loadTrack(commandSender, track_id, track -> {
                    final Component[] text = {Component.text("Track " + track_id + "\n")
                            .append(Component.text(" - Checkpoints:\n"))};

                    track.getCheckpoints()
                            .entrySet()
                            .stream()
                            .sorted(Comparator.comparingInt(Map.Entry::getValue))
                            .forEach(entry -> {
                                text[0] = text[0].append(Component.text("  " + entry.getValue() + ": " + entry.getKey().getClass().getSimpleName() + "\n"));
                            });

                    commandSender.sendMessage(text[0]);
                });
            }, trackArgument);
        }

        private void suggestionCallback(CommandSender commandSender, CommandContext commandContext, Suggestion suggestion) {
            String input = Arrays.stream(commandContext.getInput().split(" ")).toList().getLast().toLowerCase();

            trackLibrary.getAvailableTracks()
                    .stream()
                    .filter(track -> input.isBlank() || input.charAt(0) == 0 || track.toLowerCase().startsWith(input))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .limit(20)
                    .forEach(track ->
                            suggestion.addEntry(
                                    new SuggestionEntry(
                                            track,
                                            Component.text("Hello World")
                                    )
                            )
                    );
        }
    }
}
