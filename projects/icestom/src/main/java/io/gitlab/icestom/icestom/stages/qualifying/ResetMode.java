package io.gitlab.icestom.icestom.stages.qualifying;

import io.gitlab.icestom.icestom.event.stage.InvalidStageArgumentsException;

import java.util.Locale;

public enum ResetMode {
    AFTER_LAP(true, true),
    ON_REQUEST(false, true),
    NEVER(false, false);

    private final boolean returnsAfterLap;
    private final boolean allowsManualReset;

    ResetMode(boolean returnsAfterLap, boolean allowsManualReset) {
        this.returnsAfterLap = returnsAfterLap;
        this.allowsManualReset = allowsManualReset;
    }

    public boolean returnsAfterLap() {
        return returnsAfterLap;
    }

    public boolean allowsManualReset() {
        return allowsManualReset;
    }

    public static ResetMode of(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidStageArgumentsException("'reset' must be one of after_lap, on_request or never");
        }
    }
}
