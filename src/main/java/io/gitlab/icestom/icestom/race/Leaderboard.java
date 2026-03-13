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

        int current_position = leaderboard.indexOf(row);
        int new_pos = current_position;

        Race.RaceParticipation next_row_participant = participation;
        while (new_pos > 0) {
            RaceScoreboardRow next_row = leaderboard.get(new_pos - 1);
            next_row_participant = race.getParticipant(next_row.getPlayer());
            assert next_row_participant != null;

            int behind = next_row_participant.getGlobalCheckpointIndex() - participation.getGlobalCheckpointIndex();
            Split next = next_row_participant.getSplits().getLast();

            if (behind > 0) break;
            if (behind == 0 && next.ms() < latest.ms()) break;

            new_pos--;
        }

        leaderboard.remove(current_position);
        leaderboard.add(new_pos, row);

        row.setCompletedLaps(participation.getCompletedLaps());
        row.setCompletedPits(participation.getCompletedPits());

        row.setDelta(participation.deltaTo(next_row_participant));

        RaceScoreboardRow next = row;

        for (int i = new_pos + 1; i < current_position + 1; i++) {
            RaceScoreboardRow current = leaderboard.get(i);

            Race.RaceParticipation ahead = race.getParticipant(next.getPlayer());
            Race.RaceParticipation behind = race.getParticipant(current.getPlayer());

            assert ahead != null;
            assert behind != null;

            current.setDelta(behind.deltaTo(ahead));

            next = current;
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
