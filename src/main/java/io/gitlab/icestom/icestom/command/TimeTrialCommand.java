package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.command.common.CommandLoadTrack;
import io.gitlab.icestom.icestom.timetrial.TimeTrialManager;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class TimeTrialCommand extends Command {

    private static final Logger log = LoggerFactory.getLogger(TimeTrialCommand.class);

    private final TimeTrialManager timeTrialManager = IceStom.getInstance().getTimeTrialManager();
    private final TrackLibrary trackLibrary = IceStom.getInstance().getTrackLibrary();


    public TimeTrialCommand() {
        super("timetrial", "tt");

        var trackArgument = ArgumentType.String("track")
                .setSuggestionCallback(this::suggestionCallback);

        addSyntax((commandSender, commandContext) -> {
            if (!(commandSender instanceof Player player)) {
                commandSender.sendMessage(Component.translatable("command.generic.must_be_player"));
                return;
            }

            final String track_id = commandContext.get(trackArgument);

            if (player.getInstance() instanceof TimeTrialingInstance timeTrialingInstance) {
                if (timeTrialingInstance.getTrack().getId().equals(track_id)) {
                    timeTrialingInstance.resetPlayer(player);
                    return;
                }
            }

            CommandLoadTrack.loadTrack(commandSender, track_id, track -> {
                timeTrialManager.startTimeTrialing(player, track);
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
