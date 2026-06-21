package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.timetrial.TimeTrialManager;
import io.gitlab.icestom.icestom.instance.DefaultSpawnInstance;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

public class SpawnCommand extends Command {

    private static final TimeTrialManager timeTrialManager = IceStom.getInstance().getTimeTrialManager();

    public SpawnCommand() {
        super("spawn");

        setDefaultExecutor((commandSender, commandContext) -> {
            if (!(commandSender instanceof Player player)) {
                commandSender.sendMessage(Component.translatable("command.generic.must_be_player"));
                return;
            }

            Instance instance = player.getInstance();

            if (instance instanceof DefaultSpawnInstance spawnInstance) {
                spawnInstance.resetPlayer(player);
            } else if (instance instanceof TimeTrialingInstance) {
                timeTrialManager.stopTimeTrialing(player);
                IceStom.getInstance().getSpawnInstance().consume(player);
            }
        });
    }
}
