package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.instance.BoatInstance;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class BoatCommand extends Command {
    public BoatCommand() {
        super("boat", "b");

        setDefaultExecutor((commandSender, _) -> {
            if (!(commandSender instanceof Player player)) return;
            if (player.getVehicle() instanceof Boat) return;

            double floor_dist = Math.abs(Math.round(player.getPosition().y()) - player.getPosition().y());

            if (!player.getGameMode().allowFlying() && floor_dist > 0.01) {
                player.sendMessage(Component.translatable("command.boat.not_on_ground"));
                return;
            }

            if (player.getInstance() instanceof BoatInstance boatInstance) {
                boatInstance.createBoat(player, player.getPosition());

                return;
            }

            player.sendMessage(Component.translatable("command.boat.not_here"));
        });
    }
}
