package io.gitlab.icestom.icestom.ui.impl;

import io.gitlab.icestom.icestom.race.RaceLeaderboard;
import io.gitlab.icestom.icestom.race.RaceLeaderboardSnapshot;
import io.gitlab.icestom.icestom.race.ui.RaceInterfaceProvider;
import io.gitlab.icestom.icestom.race.ui.RaceLeaderboardRow;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.race.Race;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialLapCompletedEvent;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialSessionStartEvent;
import io.gitlab.icestom.icestom.timetrial.ui.TimeTrialInterfaceProvider;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.util.TextFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.object.ObjectContents;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.scoreboard.Sidebar;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VanillaInterface implements RaceInterfaceProvider, TimeTrialInterfaceProvider {

    private final Map<Player, Sidebar> sidebars = new HashMap<>();
    private final Map<UUID, String> name = new HashMap<>();

    public VanillaInterface() {
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(TimeTrialSessionStartEvent.class, event -> {
            final Player player = event.getPlayer();
            final Track track = event.getInstance().getTrack();

            player.sendMessage(Component.translatable("message.timetrial.start", Argument.component("track", track.getName())));

            globalEventHandler.addListener(TimeTrialLapCompletedEvent.class, timeTrialLapCompletedEvent -> {

            });
        });
    }

    @Override
    public void startViewing(Player viewer) {
        Sidebar sidebar = new Sidebar(Component.empty());

        for (int i = 0; i < 15; i++) {
            sidebar.createLine(new Sidebar.ScoreboardLine(String.valueOf(i), Component.empty(), 0, Sidebar.NumberFormat.blank()));
        }

        sidebar.addViewer(viewer);

        sidebars.put(viewer, sidebar);
    }


    @Override
    public void stopViewing(Player viewer) {
        @Nullable Sidebar sidebar = sidebars.remove(viewer);

        if (sidebar != null) sidebar.removeViewer(viewer);
    }

    private Component sidebarLeaderboardEntry(RaceLeaderboardRow row) {
        UUID id = row.getParticipant().getCurrentPlayer().getUuid();

        String username = name.computeIfAbsent(id, uuid -> {
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);

            if (player == null) return "<unknown>";

            return player.getUsername();
        });

        return Component.object(ObjectContents.playerHead(id))
                .append(Component.text(" "))
                .append(TextFormatter.getDelta(row.getDelta()))
                .append(Component.text(" " + username));
    }

    @Override
    public void dispatchRaceLeaderboard(Race race) {
        RaceLeaderboard<RaceLeaderboardRow> raceLeaderboard = race.getLeaderboard();

        RaceLeaderboardSnapshot<RaceLeaderboardRow> snapshot = raceLeaderboard.getSnapshot();

        List<RaceLeaderboardRow> rows = snapshot.getRows();
        for (Player player : race.getPlayers()) {
            @Nullable Sidebar sidebar = sidebars.get(player);

            if (sidebar == null) continue;

            sidebar.setTitle(Component.text("Race at " + race.getTrack().getId()));

            for (int i = 0; i < rows.size(); i++) {
                RaceLeaderboardRow row = rows.get(i);

                sidebar.updateLineContent(String.valueOf(i), sidebarLeaderboardEntry(row));
            }
        }
    }

    @Override
    public void dispatchTimeTrialLeaderboard(TimeTrialingInstance instance) {
        for (Player player : instance.getPlayers()) {
            @Nullable Sidebar sidebar = sidebars.get(player);

            if (sidebar == null) continue;

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
