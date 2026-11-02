package io.gitlab.icestom.protocol;

public enum ISChannel {
    CONFIGURATION("icestom:configuration", Phase.CONFIGURATION),
    TIMETRIAL("icestom:timetrial", Phase.PLAY),
    TRACK("icestom:track", Phase.PLAY);

    private final String identifier;
    private final Phase phase;

    ISChannel(String identifier, Phase phase) {
        this.identifier = identifier;
        this.phase = phase;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Phase getPhase() {
        return phase;
    }

    public enum Phase {
        CONFIGURATION,
        PLAY
    }
}
