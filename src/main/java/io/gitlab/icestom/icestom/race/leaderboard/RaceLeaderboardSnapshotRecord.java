package io.gitlab.icestom.icestom.race.leaderboard;

import io.gitlab.icestom.icestom.ui.leaderboard.RaceLeaderboardRow;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class RaceLeaderboardSnapshotRecord implements RaceLeaderboardSnapshot {

    private final String trackName;
    private final int totalLaps;
    private final int totalPits;
    @Nullable private final UUID flapHolder;
    private final List<RaceLeaderboardRow> rows;

    public RaceLeaderboardSnapshotRecord(String trackName, int totalLaps, int totalPits, @Nullable UUID flapHolder, List<RaceLeaderboardRow> rows) {
        this.trackName = trackName;
        this.totalLaps = totalLaps;
        this.totalPits = totalPits;
        this.flapHolder = flapHolder;
        this.rows = rows;
    }

    @Override
    public String getTrackName() {
        return trackName;
    }

    @Override
    public int getTotalLaps() {
        return totalLaps;
    }

    @Override
    public int getTotalPits() {
        return totalPits;
    }

    @Override
    public @Nullable UUID getFlapHolder() {
        return flapHolder;
    }

    @Override
    public List<RaceLeaderboardRow> getRows() {
        return rows;
    }
}
