package io.gitlab.icestom.icestom.console;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.CommandResult;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOError;
import java.io.IOException;

public class Console {

    private static final Logger log = LoggerFactory.getLogger(Console.class);

    private volatile boolean running;

    private Terminal terminal;
    private LineReader reader;

    public void start() {
        try {
            terminal = TerminalBuilder.builder()
                    .name("IceStom")
                    .dumb(true)
                    .build();

            reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .appName("IceStom")
                    .build();
        } catch (IOException e) {
            log.error("Failed to open a console, commands will have to come from in game", e);
            return;
        }

        JLineLogbackAppender.attach(reader);

        running = true;

        while (running) {
            String line;

            try {
                line = reader.readLine("> ");
            } catch (UserInterruptException e) {
                MinecraftServer.stopCleanly();
                break;
            } catch (EndOfFileException e) {
                break;
            } catch (IOError e) {
                break;
            } catch (RuntimeException e) {
                log.error("Console read failed", e);
                break;
            }

            if (line == null) continue;

            String command = line.trim();

            if (command.isEmpty()) continue;

            try {
                CommandResult result = MinecraftServer.getCommandManager().executeServerCommand(command);

                switch (result.getType()) {
                    case UNKNOWN -> log.warn("Unknown command '{}'", command);
                    case INVALID_SYNTAX -> log.warn("Invalid syntax for '{}'", command);
                    default -> {}
                }
            } catch (Throwable throwable) {
                log.error("Console command '{}' threw", command, throwable);
            }
        }

        JLineLogbackAppender.detach();
    }

    public void stop() {
        running = false;

        JLineLogbackAppender.detach();

        if (terminal == null) return;

        try {
            terminal.close();
        } catch (IOException e) {
            log.debug("Failed to close the console terminal", e);
        }
    }
}
