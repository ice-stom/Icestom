package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.translation.Argument;
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

                trackLibrary.loadTrack(track_id).thenAccept(optionalTicket -> {
                    if (optionalTicket.isEmpty()) {
                        commandSender.sendMessage(Component.translatable("<theme:error>Unknown track '<theme:special><arg:0></theme:special>'."));
                        return;
                    }

                    TrackLibrary.Ticket ticket = optionalTicket.get();

                    ticket.getTrackMeta().thenAccept(track -> {
                        ticket.burn();

                        @Nullable String contributors = track.getTags().get("icestom.contributors");

                        String date = track.getTags().get("icestom.date");

                        if (date == null) date = "unknown";

                        if (contributors != null) {
                            commandSender.sendMessage(Component.translatable("command.track.info_contributors",
                                    Argument.component("track", track.getName()),
                                    Argument.string("track_id", track_id),
                                    Argument.string("looped", track.isLooped() ? "looped" : "p2p"),
                                    Argument.string("date", date),
                                    Argument.string("author", track.getTags().getOrDefault("icestom.author", "unknown")),
                                    Argument.string("contributors", contributors),
                                    Argument.string("license", track.getTags().getOrDefault("icestom.license", "unspecified")),
                                    Argument.numeric("checkpoints", track.getHighestCheckpoint()),
                                    Argument.numeric("regions", track.getRegions().size()),
                                    Argument.numeric("triggers", track.getTriggers().size()),
                                    Argument.numeric("grids", track.getGridLocations().size()),
                                    Argument.string("obu", track.getOpenBoatUtilsPackets().isEmpty() ? "no" : "yes"),
                                    Argument.string("packets", track.getOpenBoatUtilsPackets().isEmpty() ? "" : "(" + track.getOpenBoatUtilsPackets().size() + " packets)")
                            ));
                        } else {
                            commandSender.sendMessage(Component.translatable("command.track.info",
                                    Argument.component("track", track.getName()),
                                    Argument.string("track_id", track_id),
                                    Argument.string("looped", track.isLooped() ? "looped" : "p2p"),
                                    Argument.string("date", date),
                                    Argument.string("author", track.getTags().getOrDefault("icestom.author", "unknown")),
                                    Argument.string("license", track.getTags().getOrDefault("icestom.license", "unspecified")),
                                    Argument.numeric("checkpoints", track.getHighestCheckpoint()),
                                    Argument.numeric("regions", track.getRegions().size()),
                                    Argument.numeric("triggers", track.getTriggers().size()),
                                    Argument.numeric("grids", track.getGridLocations().size()),
                                    Argument.string("obu", track.getOpenBoatUtilsPackets().isEmpty() ? "no" : "yes"),
                                    Argument.string("packets", track.getOpenBoatUtilsPackets().isEmpty() ? "" : "(" + track.getOpenBoatUtilsPackets().size() + " packets)")
                            ));
                        }
                    });
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
