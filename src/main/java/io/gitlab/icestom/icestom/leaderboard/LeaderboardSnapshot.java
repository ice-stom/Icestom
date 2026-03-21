package io.gitlab.icestom.icestom.leaderboard;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface LeaderboardSnapshot<Row, Participant> {
    @NotNull List<Row> getRows();

    int getPosition(Participant participant);
}
