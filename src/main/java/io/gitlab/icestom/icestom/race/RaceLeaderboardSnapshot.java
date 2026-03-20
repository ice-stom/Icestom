package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.race.scoreboard.RaceScoreboardRow;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.UUID;

public interface RaceLeaderboardSnapshot {

    String getTrackName();

    int getTotalLaps();
    int getTotalPits();

    Race.RaceParticipant getFlapHolder();

    List<RaceScoreboardRow> getRows();

    int getPosition(Race.RaceParticipant participant);

    default long getDelta(RaceScoreboardRow self, RaceScoreboardRow other) {
        return self.getDelta() - other.getDelta();
    }
}
