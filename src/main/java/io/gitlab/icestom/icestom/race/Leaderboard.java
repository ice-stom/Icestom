package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.race.scoreboard.RaceScoreboardRow;
import io.gitlab.icestom.icestom.timetrial.Split;
import net.minestom.server.entity.Player;

import java.util.*;

public class Leaderboard {

    private final Race race;
    private final List<RaceScoreboardRow> leaderboard = new ArrayList<>();
    private final Map<UUID, RaceScoreboardRow> playerRows = new HashMap<>();

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

    public void update(UUID player, Split latest) {
        Race.RaceParticipation participation = race.getParticipant(player);
        assert participation != null;

        RaceScoreboardRow row = playerRows.get(player);
        int currentPosition = leaderboard.indexOf(row);
        int newPos = currentPosition;

        while (newPos > 0) {
            RaceScoreboardRow aheadRow = leaderboard.get(newPos - 1);
            Race.RaceParticipation aheadParticipation = race.getParticipant(aheadRow.getPlayer());
            assert aheadParticipation != null;

            int checkpointLead = aheadParticipation.getGlobalCheckpointIndex() - participation.getGlobalCheckpointIndex();

            if (checkpointLead > 0) break;

            if (checkpointLead == 0) {
                if (aheadParticipation.getSplits().isEmpty() || latest == null) break;
                if (aheadParticipation.getSplits().getLast().ms() < latest.ms()) break;
            }

            newPos--;
        }

        leaderboard.remove(currentPosition);
        leaderboard.add(newPos, row);

        row.setCompletedLaps(participation.getCompletedLaps());
        row.setCompletedPits(participation.getCompletedPits());

        if (newPos > 0) {
            RaceScoreboardRow aheadRow = leaderboard.get(newPos - 1);
            Race.RaceParticipation aheadParticipation = race.getParticipant(aheadRow.getPlayer());
            assert aheadParticipation != null;
            row.setDelta(participation.deltaTo(aheadParticipation));
        } else {
            row.setDelta(0);
        }

        RaceScoreboardRow prev = row;
        for (int i = newPos + 1; i <= currentPosition; i++) {
            RaceScoreboardRow current = leaderboard.get(i);

            Race.RaceParticipation aheadP = race.getParticipant(prev.getPlayer());
            Race.RaceParticipation behindP = race.getParticipant(current.getPlayer());

            assert aheadP != null;
            assert behindP != null;

            current.setDelta(behindP.deltaTo(aheadP));
            prev = current;
        }
    }

    public void addPlayer(Player player) {
        if (playerRows.containsKey(player.getUuid())) return;

        RaceScoreboardRow row = new RaceScoreboardRow(
                player.getUuid(),
                0,
                null,
                -1,
                0,
                false,
                false
        );

        leaderboard.add(row);
        playerRows.put(player.getUuid(), row);
    }
}
