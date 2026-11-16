package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class LonelyCommand extends Command {
    public LonelyCommand() {
        super("lonely");

        setDefaultExecutor((commandSender, _) -> {
            if (!(commandSender instanceof Player player)) {
                commandSender.sendMessage(Component.translatable("command.generic.must_be_player"));
                return;
            }

            toggleLonely(player);
        });
    }

    public static void toggleLonely(Player player) {
        IceStomPlayer iceStomPlayer = (IceStomPlayer) player;

        boolean lonely = !iceStomPlayer.preference(TimeTrialingInstance.LONELY);
        iceStomPlayer.preference(TimeTrialingInstance.LONELY, lonely);

        if (player.getInstance() instanceof TimeTrialingInstance timeTrialingInstance) {
            timeTrialingInstance.updateLonely(player);
        }

        player.sendMessage(Component.translatable(lonely ? "command.lonely.enabled" : "command.lonely.disabled"));
    }
}
