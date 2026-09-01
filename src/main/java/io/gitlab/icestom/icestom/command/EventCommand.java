package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.event.*;
import io.gitlab.icestom.icestom.event.lua.LuaEvent;
import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EventCommand extends Command {

    private static final Logger log = LoggerFactory.getLogger(EventCommand.class);

    public EventCommand() {
        super("event");

        addSubcommand(new EventRunCommand());
        addSubcommand(new EventStageCommand());
    }

    public static class EventRunCommand extends Command {
        public EventRunCommand() {
            super("run");

            var fileArgument = ArgumentType.String("file")
                    .setSuggestionCallback(this::suggestionCallback);

            addSyntax((sender, context) -> {
                String filename = context.get(fileArgument);

                long compileStart = System.currentTimeMillis();

                CompletableFuture.supplyAsync(() -> {
                    try {
                        LuaEvent<EventParticipant> event = IceStom.getInstance().getEventManager().loadLuauEvent(filename);

                        if (event == null) {
                            sender.sendMessage(Component.translatable("command.event.load.failed_to_find_event",
                                    Argument.component("filename", Component.text(filename))
                            ));
                        }

                        return event;
                    } catch (LuauCompileException e) {
                        sender.sendMessage(Component.translatable("command.event.load.failed_to_compile_file",
                                Argument.component("filename", Component.text(filename))
                        ));
                        log.error("Failed to compile {}", filename, e);
                        return null;
                    } catch (Exception e) {
                        sender.sendMessage(Component.translatable("command.event.load.failed_to_load_file",
                                Argument.component("filename", Component.text(filename))
                        ));
                        log.error("Failed to load {}", filename, e);
                        return null;
                    }
                }).thenAccept(event -> {
                    if (event == null) return;

                    sender.sendMessage(Component.translatable("command.event.load.compiled_in",
                            Argument.component("filename", Component.text(filename)),
                            Argument.component("time", Component.text(System.currentTimeMillis() - compileStart))
                    ));

                    List<Result<EventParticipant>> results = new ArrayList<>();

                    if (sender instanceof EventParticipant participant) {
                        results.add(new Result<>(participant));
                    }

                    event.begin(results).thenRun(event::cleanup);
                });
            }, fileArgument);
        }

        private void suggestionCallback(CommandSender commandSender, CommandContext commandContext, Suggestion suggestion) {
            String rawInput = suggestion.getInput();
            int start = Math.min(suggestion.getStart(), rawInput.length());
            int end = Math.min(start + suggestion.getLength(), rawInput.length());
            String input = rawInput.substring(start, end).toLowerCase();

            IceStom.getInstance().getEventManager().getEventDefinitions()
                    .stream()
                    .filter(event -> input.isBlank() || event.toLowerCase().startsWith(input))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .limit(20)
                    .forEach(track -> suggestion.addEntry(new SuggestionEntry(track)));
        }
    }

    /**
     * /event stage <event>                           -> lists loaded stages for the active event
     * /event stage <event> <stage>                   -> reports state + transitions for the specified stage
     * /event stage <event> <stage> <transition>       -> runs the specified state transition on the stage
     */
    public static class EventStageCommand extends Command {

        private final net.minestom.server.command.builder.arguments.Argument<String> eventArgument;
        private final net.minestom.server.command.builder.arguments.Argument<String> stageArgument;

        public EventStageCommand() {
            super("stage");

            eventArgument = ArgumentType.String("event")
                    .setSuggestionCallback(this::suggestActiveEvents);

            stageArgument = ArgumentType.String("stage_name")
                    .setSuggestionCallback(this::suggestLoadedStages);

            var transitionArgument = ArgumentType.String("transition")
                    .setSuggestionCallback(this::suggestTransitions);

            // 1. /event stage <event> -> list all loaded stages
            addSyntax((sender, context) -> {
                IceStomEvent<EventParticipant> event = getEvent(sender, context.get(eventArgument));
                if (event == null) return;

                List<EventStage> stages = event.getLoadedStages();
                if (stages.isEmpty()) {
                    sender.sendMessage(Component.translatable("command.event.stage.no_loaded_stages",
                            Argument.component("event", Component.text(event.getId().toString()))
                    ));
                    return;
                }

                String stageList = stages.stream()
                        .map(EventStage::getStageName)
                        .collect(Collectors.joining(", "));

                sender.sendMessage(Component.translatable("command.event.stage.list_stages",
                        Argument.component("event", Component.text(event.getId().toString())),
                        Argument.component("stages", Component.text(stageList))
                ));
            }, eventArgument);

            // 2. /event stage <event> <stage> -> inspect stage state + legal transitions
            addSyntax((sender, context) -> {
                IceStomEvent<EventParticipant> event = getEvent(sender, context.get(eventArgument));
                if (event == null) return;

                String stageName = context.get(stageArgument);
                EventStage stage = getStage(sender, event, stageName);
                if (stage == null) return;

                if (!(stage instanceof Stateful<?> stateful)) {
                    sender.sendMessage(Component.translatable("command.event.stage.not_stateful",
                            Argument.component("stage", Component.text(stageName))
                    ));
                    return;
                }

                Object currentState = stateful.getState();
                String transitions = stateful.getStageChanges().stream()
                        .filter(change -> change.before() == currentState)
                        .map(Stateful.StateChange::name)
                        .collect(Collectors.joining(", "));

                sender.sendMessage(Component.translatable("command.event.stage.info",
                        Argument.component("event", Component.text(event.getId().toString())),
                        Argument.component("stage", Component.text(stageName)),
                        Argument.component("state", Component.text(currentState.toString())),
                        Argument.component("transitions", Component.text(transitions.isEmpty() ? "none" : transitions))
                ));
            }, eventArgument, stageArgument);

            // 3. /event stage <event> <stage> <transition> -> execute a state change on the stage
            addSyntax((sender, context) -> {
                IceStomEvent<EventParticipant> event = getEvent(sender, context.get(eventArgument));
                if (event == null) return;

                String stageName = context.get(stageArgument);
                EventStage stage = getStage(sender, event, stageName);
                if (stage == null) return;

                if (!(stage instanceof Stateful<?> stateful)) {
                    sender.sendMessage(Component.translatable("command.event.stage.not_stateful",
                            Argument.component("stage", Component.text(stageName))
                    ));
                    return;
                }

                String transitionName = context.get(transitionArgument);
                Object currentState = stateful.getState();

                Stateful.StateChange<?> matchingChange = stateful.getStageChanges().stream()
                        .filter(change -> change.before() == currentState && change.name().equals(transitionName))
                        .findFirst()
                        .orElse(null);

                if (matchingChange == null) {
                    sender.sendMessage(Component.translatable("command.event.stage.invalid_transition",
                            Argument.component("stage", Component.text(stageName)),
                            Argument.component("transition", Component.text(transitionName))
                    ));
                    return;
                }

                matchingChange.run().run();

                sender.sendMessage(Component.translatable("command.event.stage.transitioned",
                        Argument.component("stage", Component.text(stageName)),
                        Argument.component("transition", Component.text(transitionName))
                ));
            }, eventArgument, stageArgument, transitionArgument);
        }

        private IceStomEvent<EventParticipant> getEvent(CommandSender sender, String eventIdStr) {
            UUID uuid;
            try {
                uuid = UUID.fromString(eventIdStr);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Component.translatable("command.event.state.not_found",
                        Argument.component("event", Component.text(eventIdStr))
                ));
                return null;
            }

            IceStomEvent<EventParticipant> event = IceStom.getInstance().getEventManager().getActiveEvents().stream()
                    .filter(e -> e.getId().equals(uuid))
                    .findFirst()
                    .orElse(null);

            if (event == null) {
                sender.sendMessage(Component.translatable("command.event.state.not_found",
                        Argument.component("event", Component.text(eventIdStr))
                ));
            }

            return event;
        }

        private EventStage getStage(CommandSender sender, IceStomEvent<EventParticipant> event, String stageName) {
            EventStage stage = event.getLoadedStages().stream()
                    .filter(s -> s.getStageName().equalsIgnoreCase(stageName))
                    .findFirst()
                    .orElse(null);

            if (stage == null) {
                sender.sendMessage(Component.translatable("command.event.stage.not_found",
                        Argument.component("stage", Component.text(stageName))
                ));
            }

            return stage;
        }

        private void suggestActiveEvents(CommandSender sender, CommandContext context, Suggestion suggestion) {
            String rawInput = suggestion.getInput();
            int start = Math.min(suggestion.getStart(), rawInput.length());
            int end = Math.min(start + suggestion.getLength(), rawInput.length());
            String input = rawInput.substring(start, end).toLowerCase();

            IceStom.getInstance().getEventManager().getActiveEvents().stream()
                    .filter(event -> input.isBlank() || event.getId().toString().toLowerCase().startsWith(input))
                    .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getId().toString(), b.getId().toString()))
                    .limit(20)
                    .forEach(event -> suggestion.addEntry(new SuggestionEntry(event.getId().toString())));
        }

        private void suggestLoadedStages(CommandSender sender, CommandContext context, Suggestion suggestion) {
            String eventIdStr = context.get(eventArgument);
            UUID uuid;
            try {
                uuid = UUID.fromString(eventIdStr);
            } catch (IllegalArgumentException e) {
                return;
            }

            IceStomEvent<EventParticipant> event = IceStom.getInstance().getEventManager().getActiveEvents().stream()
                    .filter(e -> e.getId().equals(uuid))
                    .findFirst()
                    .orElse(null);

            if (event == null) return;

            String rawInput = suggestion.getInput();
            int start = Math.min(suggestion.getStart(), rawInput.length());
            int end = Math.min(start + suggestion.getLength(), rawInput.length());
            String input = rawInput.substring(start, end).toLowerCase();

            event.getLoadedStages().stream()
                    .filter(stage -> input.isBlank() || stage.getStageName().toLowerCase().startsWith(input))
                    .forEach(stage -> {
                        String stateInfo = (stage instanceof Stateful<?> stateful)
                                ? stateful.getState().toString()
                                : "not stateful";

                        suggestion.addEntry(new SuggestionEntry(
                                stage.getStageName(),
                                Component.text("State: " + stateInfo)
                        ));
                    });
        }

        private void suggestTransitions(CommandSender sender, CommandContext context, Suggestion suggestion) {
            String eventIdStr = context.get(eventArgument);
            String stageName = context.get(stageArgument);

            UUID uuid;
            try {
                uuid = UUID.fromString(eventIdStr);
            } catch (IllegalArgumentException e) {
                return;
            }

            IceStomEvent<EventParticipant> event = IceStom.getInstance().getEventManager().getActiveEvents().stream()
                    .filter(e -> e.getId().equals(uuid))
                    .findFirst()
                    .orElse(null);

            if (event == null) return;

            EventStage stage = event.getLoadedStages().stream()
                    .filter(s -> s.getStageName().equalsIgnoreCase(stageName))
                    .findFirst()
                    .orElse(null);

            if (!(stage instanceof Stateful<?> stateful)) return;

            String rawInput = suggestion.getInput();
            int start = Math.min(suggestion.getStart(), rawInput.length());
            int end = Math.min(start + suggestion.getLength(), rawInput.length());
            String input = rawInput.substring(start, end).toLowerCase();

            Object currentState = stateful.getState();
            stateful.getStageChanges().stream()
                    .filter(change -> change.before() == currentState)
                    .filter(change -> input.isBlank() || change.name().toLowerCase().startsWith(input))
                    .forEach(change -> suggestion.addEntry(new SuggestionEntry(
                            change.name(),
                            Component.text(change.before() + " -> " + change.after())
                    )));
        }
    }
}