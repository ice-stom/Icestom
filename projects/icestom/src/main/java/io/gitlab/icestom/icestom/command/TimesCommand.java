package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimeTrialResult;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import io.gitlab.icestom.icestom.util.TextFormatter;
import io.gitlab.icestom.icestom.util.UsernameCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TimesCommand extends Command {

    private static final int PAGE_SIZE = 10;

    private final TrackLibrary trackLibrary = IceStom.getInstance().getTrackLibrary();

    public TimesCommand() {
        super("times");

        var trackArgument = ArgumentType.String("track")
                .setSuggestionCallback(this::suggestionCallback);
        var pageArgument = ArgumentType.Integer("page").min(1);

        setDefaultExecutor((commandSender, commandContext) -> {
            if (!(commandSender instanceof Player player)) {
                commandSender.sendMessage(Component.translatable("command.generic.must_be_player"));
                return;
            }

            if (!(player.getInstance() instanceof TrackInstance trackInstance)) {
                commandSender.sendMessage(Component.translatable("command.times.no_track"));
                return;
            }

            sendTimes(commandSender, trackInstance.getTrack().getId(), 1);
        });

        addSyntax((commandSender, commandContext) ->
                sendTimes(commandSender, commandContext.get(trackArgument), 1), trackArgument);

        addSyntax((commandSender, commandContext) ->
                sendTimes(commandSender, commandContext.get(trackArgument), commandContext.get(pageArgument)), trackArgument, pageArgument);
    }

    private void sendTimes(CommandSender commandSender, String track_id, int page) {
        TimeTrialCommand.TrackFilter.Result result = TimeTrialCommand.checkFilter(track_id, commandSender);

        if (result == TimeTrialCommand.TrackFilter.Result.DENY) {
            commandSender.sendMessage(Component.translatable("command.times.deny_track", Component.text(track_id)));
            return;
        } else if (result == TimeTrialCommand.TrackFilter.Result.HIDE) {
            commandSender.sendMessage(Component.translatable("command.generic.unknown_track", Component.text(track_id)));
            return;
        }

        trackLibrary.loadTrack(track_id).thenAccept(optionalTicket -> {
            if (optionalTicket.isEmpty()) {
                commandSender.sendMessage(Component.translatable("command.generic.unknown_track", Component.text(track_id)));
                return;
            }

            TrackLibrary.Ticket ticket = optionalTicket.get();

            ticket.getTrackMeta().thenAccept(track -> {
                ticket.burn();
                sendTimes(commandSender, track, page);
            });
        });
    }

    private void sendTimes(CommandSender commandSender, Track track, int page) {
        int offset = (page - 1) * PAGE_SIZE;

        // fetch one extra row to know whether a next page exists
        List<TimeTrialResult> results = IceStom.getInstance().getTimetrialDatabase().getBestAttempts(track.getId(), PAGE_SIZE + 1, offset);

        if (results.isEmpty()) {
            commandSender.sendMessage(Component.translatable(page == 1 ? "command.times.no_times" : "command.times.no_page",
                    Argument.component("track", track.getName()),
                    Argument.numeric("page", page)));
            return;
        }

        boolean hasNext = results.size() > PAGE_SIZE;
        List<TimeTrialResult> rows = results.subList(0, Math.min(PAGE_SIZE, results.size()));

        List<CompletableFuture<String>> usernames = rows.stream()
                .map(attempt -> UsernameCache.getUsername(attempt.player()))
                .toList();

        CompletableFuture.allOf(usernames.toArray(CompletableFuture[]::new)).thenRun(() -> {
            TextComponent.Builder message = Component.text();

            message.append(Component.translatable("command.times.header", Argument.component("track", track.getName())));

            for (int i = 0; i < rows.size(); i++) {
                TimeTrialResult attempt = rows.get(i);

                String name = usernames.get(i).join();
                if (name == null) name = "<unknown>";

                int checkpoints = attempt.splits().size() - 1;
                Component checkpointComponent = track.isLastCheckpoint(checkpoints)
                        ? Component.empty()
                        : Component.text(checkpoints + "C ", NamedTextColor.GREEN);

                message.appendNewline();
                message.append(Component.translatable("command.times.entry",
                        Argument.numeric("position", offset + i + 1),
                        Argument.component("player", Component.text(name)),
                        Argument.component("checkpoints", checkpointComponent),
                        Argument.component("time", TextFormatter.getTime(attempt.getTime()))));
            }

            message.appendNewline();
            message.append(Component.translatable("command.times.footer",
                    Argument.component("previous", pageButton("command.times.previous", track.getId(), page - 1, page > 1)),
                    Argument.numeric("page", page),
                    Argument.component("next", pageButton("command.times.next", track.getId(), page + 1, hasNext))));

            commandSender.sendMessage(message.build());
        });
    }

    private static Component pageButton(String key, String track_id, int page, boolean enabled) {
        if (!enabled) {
            return Component.translatable(key, NamedTextColor.DARK_GRAY);
        }

        return Component.translatable(key, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/times " + track_id + " " + page))
                .hoverEvent(HoverEvent.showText(Component.translatable("command.times.goto_page", Argument.numeric("page", page))));
    }

    private void suggestionCallback(CommandSender commandSender, CommandContext commandContext, Suggestion suggestion) {
        String input = Arrays.stream(commandContext.getInput().split(" ")).toList().getLast().toLowerCase();

        trackLibrary.getAvailableTracks()
                .stream()
                .filter(track -> TimeTrialCommand.checkFilter(track, commandSender) != TimeTrialCommand.TrackFilter.Result.HIDE)
                .filter(track -> input.isBlank() || input.charAt(0) == 0 || track.toLowerCase().startsWith(input))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(20)
                .forEach(track -> suggestion.addEntry(new SuggestionEntry(track)));
    }
}
