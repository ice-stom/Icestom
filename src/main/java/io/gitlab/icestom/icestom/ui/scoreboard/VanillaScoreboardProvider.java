package io.gitlab.icestom.icestom.ui.scoreboard;

import io.gitlab.icestom.icestom.race.Leaderboard;
import io.gitlab.icestom.icestom.race.RaceLeaderboardSnapshot;
import io.gitlab.icestom.icestom.race.scoreboard.RaceScoreboardProvider;
import io.gitlab.icestom.icestom.race.scoreboard.RaceScoreboardRow;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.race.Race;
import io.gitlab.icestom.icestom.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VanillaScoreboardProvider implements RaceScoreboardProvider, TimeTrialScoreboardProvider {

    private final Map<Player, Sidebar> sidebars = new HashMap<>();

    @Override
    public void startViewing(Player viewer) {
        Sidebar sidebar = new Sidebar(Component.text("Title"));

        for (int i = 0; i < 2; i++) {
            sidebar.createLine(new Sidebar.ScoreboardLine(String.valueOf(i), Component.empty(), 0, Sidebar.NumberFormat.blank()));
        }

        sidebar.addViewer(viewer);

        sidebars.put(viewer, sidebar);
    }


    // TODO: implement this so no memory leak :D
    @Override
    public void stopViewing(Player viewer) {
        sidebars.computeIfPresent(viewer, (player, sidebar) -> {
            sidebar.removeViewer(player);

            return null;
        });
    }

    @Override
    public void dispatchRaceLeaderboard(Race race) {
        Leaderboard leaderboard = race.getLeaderboard();

        RaceLeaderboardSnapshot snapshot = leaderboard.getSnapshot();

        List<RaceScoreboardRow> rows = snapshot.getRows();
        for (int i = 0; i < rows.size(); i++) {
            RaceScoreboardRow row = rows.get(i);

            for (Player player : race.getPlayers()) {
                @Nullable Sidebar sidebar = sidebars.get(player);

                if (sidebar == null) return;

                sidebar.updateLineContent(String.valueOf(i), Component.text(row.getPlayer().toString()).append(Component.space()).append(Component.text(row.getCompletedLaps())));
            }
        }
    }

    @Override
    public void dispatchTimeTrialLeaderboard(TimeTrialingInstance instance) {
        for (Player player : instance.getPlayers()) {
            @Nullable Sidebar sidebar = sidebars.get(player);

            if (sidebar == null) return;

            sidebar.addViewer(player);

            Track track = instance.getTrack();

            sidebar.updateLineContent("0", Component.empty()
                    .append(Component.text("Track: "))
                    .append(Component.text(track.getId(), NamedTextColor.GOLD)));

            sidebar.updateLineContent("1", Component.empty()
                    .append(Component.text("Checkpoints: "))
                    .append(Component.text(track.getCheckpoints().size(), NamedTextColor.GOLD)));
        }
    }
}
