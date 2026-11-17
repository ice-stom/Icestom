package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.instance.BoatInstance;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;

public class ChangeBoatCommand extends Command {

    // I don't wanna deal with chest boats because I'm lazy
    private static final String[] BOAT_TYPES = EntityType.values().stream()
            .map(entityType -> entityType.key().value())
            .filter(name -> (name.endsWith("_boat") || name.endsWith("_raft")) && !name.contains("chest"))
            .sorted()
            .toArray(String[]::new);

    public ChangeBoatCommand() {
        super("changeboat");

        var typeArgument = ArgumentType.Word("type").from(BOAT_TYPES);

        setDefaultExecutor((commandSender, _) ->
                commandSender.sendMessage(Component.translatable("command.changeboat.usage",
                        Argument.string("types", String.join(", ", BOAT_TYPES)))));

        setArgumentCallback((commandSender, exception) ->
                commandSender.sendMessage(Component.translatable("command.changeboat.unknown_type",
                        Argument.string("type", exception.getInput()))), typeArgument);

        addSyntax((commandSender, commandContext) -> {
            if (!(commandSender instanceof Player player)) {
                commandSender.sendMessage(Component.translatable("command.generic.must_be_player"));
                return;
            }

            final String type = commandContext.get(typeArgument);

            ((IceStomPlayer) player).preference(BoatInstance.BOAT_TYPE, Key.key(type));

            if (player.getInstance() instanceof TimeTrialingInstance timeTrialingInstance && timeTrialingInstance.allowsManualReset()) {
                timeTrialingInstance.resetPlayer(player); // can be removed, idk what's better
                player.sendMessage(Component.translatable("command.changeboat.changed", Argument.string("type", type)));
                return;
            }

            player.sendMessage(Component.translatable("command.changeboat.changed_next", Argument.string("type", type)));
        }, typeArgument);
    }
}
