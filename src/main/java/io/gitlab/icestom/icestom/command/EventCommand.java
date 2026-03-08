package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.Event;
import io.gitlab.icestom.icestom.event.EventManager;
import io.gitlab.icestom.icestom.event.stage.TrackExploration;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

import java.util.List;

public class EventCommand extends Command {

    private static final EventManager eventManger = IceStom.getInstance().getEventManager();

    public EventCommand() {
        super("event");

        addSubcommand(new EventListCommand());
        addSubcommand(new EventTestCommand());
    }

    public static class EventListCommand extends Command {
        public EventListCommand() {
            super("list");

            setDefaultExecutor((commandSender, commandContext) -> {
                Component text = Component.text("Events:");

                for (Event event : eventManger.getEvents()) {
                    text = text.append(Component.text("\n - " + event.getId()));
                }

                commandSender.sendMessage(text);
            });
        }
    }

    public static class EventTestCommand extends Command {
        public EventTestCommand() {
            super("test");

            setDefaultExecutor((commandSender, commandContext) -> {
                Event event = new Event("test_event", List.of(
                        new TrackExploration(IceStom.getInstance().getTrackLibrary().getTracks().values().stream().findFirst().get())
                ));
                eventManger.addEvent(event);

                for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                    event.addPlayer(player);
                }

                event.nextStage();
            });
        }
    }
}
