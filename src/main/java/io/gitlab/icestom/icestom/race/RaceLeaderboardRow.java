package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.leaderboard.LeaderboardRow;
import org.jetbrains.annotations.Nullable;

public class RaceLeaderboardRow implements LeaderboardRow<RaceStage.RaceParticipant> {
    private final RaceStage.RaceParticipant participant;

    private long delta;

    public RaceLeaderboardRow(RaceStage.RaceParticipant participant, long delta) {
        this.participant = participant;
        this.delta = delta;
    }

    public RaceStage.RaceParticipant getParticipant() {
        return participant;
    }

    public long getDelta() {
        return delta;
    }


    public void setDelta(long delta) {
        this.delta = delta;
    }
}
