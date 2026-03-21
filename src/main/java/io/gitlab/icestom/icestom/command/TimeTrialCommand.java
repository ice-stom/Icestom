package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.timetrial.TimeTrialManager;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.TrackLibrary;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public class TimeTrialCommand extends Command {

    private final TrackLibrary trackLibrary = IceStom.getInstance().getTrackLibrary();
    private final TimeTrialManager timeTrialManager = IceStom.getInstance().getTimeTrialManager();

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

            @Nullable Track track = trackLibrary.getTracks().get(track_id);

            if (track == null) {
                commandSender.sendMessage(Component.translatable("command.timetrial.unknown_track", Component.text(track_id)));
                return;
            }

            if (player.getInstance() instanceof TimeTrialingInstance timeTrialingInstance) {
                if (timeTrialingInstance.getTrack() == track) {
                    timeTrialingInstance.resetPlayer(player);
                    return;
                }
            }

            timeTrialManager.starTimeTrialing(player, track);
        }, trackArgument);
    }

    private void suggestionCallback(CommandSender commandSender, CommandContext commandContext, Suggestion suggestion) {
        for (String id : trackLibrary.getTracks().keySet()) {
            suggestion.addEntry(new SuggestionEntry(id));
        }
    }
}
