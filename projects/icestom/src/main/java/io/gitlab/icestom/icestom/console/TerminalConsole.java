package io.gitlab.icestom.icestom.console;

import org.jline.reader.LineReader;

import java.util.concurrent.atomic.AtomicReference;

public class TerminalConsole {
    private static final AtomicReference<LineReader> LINE_READER = new AtomicReference<>();

    private TerminalConsole() {
    }

    public static void setLineReader(LineReader reader) {
        LINE_READER.set(reader);
    }

    public static LineReader getLineReader() {
        return LINE_READER.get();
    }
}
