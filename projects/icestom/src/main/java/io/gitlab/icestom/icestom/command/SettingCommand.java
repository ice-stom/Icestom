package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.database.preference.PreferenceKey;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class SettingCommand extends Command {

    public SettingCommand() {
        super("settings", "s");

        addSubcommand(new SetSettingSubcommand<>(TrackInstance.BOAT_TYPE, List.of(
                Key.key("oak_boat"),
                Key.key("spruce_boat"),
                Key.key("birch_boat"),
                Key.key("jungle_boat"),
                Key.key("acacia_boat"),
                Key.key("dark_oak_boat"),
                Key.key("mangrove_boat"),
                Key.key("cherry_boat"),
                Key.key("pale_oak_boat"),
                Key.key("poplar_boat"),
                Key.key("bamboo_raft"),
                Key.key("oak_chest_boat"),
                Key.key("spruce_chest_boat"),
                Key.key("birch_chest_boat"),
                Key.key("jungle_chest_boat"),
                Key.key("acacia_chest_boat"),
                Key.key("dark_oak_chest_boat"),
                Key.key("mangrove_chest_boat"),
                Key.key("cherry_chest_boat"),
                Key.key("pale_oak_chest_boat"),
                Key.key("poplar_chest_boat"),
                Key.key("bamboo_chest_raft")
        ), Key::key));
    }
    public static class SetSettingSubcommand<T> extends Command {
        public SetSettingSubcommand(PreferenceKey<T> key, List<T> options, Function<String, T> convert) {
            super(key.key().toString());

            var value = ArgumentType.String("value")
                    .setSuggestionCallback((sender, context, suggestion) -> {
                        String input = Arrays.stream(context.getInput().split(" ")).toList().getLast().toLowerCase();

                        options.stream()
                                .map(Object::toString)
                                .filter(track -> input.isBlank() || input.charAt(0) == 0 || track.toLowerCase().startsWith(input))
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .limit(20)
                                .forEach(str -> suggestion.addEntry(
                                        new SuggestionEntry(
                                                str.split(":")[1],
                                                Component.text(str, NamedTextColor.GOLD)
                                        )
                                ));
                    });

            addSyntax((sender, context) -> {
                if (!(sender instanceof IceStomPlayer player)) {
                    sender.sendMessage(Component.translatable("command.generic.must_be_player"));
                    return;
                };

                T v = convert.apply(context.get(value));

                if (!options.contains(v)) {
                    sender.sendMessage(Component.translatable("command.setting.set.bad_option",
                            Argument.string("key", key.key().toString()),
                            Argument.string("value", v.toString())
                    ));
                    return;
                }

                player.preference(key, v);

                sender.sendMessage(Component.translatable("command.setting.set",
                        Argument.string("key", key.key().toString()),
                        Argument.string("value", player.preference(key).toString())
                ));
            }, value);

            setDefaultExecutor((sender, context) -> {
                if (!(sender instanceof IceStomPlayer player)) {
                    sender.sendMessage(Component.translatable("command.generic.must_be_player"));
                    return;
                };

                player.preference(key);

                sender.sendMessage(Component.translatable("command.setting.set",
                        Argument.string("key", key.key().toString()),
                        Argument.string("value", player.preference(key).toString())
                ));
            });

        }
    }
}
