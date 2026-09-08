package io.gitlab.icestom.icestom.leaderboard;

import io.gitlab.icestom.icestom.timetrial.Split;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Leaderboard<Row extends LeaderboardRow<Participant>, Participant extends LeaderboardParticipant<Participant>> {

    protected final List<Row> leaderboard = new ArrayList<>();
    private final Map<Participant, Row> participantRows = new HashMap<>();

    private final Function<Participant, Row> newRowFactory;

    protected Leaderboard(Function<Participant, Row> newRowFactory) {
        this.newRowFactory = newRowFactory;
    }

    public void update(@NotNull Participant participant, Split latest) {
        Row row = participantRows.get(participant);
        int currentPosition = leaderboard.indexOf(row);
        int newPos = currentPosition;

        while (newPos > 0) {
            Row aheadRow = leaderboard.get(newPos - 1);
            Participant aheadParticipation = aheadRow.getParticipant();
            assert aheadParticipation != null;

            int checkpointLead = aheadParticipation.getGlobalCheckpointIndex() - participant.getGlobalCheckpointIndex();

            if (checkpointLead > 0) break;

            if (checkpointLead == 0) {
                if (aheadParticipation.getRaceSplits().isEmpty() || latest == null) break;
                if (aheadParticipation.getRaceSplits().getLast().ms() < latest.ms()) break;
            }

            newPos--;
        }

        leaderboard.remove(currentPosition);
        leaderboard.add(newPos, row);

        if (newPos > 0) {
            Row aheadRow = leaderboard.get(newPos - 1);
            Participant aheadParticipation = aheadRow.getParticipant();
            assert aheadParticipation != null;
            row.setDelta(participant.deltaTo(aheadParticipation));
        } else {
            row.setDelta(0);
        }

        Row prev = row;
        for (int i = newPos + 1; i <= currentPosition; i++) {
            Row current = leaderboard.get(i);

            Participant aheadP = prev.getParticipant();
            Participant behindP = current.getParticipant();

            assert aheadP != null;
            assert behindP != null;

            current.setDelta(behindP.deltaTo(aheadP));
            prev = current;
        }
    }

    public void addParticipant(Participant participant) {
        if (participantRows.containsKey(participant)) return;

        Row row = newRowFactory.apply(participant);

        leaderboard.add(row);
        participantRows.put(participant, row);
    }
}
