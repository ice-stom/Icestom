package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.linefold.LZ4SinglePositionReader;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.timer.TaskSchedule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class PlaybackCommand extends Command {

    public PlaybackCommand() {
        super("playback");

        var filenameArg = ArgumentType.String("filename");

        // Dynamically provide file suggestions from the "recordings" directory
        filenameArg.setSuggestionCallback((sender, context, suggestion) -> {
            Path recordingsDir = Path.of("recordings");
            if (Files.exists(recordingsDir) && Files.isDirectory(recordingsDir)) {
                try (Stream<Path> paths = Files.list(recordingsDir)) {
                    paths.filter(Files::isRegularFile)
                            .map(path -> path.getFileName().toString())
                            .forEach(s -> {
                                suggestion.addEntry(new SuggestionEntry(s));
                            });
                } catch (IOException ignored) {
                    // Handle or ignore read errors during tab-completion
                }
            }
        });

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage("Usage: /playback <filename>");
        });

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can execute this command.");
                return;
            }

            String filename = context.get(filenameArg);
            Path path = Path.of("recordings").resolve(filename);

            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                player.sendMessage("Recording file not found: " + filename);
                return;
            }

            List<Pos> positions = new ArrayList<>();
            try {
                LZ4SinglePositionReader.readAll(path, (x, y, z) -> {
                    positions.add(new Pos(x, y, z));
                });
            } catch (IOException e) {
                player.sendMessage("Failed to read recording: " + e.getMessage());
                return;
            }

            if (positions.isEmpty()) {
                player.sendMessage("The recording contains no position frames.");
                return;
            }

            player.sendMessage("Starting playback of " + positions.size() + " frames...");
            Instance instance = player.getInstance();
            AtomicInteger currentIndex = new AtomicInteger(0);

            instance.scheduler().submitTask(() -> {
                int index = currentIndex.getAndIncrement();
                if (index >= positions.size()) {
                    player.sendMessage("Playback finished.");
                    return TaskSchedule.stop();
                }

                Pos pos = positions.get(index);

                ParticlePacket particlePacket = new ParticlePacket(
                        Particle.ANGRY_VILLAGER,
                        true,
                        true,
                        pos.x(), pos.y(), pos.z(),
                        0f, 0f, 0f,
                        0f,
                        1
                );

                instance.sendGroupedPacket(particlePacket);

                return TaskSchedule.nextTick();
            });

        }, filenameArg);
    }
}