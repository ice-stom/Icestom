package io.gitlab.icestom.icestom.ui.interfaces.impl;

import io.gitlab.icestom.icestom.race.RaceInstance;
import io.gitlab.icestom.icestom.race.RaceLeaderboard;
import io.gitlab.icestom.icestom.race.RaceLeaderboardRow;
import io.gitlab.icestom.icestom.race.event.RaceLapCompletedEvent;
import io.gitlab.icestom.icestom.race.event.RaceLapTimerEvent;
import io.gitlab.icestom.icestom.race.event.RaceLeaderboardUpdateEvent;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.event.TimedLapCheckpointAdvancedEvent;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialTimedLapEndedEvent;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialLapTimerEvent;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialStartEvent;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.ui.interfaces.AbstractRaceInterface;
import io.gitlab.icestom.icestom.ui.interfaces.AbstractTimeTrialInterface;
import io.gitlab.icestom.icestom.ui.interfaces.Interface;
import io.gitlab.icestom.icestom.ui.interfaces.InterfaceProvider;
import io.gitlab.icestom.icestom.util.TextFormatter;
import io.gitlab.icestom.icestom.util.UsernameCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.object.ObjectContents;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class VanillaInterface implements InterfaceProvider {

    private static final Map<Player, Sidebar> sidebars = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <H, I extends Interface<H, I>> I getInterface(H holder) {
        return (I) switch (holder) {
            case TimeTrialingInstance instance -> new VanillaTimetrialInterface(instance);
            case RaceInstance instance -> new VanillaRaceInterface(instance);
            default -> throw new RuntimeException("Unreachable");
        };
    }

    protected static void createSidebar(Player player) {
        Sidebar sidebar = new Sidebar(Component.empty());

        sidebar.addViewer(player);

        sidebars.put(player, sidebar);
    }

    protected static void destroySidebar(Player player) {
        @Nullable Sidebar sidebar = sidebars.remove(player);

        if (sidebar != null) sidebar.removeViewer(player);
    }

    private static @NotNull Component lapCompletedMessage(Track track, TimedLapResultSource result, @Nullable TimedLapResultSource best) {
        boolean is_first = best == null;
        boolean is_full_run = track.isLastCheckpoint(result.splits().size() - 1);
        if (is_first) {
            if (is_full_run) {
                return Component.translatable("message.timetrial.complete_first",
                        Argument.component("track", track.getName()),
                        Argument.component("time", TextFormatter.getTime(result.getTime()))
                );
            } else {
                return Component.translatable("message.timetrial.complete_checkpoint_first",
                        Argument.component("track", track.getName()),
                        Argument.component("checkpoints", Component.text(result.splits().size() - 1)),
                        Argument.component("time", TextFormatter.getTime(result.getTime()))
                );
            }
        } else {
            int compareIndex = Math.min(best.splits().size(), result.splits().size()) - 1;
            long delta = result.getSplitTime(compareIndex) - best.getSplitTime(compareIndex);
            if (is_full_run) {
                return Component.translatable("message.timetrial.complete",
                        Argument.component("track", track.getName()),
                        Argument.component("time", TextFormatter.getTime(result.getTime())),
                        Argument.component("delta", TextFormatter.getDelta(delta))
                );
            } else {
                return Component.translatable("message.timetrial.complete_checkpoint",
                        Argument.component("track", track.getName()),
                        Argument.component("checkpoints", Component.text(result.splits().size() - 1)),
                        Argument.component("time", TextFormatter.getTime(result.getTime())),
                        Argument.component("delta", TextFormatter.getDelta(delta))
                );
            }
        }
    }

    @Override
    public boolean supportsPlayer(Player player) {
        return true;
    }

    public static class VanillaTimetrialInterface extends AbstractTimeTrialInterface {

        public VanillaTimetrialInterface(TimeTrialingInstance holder) {
            super(holder);

            eventNode().addListener(TimedLapCheckpointAdvancedEvent.class, event -> {
                final Player player = event.getPlayer();
                final TimedLap lap = event.getLap();
                final TimedLapResultSource result = lap.getBestPreviousResult();
                if (result != null && result.splits().size() > lap.getLastReachedCheckpoint()) {
                    player.sendMessage(Component.translatable(
                            "message.timetrail.checkpoint_pass",
                            Argument.component("checkpoint", Component.text(lap.getLastReachedCheckpoint())),
                            Argument.component("time", TextFormatter.getTime(lap.getSplitTime(lap.getLastReachedCheckpoint()))),
                            Argument.component("delta", TextFormatter.getDelta(lap.getRecentSplit()))
                    ));
                } else {
                    player.sendMessage(Component.translatable(
                            "message.timetrail.checkpoint_pass_no_delta",
                            Argument.component("checkpoint", Component.text(lap.getLastReachedCheckpoint())),
                            Argument.component("time", TextFormatter.getTime(lap.getSplitTime(lap.getLastReachedCheckpoint())))
                    ));
                }
            });

            eventNode().addListener(TimeTrialStartEvent.class, event -> {
                final Player player = event.getPlayer();
                final Track track = event.getInstance().getTrack();

                player.sendMessage(Component.translatable("message.timetrial.start", Argument.component("track", track.getName())));

                Sidebar sidebar = sidebars.get(player);

                if (sidebar != null) {
                    sidebar.updateLineContent("0", Component.empty()
                            .append(Component.text("Track: "))
                            .append(Component.text(track.getId(), NamedTextColor.GOLD)));

                    sidebar.updateLineContent("1", Component.empty()
                            .append(Component.text("Checkpoints: "))
                            .append(Component.text(track.getCheckpoints().size(), NamedTextColor.GOLD)));
                }
            });

            eventNode().addListener(TimeTrialLapTimerEvent.class, event -> {
                final Player player = event.getPlayer();
                final TimeTrialingInstance instance = event.getInstance();

                Component actionBar = event.getLap().getActionBar(instance.getWorldAge());

                player.sendActionBar(actionBar);
            });

            eventNode().addListener(TimeTrialTimedLapEndedEvent.class, event -> {
                final Player player = event.getPlayer();
                final Track track = event.getInstance().getTrack();
                final TimedLapResultSource result = event.getResult();
                final @Nullable TimedLapResultSource best = event.getPreviousBest();

                player.sendMessage(lapCompletedMessage(
                        track,
                        result,
                        best
                ));
            });
        }

        @Override
        public void startWatching(Player player) {
            createSidebar(player);
        }

        @Override
        public void stopWatching(Player player) {
            destroySidebar(player);
        }

        @Override
        public Set<Player> getWatching() {
            return getHolder().getPlayers();
        }
    }

    public static class VanillaRaceInterface extends AbstractRaceInterface {

        public VanillaRaceInterface(RaceInstance holder) {
            super(holder);

            eventNode().addListener(RaceLeaderboardUpdateEvent.class, event -> {
                final RaceInstance raceInstance = event.getInstance();
                final Track track = raceInstance.getTrack();

                final List<RaceLeaderboardRow> rows = raceInstance.getLeaderboard().getSnapshot().getRows();

                for (Player player : raceInstance.getPlayers()) {
                    @Nullable Sidebar sidebar = sidebars.get(player);

                    if (sidebar == null) continue;

                    sidebar.setTitle(Component.text("Race at ").append(track.getName()));

                    for (int i = 0; i < rows.size(); i++) {

                        if (sidebar.getLine(String.valueOf(i)) == null) {
                            sidebar.createLine(new Sidebar.ScoreboardLine(
                                    String.valueOf(i),
                                    Component.empty(),
                                    i,
                                    Sidebar.NumberFormat.blank()
                            ));
                        }

                        RaceLeaderboardRow row = rows.get(i);

                        sidebar.updateLineContent(String.valueOf(i), sidebarLeaderboardEntry(row));
                    }
                }
            });

            eventNode().addListener(RaceLapTimerEvent.class, event -> {
                final RaceInstance.RaceParticipant participant = event.getParticipant();
                final RaceInstance instance = event.getInstance();
                final RaceLeaderboard<RaceLeaderboardRow> leaderboard = instance.getLeaderboard();
                long worldAge = instance.getWorldAge();

                TimedLap lap = participant.getCurrentLap();

                int pos = leaderboard.getSnapshot().getPosition(participant) + 1;

                TextComponent.Builder text = Component.text();

                text.append(Component.text("P")
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(pos, TextColor.color(0x4fd3ff))));

                text.append(Component.text(" "));
                text.append(Component.text(Math.max(0, participant.getCompletedLaps()) + "/" + instance.getTotalLaps()));
                text.append(Component.text(" "));
                text.append(TextFormatter.getTimeRounded(lap.getCurrentTime(worldAge)).color(NamedTextColor.YELLOW));

                if (participant.getCompletedLaps() > 0) {
                    text.append(Component.text(" - "));
                    text.append(TextFormatter.getDelta(lap.getRecentSplit()));
                }

                participant.getCurrentPlayer().sendActionBar(text);
            });

            eventNode().addListener(RaceLapCompletedEvent.class, event -> {
                final Player player = event.getPlayer();
                final Track track = event.getInstance().getTrack();
                final TimedLapResultSource result = event.getResult();
                @Nullable final TimedLapResultSource best = event.getPreviousBestResult();

                player.sendMessage(lapCompletedMessage(
                        track,
                        result,
                        best
                ));
            });
        }

        private Component sidebarLeaderboardEntry(RaceLeaderboardRow row) {
            UUID id = row.getParticipant().getCurrentPlayer().getUuid();

            String username = UsernameCache.getUsernameCached(id);

            return Component.object(ObjectContents.playerHead(id))
                    .append(Component.text(" "))
                    .append(TextFormatter.getDelta(row.getDelta()))
                    .append(Component.text(" " + username));
        }

        @Override
        public void startWatching(Player player) {
            createSidebar(player);
        }

        @Override
        public void stopWatching(Player player) {
            destroySidebar(player);
        }

        @Override
        public Set<Player> getWatching() {
            return getHolder().getPlayers();
        }
    }
}
