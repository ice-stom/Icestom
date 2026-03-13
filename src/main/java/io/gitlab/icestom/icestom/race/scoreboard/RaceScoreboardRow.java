package io.gitlab.icestom.icestom.race.scoreboard;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RaceScoreboardRow {
    private final UUID player;
    private long delta;

    @Nullable private Long flapTime;

    private int completedLaps;
    private int completedPits;

    private boolean pit;
    private boolean dnf;

    public RaceScoreboardRow(UUID player, long delta,
                             @Nullable Long flapTime, int completedLaps, int completedPits, boolean pit, boolean dnf) {
        this.player = player;
        this.delta = delta;
        this.flapTime = flapTime;
        this.completedLaps = completedLaps;
        this.completedPits = completedPits;
        this.pit = pit;
        this.dnf = dnf;
    }

    public UUID getPlayer() {
        return player;
    }

    public long getDelta() {
        return delta;
    }

    public void setDelta(long delta) {
        this.delta = delta;
    }

    public @Nullable Long getFlapTime() {
        return flapTime;
    }

    public void setFlapTime(@Nullable Long flapTime) {
        this.flapTime = flapTime;
    }

    public int getCompletedLaps() {
        return completedLaps;
    }

    public void setCompletedLaps(int completedLaps) {
        this.completedLaps = completedLaps;
    }

    public int getCompletedPits() {
        return completedPits;
    }

    public void setCompletedPits(int completedPits) {
        this.completedPits = completedPits;
    }

    public boolean isPit() {
        return pit;
    }

    public void setPit(boolean pit) {
        this.pit = pit;
    }

    public boolean isDnf() {
        return dnf;
    }

    public void setDnf(boolean dnf) {
        this.dnf = dnf;
    }
}
