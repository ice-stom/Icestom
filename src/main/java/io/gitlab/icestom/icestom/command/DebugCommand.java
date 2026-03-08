package io.gitlab.icestom.icestom.command;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;

public class DebugCommand extends Command {
    public DebugCommand() {
        super("debug");

        addSubcommand(new LetMeOutCommand());
        addSubcommand(new InstanceList());
    }

    public static class LetMeOutCommand extends Command {
        public LetMeOutCommand() {
            super("letmeout");

            setDefaultExecutor((commandSender, _) -> {
                if (!(commandSender instanceof Player player)) return;

                @Nullable Entity vehicle = player.getVehicle();

                if (vehicle != null) {
                    vehicle.removePassenger(player);
                }
            });
        }
    }

    public static class InstanceList extends Command {
        public InstanceList() {
            super("instancelist");

            setDefaultExecutor((commandSender, _) -> {
                Component text = Component.text("Instances:\n");

                for (Instance instance : MinecraftServer.getInstanceManager().getInstances()) {
                    text = text.append(Component.text(" - " + instance.getClass().getSimpleName() + "\n"));
                }

                commandSender.sendMessage(text);
            });
        }
    }
}
