package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.race.ui.RaceLeaderboardRow;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaceLeaderboardSnapshotRecord<R extends RaceLeaderboardRow> implements RaceLeaderboardSnapshot<R> {

    private final String trackName;
    private final int totalLaps;
    private final int totalPits;
    @Nullable private final RaceInstance.RaceParticipant flapHolder;
    private final List<R> rows;
    private final Map<RaceInstance.RaceParticipant, Integer> positions;

    public RaceLeaderboardSnapshotRecord(String trackName, int totalLaps, int totalPits, @Nullable RaceInstance.RaceParticipant flapHolder, List<R> rows) {
        this.trackName = trackName;
        this.totalLaps = totalLaps;
        this.totalPits = totalPits;
        this.flapHolder = flapHolder;
        this.rows = rows;

        this.positions = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            RaceLeaderboardRow row = rows.get(i);

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
    public @Nullable RaceInstance.RaceParticipant getFlapHolder() {
        return flapHolder;
    }

    @Override
    public @NonNull List<R> getRows() {
        return rows;
    }

    @Override
    public int getPosition(RaceInstance.RaceParticipant participant) {
        return positions.getOrDefault(participant, -1);
    }
}
