package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.race.scoreboard.RaceScoreboardRow;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class RaceLeaderboardSnapshotRecord implements RaceLeaderboardSnapshot {

    private final String trackName;
    private final int totalLaps;
    private final int totalPits;
    @Nullable private final UUID flapHolder;
    private final List<RaceScoreboardRow> rows;

    public RaceLeaderboardSnapshotRecord(String trackName, int totalLaps, int totalPits, @Nullable UUID flapHolder, List<RaceScoreboardRow> rows) {
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
    public List<RaceScoreboardRow> getRows() {
        return rows;
    }
}
