package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.race.RaceStage;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import io.gitlab.icestom.icestom.track.library.source.TrackSource;
import io.gitlab.icestom.icestom.util.TextFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.LightingChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class DebugCommand extends Command {
    public DebugCommand() {
        super("debug");

        addSubcommand(new LetMeOutCommand());
        addSubcommand(new ToGrid());
        addSubcommand(new EscapeGrid());
        addSubcommand(new InstanceList());
        addSubcommand(new Block());
        addSubcommand(new Relight());
        addSubcommand(new Logo());
        addSubcommand(new LibraryCommand());
    }

    public static class LibraryCommand extends Command {

        public LibraryCommand() {
            super("library");

            TrackLibrary library = IceStom.getInstance().getTrackLibrary();

            setDefaultExecutor((sender, context) -> sendUsage(sender));

            var sourcesArg = ArgumentType.Literal("sources");
            var tracksArg = ArgumentType.Literal("tracks");

            addSyntax((sender, context) -> showSources(sender, library), sourcesArg);
            addSyntax((sender, context) -> showTracks(sender, library), tracksArg);
        }

        private void sendUsage(CommandSender sender) {
            sender.sendMessage(Component.text("Usage: /library <sources|tracks>", NamedTextColor.RED));
        }

        private void showSources(CommandSender sender, TrackLibrary library) {
            Map<String, TrackSource> sources = library.getSources();

            if (sources.isEmpty()) {
                sender.sendMessage(Component.text("No sources registered.", NamedTextColor.YELLOW));
                return;
            }

            sender.sendMessage(Component.text("Sources (" + sources.size() + "):", NamedTextColor.GOLD));
            sources.forEach((id, source) -> sender.sendMessage(
                    Component.text(" - " + id + " -> " + source.getClass().getSimpleName(), NamedTextColor.GRAY)
            ));
        }

        private void showTracks(CommandSender sender, TrackLibrary library) {
            Map<UUID, Track> loadedTracks = library.getLoadedTracks();
            Map<UUID, Integer> refCounts = library.getRefCounts();

            if (loadedTracks.isEmpty()) {
                sender.sendMessage(Component.text("No tracks currently loaded.", NamedTextColor.YELLOW));
                return;
            }

            sender.sendMessage(Component.text("Loaded tracks (" + loadedTracks.size() + "):", NamedTextColor.GOLD));
            loadedTracks.forEach((instanceId, track) -> {
                int refs = refCounts.getOrDefault(instanceId, 0);
                sender.sendMessage(Component.text(
                        " - " + instanceId + " [" + track.getClass().getSimpleName() + "] refs=" + refs,
                        NamedTextColor.GRAY
                ));
            });
        }
    }

    public static class LetMeOutCommand extends Command {
        public LetMeOutCommand() {
            super("letmeout");

            setDefaultExecutor((commandSender, _) -> {
                if (!(commandSender instanceof Player player)) return;

                @Nullable Entity vehicle = player.getVehicle();

                if (vehicle != null) {
                    vehicle.removePassenger(player);
                }
            });
        }
    }

    public static class ToGrid extends Command {
        public ToGrid() {
            super("togrid");

            var index = ArgumentType.Integer("track");

            addSyntax((commandSender, commandContext) -> {
                int grid_no = commandContext.get(index);

                if (!(commandSender instanceof Player player)) return;

                if (player.getInstance() instanceof TrackInstance trackInstance) {

                    Track track = trackInstance.getTrack();

                    if (grid_no >= track.getGridLocations().size()) {
                        commandSender.sendMessage(Component.translatable("message.race.no_grid_with_index"));
                        return;
                    }
                    player.teleport(trackInstance.getTrack().getGridLocations().get(grid_no));
                }

            }, index);
        }
    }

    public static class EscapeGrid extends Command {
        public EscapeGrid() {
            super("escape_grid");

            setDefaultExecutor((commandSender, _) -> {
                if (!(commandSender instanceof Player player)) return;

                @Nullable Entity vehicle = player.getVehicle();

                if (vehicle != null) {
                    @Nullable Entity holder = vehicle.getVehicle();

                    if (holder != null) holder.removePassenger(vehicle);
                }
            });
        }
    }

    public static class InstanceList extends Command {
        public InstanceList() {
            super("instancelist");

            setDefaultExecutor((commandSender, _) -> {
                Component text = Component.text("Instances:");

                for (Instance instance : MinecraftServer.getInstanceManager().getInstances()) {
                    text = text.append(Component.text("\n - " + instance.getClass().getSimpleName()));
                }

                if (commandSender instanceof Player player) {
                    text = text.append(Component.text("\n\nYou're in " + player.getInstance().getClass().getSimpleName()));
                }

                commandSender.sendMessage(text);
            });
        }
    }

    public static class Block extends Command {
        public Block() {
            super("block");

            setDefaultExecutor((commandSender, _) -> {
                if (!(commandSender instanceof Player player)) return;

                player.sendMessage(Component.text(player.getInstance().getBlock(player.getPosition()).key().toString()));
            });
        }
    }

    public static class Relight extends Command {
        public Relight() {
            super("relight");

            setDefaultExecutor((commandSender, _) -> {
                if (!(commandSender instanceof Player player)) return;

                player.getInstance().setChunkSupplier(LightingChunk::new);
                LightingChunk.relight(player.getInstance(), player.getInstance().getChunks());
            });
        }
    }

    public static class Logo extends Command {
        public Logo() {
            super("logo");

            setDefaultExecutor((commandSender, _) -> {
                if (!(commandSender instanceof Player player)) return;

                for (int y = 0; y < 9; y++) {
                    player.sendMessage(TextFormatter.getIcestomLogoRow(y));
                }
            });
        }
    }
}
