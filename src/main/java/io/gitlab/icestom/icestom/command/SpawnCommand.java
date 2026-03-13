package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.timetrial.TimeTrialManager;
import io.gitlab.icestom.icestom.instance.SpawnInstance;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

public class SpawnCommand extends Command {

    private static final TimeTrialManager timeTrialManager = IceStom.getInstance().getTimeTrialManager();

    public SpawnCommand() {
        super("spawn");

        setDefaultExecutor((commandSender, commandContext) -> {
            if (!(commandSender instanceof Player player)) {
                commandSender.sendMessage(Component.text("You must be a player to run this command.", NamedTextColor.RED));
                return;
            }

            Instance instance = player.getInstance();

            if (instance instanceof SpawnInstance spawnInstance) {
                spawnInstance.consume(player);
            } else if (instance instanceof TimeTrialingInstance) {
                timeTrialManager.stopTimeTrialing(player);
            }
        });
    }
}
