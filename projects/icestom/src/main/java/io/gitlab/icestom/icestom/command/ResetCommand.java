package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class ResetCommand extends Command {
    public ResetCommand() {
        super("reset");

        setDefaultExecutor((commandSender, commandContext) -> {
            if (!(commandSender instanceof Player player)) {
                commandSender.sendMessage(Component.translatable("command.generic.must_be_player"));
                return;
            }

            if (!(player.getInstance() instanceof TimeTrialingInstance timeTrialingInstance)) {
                commandSender.sendMessage(Component.translatable("command.reset.not_timetrialing"));
                return;
            }

            timeTrialingInstance.resetPlayer(player);
        });
    }
}
