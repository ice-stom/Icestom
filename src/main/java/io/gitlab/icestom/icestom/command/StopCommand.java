package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.instance.BoatInstance;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class StopCommand extends Command {
    public StopCommand() {
        super("stop");

        setDefaultExecutor((commandSender, _) -> {
            MinecraftServer.stopCleanly();
        });
    }
}
