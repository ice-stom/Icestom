package io.gitlab.icestom.icestom.command;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerGameModeRequestEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class GamemodeCommand extends Command {
    public GamemodeCommand() {
        super("gamemode");

        var gamemode = ArgumentType.String("gamemode");

        setDefaultExecutor((commandSender, context) -> {
            if (!(commandSender instanceof Player player)) return;

            if (player.getGameMode() == GameMode.SPECTATOR) {
                MinecraftServer.getGlobalEventHandler()
                        .call(new PlayerGameModeRequestEvent(player, GameMode.ADVENTURE));
            } else {
                MinecraftServer.getGlobalEventHandler()
                        .call(new PlayerGameModeRequestEvent(player, GameMode.SPECTATOR));
            }
        });

        addSyntax((commandSender, commandContext) -> {
            String gamemode_arg = commandContext.get(gamemode);

            if (!(commandSender instanceof Player player)) return;

            @Nullable GameMode gameMode = getGamemode(gamemode_arg);

            if (gameMode != null) {
                MinecraftServer.getGlobalEventHandler()
                        .call(new PlayerGameModeRequestEvent(player, gameMode));
            }

        }, gamemode);
    }

    private static @Nullable GameMode getGamemode(String str) {
        return switch (str.toLowerCase()) {
            case "0", "survival" -> GameMode.SURVIVAL;
            case "a", "adventure" -> GameMode.ADVENTURE;
            case "c", "creative" -> GameMode.CREATIVE;
            case "s", "spec", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }
}
