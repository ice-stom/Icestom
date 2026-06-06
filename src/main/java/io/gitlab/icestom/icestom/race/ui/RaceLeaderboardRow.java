package io.gitlab.icestom.icestom.race.ui;

import io.gitlab.icestom.icestom.leaderboard.LeaderboardRow;
import io.gitlab.icestom.icestom.race.RaceInstance;
import org.jetbrains.annotations.Nullable;

public class RaceLeaderboardRow implements LeaderboardRow<RaceInstance.RaceParticipant> {
    private final RaceInstance.RaceParticipant participant;
    private long delta;

    @Nullable private Long flapTime;

    private int completedLaps;
    private int completedPits;

    private boolean pit;
    private boolean dnf;

    public RaceLeaderboardRow(RaceInstance.RaceParticipant participant, long delta,
                              @Nullable Long flapTime, int completedLaps, int completedPits, boolean pit, boolean dnf) {
        this.participant = participant;
        this.delta = delta;
        this.flapTime = flapTime;
        this.completedLaps = completedLaps;
        this.completedPits = completedPits;
        this.pit = pit;
        this.dnf = dnf;
    }

    public RaceInstance.RaceParticipant getParticipant() {
        return participant;
    }

    @Override
    public void update() {
        setCompletedLaps(participant.getCompletedLaps());
        setCompletedPits(participant.getCompletedPits());
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
