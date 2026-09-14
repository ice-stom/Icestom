package io.gitlab.icestom.icestom.console;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.jline.reader.LineReader;

import java.nio.charset.StandardCharsets;

public class JLineLogbackAppender extends ConsoleAppender<ILoggingEvent> {
    @Override
    protected void append(ILoggingEvent event) {
        LineReader reader = TerminalConsole.getLineReader();

        if (reader == null) {
            super.append(event);
            return;
        }

        String formatted = new String(encoder.encode(event), StandardCharsets.UTF_8);

        formatted = formatted.replace("\r\n", "\n").replace("\n", "\r\n");

        reader.printAbove(formatted);
    }
}
