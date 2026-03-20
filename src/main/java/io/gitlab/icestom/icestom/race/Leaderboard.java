package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.race.scoreboard.RaceScoreboardRow;
import io.gitlab.icestom.icestom.timetrial.Split;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Leaderboard {

    private final Race race;
    private final List<RaceScoreboardRow> leaderboard = new ArrayList<>();
    private final Map<Race.RaceParticipant, RaceScoreboardRow> playerRows = new HashMap<>();

    public Leaderboard(Race race) {
        this.race = race;
    }

    public RaceLeaderboardSnapshot getSnapshot() {
        return new RaceLeaderboardSnapshotRecord(
                race.getTrack().getId(),
                race.getTotalLaps(),
                race.getTotalPits(),
                null,
                leaderboard
        );
    }

    public void update(@NotNull Race.RaceParticipant participant, Split latest) {
        RaceScoreboardRow row = playerRows.get(participant);
        int currentPosition = leaderboard.indexOf(row);
        int newPos = currentPosition;

        while (newPos > 0) {
            RaceScoreboardRow aheadRow = leaderboard.get(newPos - 1);
            Race.RaceParticipant aheadParticipation = aheadRow.getParticipant();
            assert aheadParticipation != null;

            int checkpointLead = aheadParticipation.getGlobalCheckpointIndex() - participant.getGlobalCheckpointIndex();

            if (checkpointLead > 0) break;

            if (checkpointLead == 0) {
                if (aheadParticipation.getSplits().isEmpty() || latest == null) break;
                if (aheadParticipation.getSplits().getLast().ms() < latest.ms()) break;
            }

            newPos--;
        }

        leaderboard.remove(currentPosition);
        leaderboard.add(newPos, row);

        row.setCompletedLaps(participant.getCompletedLaps());
        row.setCompletedPits(participant.getCompletedPits());

        if (newPos > 0) {
            RaceScoreboardRow aheadRow = leaderboard.get(newPos - 1);
            Race.RaceParticipant aheadParticipation = aheadRow.getParticipant();
            assert aheadParticipation != null;
            row.setDelta(participant.deltaTo(aheadParticipation));
        } else {
            row.setDelta(0);
        }

        RaceScoreboardRow prev = row;
        for (int i = newPos + 1; i <= currentPosition; i++) {
            RaceScoreboardRow current = leaderboard.get(i);

            Race.RaceParticipant aheadP = prev.getParticipant();
            Race.RaceParticipant behindP = current.getParticipant();

            assert aheadP != null;
            assert behindP != null;

            current.setDelta(behindP.deltaTo(aheadP));
            prev = current;
        }
    }

    public void addParticipant(Race.RaceParticipant participant) {
        if (playerRows.containsKey(participant)) return;

        RaceScoreboardRow row = new RaceScoreboardRow(
                participant,
                0,
                null,
                -1,
                0,
                false,
                false
        );

        leaderboard.add(row);
        playerRows.put(participant, row);
    }
}
