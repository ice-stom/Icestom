package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.race.scoreboard.RaceScoreboardRow;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RaceLeaderboardSnapshotRecord implements RaceLeaderboardSnapshot {

    private final String trackName;
    private final int totalLaps;
    private final int totalPits;
    @Nullable private final Race.RaceParticipant flapHolder;
    private final List<RaceScoreboardRow> rows;
    private final Map<Race.RaceParticipant, Integer> positions;

    public RaceLeaderboardSnapshotRecord(String trackName, int totalLaps, int totalPits, @Nullable Race.RaceParticipant flapHolder, List<RaceScoreboardRow> rows) {
        this.trackName = trackName;
        this.totalLaps = totalLaps;
        this.totalPits = totalPits;
        this.flapHolder = flapHolder;
        this.rows = rows;

        this.positions = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            RaceScoreboardRow row = rows.get(i);

            positions.put(row.getParticipant(), i);
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
    public @Nullable Race.RaceParticipant getFlapHolder() {
        return flapHolder;
    }

    @Override
    public List<RaceScoreboardRow> getRows() {
        return rows;
    }

    @Override
    public int getPosition(Race.RaceParticipant participant) {
        return positions.getOrDefault(participant, -1);
    }
}
