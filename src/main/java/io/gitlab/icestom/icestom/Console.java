package io.gitlab.icestom.icestom;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.CommandResult;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Console {

    private static final Logger log = LoggerFactory.getLogger(Console.class);

    private final AtomicBoolean running = new AtomicBoolean(true);

    private Terminal terminal;
    private LineReader reader;
    private JLineAppender appender;

    public void start() {
        try {
            terminal = TerminalBuilder.builder()
                    .name("Icestom")
                    .system(true)
                    .jna(true)
                    .build();

            reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .appName("Icestom")
                    .completer(new MinestomCompleter())
                    .build();

            reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);
            reader.unsetOpt(LineReader.Option.INSERT_TAB);
            reader.setOpt(LineReader.Option.EMPTY_WORD_OPTIONS);
            reader.setOpt(LineReader.Option.AUTO_LIST);
            reader.setKeyMap(LineReader.MAIN);

            reader.getKeyMaps()
                    .get(LineReader.MAIN)
                    .bind(new Reference(LineReader.COMPLETE_WORD), "\t");

            installLogbackAppender();
            readCommands();

        } catch (IOException e) {
            log.error("Failed to initialize console", e);
        } finally {
            stop();
        }
    }

    private void readCommands() {
        while (running.get()) {
            try {
                String line = reader.readLine("> ");

                if (line == null) break;

                processInput(line);
            } catch (EndOfFileException e) {
                break;
            } catch (UserInterruptException e) {
                shutdown();
                break;
            } catch (Exception e) {
                log.error("Error while reading console input", e);
            }
        }
    }

    private void processInput(String input) {
        String command = input.trim();

        if (command.isEmpty()) {
            return;
        }

        runCommand(command);
    }

    private void runCommand(String command) {
        try {
            CommandResult result = MinecraftServer
                    .getCommandManager()
                    .execute(
                            MinecraftServer.getCommandManager().getConsoleSender(),
                            command
                    );

            if (result.getType() == CommandResult.Type.UNKNOWN) {
                log.warn("Unknown command: {}", command);
            } else if (result.getType() == CommandResult.Type.INVALID_SYNTAX) {
                log.warn("Invalid command syntax: {}", command);
            }

        } catch (Exception e) {
            log.error("Error while executing command '{}'", command, e);
        }
    }

    private void installLogbackAppender() {
        LoggerContext context =
                (LoggerContext) LoggerFactory.getILoggerFactory();

        var root = context.getLogger(Logger.ROOT_LOGGER_NAME);

        for (Iterator<Appender<ILoggingEvent>> it = root.iteratorForAppenders(); it.hasNext(); ) {
            var existing = it.next();
            if (existing instanceof ch.qos.logback.core.ConsoleAppender<?>) {
                root.detachAppender(existing);
                existing.stop();
            }
        }

        appender = new JLineAppender();
        appender.setContext(context);
        appender.setReader(reader);
        appender.setName("JLineAppender");
        appender.start();

        root.addAppender(appender);
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        if (appender != null) {
            LoggerContext context =
                    (LoggerContext) LoggerFactory.getILoggerFactory();

            context.getLogger(Logger.ROOT_LOGGER_NAME)
                    .detachAppender(appender);

            appender.stop();
            appender = null;
        }

        if (terminal != null) {
            try {
                terminal.close();
            } catch (IOException e) {
                log.debug("Failed to close terminal", e);
            }

            terminal = null;
        }
    }

    private void shutdown() {
        running.set(false);

        try {
            MinecraftServer.stopCleanly();
        } catch (Exception e) {
            log.error("Failed to stop cleanly", e);
        }
    }

    private static class MinestomCompleter implements Completer {

        @Override
        public void complete(
                LineReader reader,
                ParsedLine line,
                List<Candidate> candidates
        ) {
            if (line.wordIndex() > 0) {
                return;
            }

            var commandManager = MinecraftServer.getCommandManager();

            String word = line.word();
            if (word == null) {
                word = "";
            }

            Set<String> added = new HashSet<>();

            for (var command : commandManager.getCommands()) {
                String name = command.getName();

                if (name.isEmpty()) continue;

                if (!name.regionMatches(
                        true,
                        0,
                        word,
                        0,
                        word.length()
                )) {
                    continue;
                }

                if (!added.add(name.toLowerCase())) {
                    continue;
                }

                candidates.add(new Candidate(
                        name,
                        name,
                        null,
                        "Command",
                        null,
                        null,
                        false
                ));
            }
        }
    }

    private static class JLineAppender extends AppenderBase<ILoggingEvent> {

        private volatile LineReader reader;

        public void setReader(LineReader reader) {
            this.reader = reader;
        }

        @Override
        protected void append(ILoggingEvent event) {
            LineReader currentReader = reader;

            if (currentReader == null) {
                return;
            }

            String message = event.getFormattedMessage();

            if (message == null || message.isEmpty()) {
                return;
            }

            try {
                currentReader.printAbove(message);
            } catch (Exception ignored) {
            }
        }
    }
}
