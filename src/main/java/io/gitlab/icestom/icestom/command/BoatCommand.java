package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.instance.BoatInstance;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class BoatCommand extends Command {
    public BoatCommand() {
        super("boat");

        setDefaultExecutor((commandSender, _) -> {
            if (!(commandSender instanceof Player player)) return;

            if (player.getInstance() instanceof BoatInstance boatInstance) {
                boatInstance.createBoat(player, player.getPosition());
            }
        });
    }
}
