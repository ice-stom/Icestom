package io.gitlab.icestom.icestom.command;

import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DebugCommand extends Command {
    public DebugCommand() {
        super("debug");

        addSubcommand(new LetMeOutCommand());
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
}
