package io.gitlab.icestom.icestom.leaderboard;

public interface LeaderboardRow<Participant> {
    Participant getParticipant();

    void setDelta(long delta);
}
