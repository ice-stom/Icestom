package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.race.scoreboard.RaceScoreboardRow;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RaceLeaderboardSnapshotRecord implements RaceLeaderboardSnapshot {

    private final String trackName;
    private final int totalLaps;
    private final int totalPits;
    @Nullable private final UUID flapHolder;
    private final List<RaceScoreboardRow> rows;
    private final Map<UUID, Integer> positions;

    public RaceLeaderboardSnapshotRecord(String trackName, int totalLaps, int totalPits, @Nullable UUID flapHolder, List<RaceScoreboardRow> rows) {
        this.trackName = trackName;
        this.totalLaps = totalLaps;
        this.totalPits = totalPits;
        this.flapHolder = flapHolder;
        this.rows = rows;

        this.positions = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            RaceScoreboardRow row = rows.get(i);

            positions.put(row.getPlayer(), i);
        }
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

    @Override
    public int getPosition(UUID player) {
        return positions.getOrDefault(player, -1);
    }
}
