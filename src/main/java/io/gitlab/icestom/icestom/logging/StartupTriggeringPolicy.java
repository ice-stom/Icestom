package io.gitlab.icestom.icestom.logging;

import ch.qos.logback.core.rolling.TriggeringPolicyBase;

import java.io.File;

public class StartupTriggeringPolicy<E> extends TriggeringPolicyBase<E> {

    private volatile boolean checked = false;
    private volatile boolean shouldRoll = false;

    @Override
    public boolean isTriggeringEvent(File activeFile, E event) {
        if (!checked) {
            checked = true;
            // Only roll if there's actually prior content worth keeping
            shouldRoll = activeFile != null && activeFile.exists() && activeFile.length() > 0;
        }
        if (shouldRoll) {
            shouldRoll = false; // only fire once
            return true;
        }
        return false;
    }
}