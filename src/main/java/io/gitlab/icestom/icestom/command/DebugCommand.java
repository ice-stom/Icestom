package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.openboatutils.OBUContextPackets;
import io.gitlab.icestom.icestom.openboatutils.OBUSettingsPackets;
import io.gitlab.icestom.icestom.openboatutils.GroupedPacketPayload;
import io.gitlab.icestom.icestom.race.RaceInstance;
import io.gitlab.icestom.icestom.track.Track;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.LightingChunk;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class DebugCommand extends Command {
    public DebugCommand() {
        super("debug");

        addSubcommand(new LetMeOutCommand());
        addSubcommand(new ToGrid());
        addSubcommand(new EscapeGrid());
        addSubcommand(new InstanceList());
        addSubcommand(new StartRace());
        addSubcommand(new TestEntityContext());
        addSubcommand(new Block());
        addSubcommand(new Relight());
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

                commandSender.sendMessage(text);
            });
        }
    }

    public static class StartRace extends Command {
        public StartRace() {
            super("startrace");

            setDefaultExecutor((commandSender, _) -> {
                if (!(commandSender instanceof Player player)) return;

                if (player.getInstance() instanceof RaceInstance raceInstance) raceInstance.startCountdown();
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

    public static class TestEntityContext extends Command {
        public TestEntityContext() {
            super("testentityContext");

            setDefaultExecutor((commandSender, _) -> {
                if (!(commandSender instanceof Player player)) return;

                if (player.getVehicle() instanceof Boat boat) {
                    try {
                        player.sendPacket(new OBUContextPackets.EntityContext(
                                boat.getUuid(),
                                new GroupedPacketPayload(List.of(
                                        new OBUSettingsPackets.ForwardAccelPacket(-1)
                                ))
                        ).toPacket(OBUContextPackets.getChannel()));
                    } catch (IOException _) {}
                }
            });
        }
    }
}
