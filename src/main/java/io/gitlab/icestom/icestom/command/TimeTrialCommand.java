package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.command.common.CommandLoadTrack;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.timetrial.TimeTrialManager;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TimeTrialCommand extends Command {

    private static final Logger log = LoggerFactory.getLogger(TimeTrialCommand.class);

    private static final String PERMISSION_BYPASS_FILTER = "icestom.timetrial.bypass_filter";

    private final TimeTrialManager timeTrialManager = IceStom.getInstance().getTimeTrialManager();
    private final TrackLibrary trackLibrary = IceStom.getInstance().getTrackLibrary();

    private static TrackFilter trackFilter = (track_id, player) -> TrackFilter.Result.ALLOW;

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

            if (!((IceStomPlayer) player).hasPermission(PERMISSION_BYPASS_FILTER)) {
                TrackFilter.Result result = trackFilter.apply(track_id, player);

                if (result == TrackFilter.Result.DENY) {
                    commandSender.sendMessage(Component.translatable("command.timetrial.deny_track", Component.text(track_id)));
                } else if (result == TrackFilter.Result.HIDE) {
                    commandSender.sendMessage(Component.translatable("command.generic.unknown_track", Component.text(track_id)));
                }
            }

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

        boolean bypassFilter;

        if (commandSender instanceof IceStomPlayer player) bypassFilter = player.hasPermission(PERMISSION_BYPASS_FILTER);
        else { bypassFilter = false; }

        trackLibrary.getAvailableTracks()
                .stream()
                .filter(string -> {
                    if (bypassFilter) return true;

                    if (commandSender instanceof IceStomPlayer player) {
                        return trackFilter.apply(string, player) != TrackFilter.Result.HIDE;
                    }

                    return true;
                })
                .filter(track -> input.isBlank() || input.charAt(0) == 0 || track.toLowerCase().startsWith(input))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(20)
                .forEach(track ->
                        suggestion.addEntry(
                                new SuggestionEntry(
                                        track,
                                        Component.text("${track_description}", NamedTextColor.GRAY)
                                )
                        )
                );
    }

    public static void updateFilter(Function<TrackFilter, TrackFilter> updater) {
        trackFilter = updater.apply(trackFilter);
    }

    @FunctionalInterface
    public interface TrackFilter {
        @NotNull Result apply(String track_id, Player player);

        enum Result {
            HIDE,
            DENY,
            ALLOW
        }
    }
}
