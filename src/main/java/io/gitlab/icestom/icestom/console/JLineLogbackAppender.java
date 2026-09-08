package io.gitlab.icestom.icestom.console;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import org.jline.reader.LineReader;

import java.nio.charset.StandardCharsets;

public class JLineLogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static volatile LineReader reader;

    private Encoder<ILoggingEvent> encoder;

    static void attach(LineReader lineReader) {
        reader = lineReader;
    }

    static void detach() {
        reader = null;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder set for appender " + getName());
            return;
        }

        if (encoder instanceof ContextAware contextAware) contextAware.setContext(getContext());
        if (encoder instanceof LifeCycle lifeCycle && !lifeCycle.isStarted()) lifeCycle.start();

        super.start();
    }

    @Override
    public void stop() {
        if (encoder instanceof LifeCycle lifeCycle && lifeCycle.isStarted()) lifeCycle.stop();

        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) return;

        String line = new String(encoder.encode(event), StandardCharsets.UTF_8);

        LineReader current = reader;

        if (current == null) {
            System.out.print(line);
            System.out.flush();
            return;
        }

        current.printAbove(line.stripTrailing());
    }
}
