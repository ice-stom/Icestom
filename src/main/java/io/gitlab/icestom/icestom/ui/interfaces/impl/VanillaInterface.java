package io.gitlab.icestom.icestom.ui.interfaces.impl;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.event.EventParticipant;
import io.gitlab.icestom.icestom.race.RaceLeaderboard;
import io.gitlab.icestom.icestom.race.RaceLeaderboardRow;
import io.gitlab.icestom.icestom.race.RaceStage;
import io.gitlab.icestom.icestom.race.event.RaceCompletedEvent;
import io.gitlab.icestom.icestom.race.event.RaceLapTimerEvent;
import io.gitlab.icestom.icestom.race.event.RaceLeaderboardUpdateEvent;
import io.gitlab.icestom.icestom.race.event.RaceTimedLapCompletedEvent;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.event.*;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.ui.TickCountdown;
import io.gitlab.icestom.icestom.ui.event.GenericErrorMessageEvent;
import io.gitlab.icestom.icestom.ui.event.GenericMessageEvent;
import io.gitlab.icestom.icestom.ui.interfaces.*;
import io.gitlab.icestom.icestom.util.TextFormatter;
import io.gitlab.icestom.icestom.util.UsernameCache;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.object.ObjectContents;
import net.minestom.server.adventure.AdventurePacketConvertor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.network.packet.server.play.BossBarPacket;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.utils.PacketSendingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VanillaInterface implements InterfaceProvider {

    private static final Map<Player, Sidebar> sidebars = new HashMap<>();

    private static final Sound PING = Sound.sound(
            Key.key("entity.experience_orb.pickup"),
            Sound.Source.MASTER,
            1f,
            1f
    );
    private static final Logger log = LoggerFactory.getLogger(VanillaInterface.class);

    private static final BossBarPacket.Action addEmptyBossbar = new BossBarPacket.AddAction(Component.empty(), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, AdventurePacketConvertor.getBossBarFlagValue(List.of()));

    @Override
    public <H, I extends Interface<H, I>> I getInterface(H holder) {
        try {
            return (I) switch (holder) {
                case TimeTrialingInstance instance -> new VanillaTimetrialInterface(instance);
                case RaceStage instance -> new VanillaRaceInterface(instance);
                case IceStom instance -> new VanillaGeneralInterface(instance);
                default -> throw new RuntimeException(
                        "Unsupported Interface type: " +
                                holder.getClass().getSimpleName()
                );
            };
        } catch (Exception e) {
            log.error("getInterface failed for {}", holder, e);
            throw e;
        }
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
                return Component.translatable("message.lap.complete_first",
                        Argument.component("track", track.getName()),
                        Argument.component("time", TextFormatter.getTime(result.getTime()))
                );
            } else {
                return Component.translatable("message.lap.complete_checkpoint_first",
                        Argument.component("track", track.getName()),
                        Argument.component("checkpoints", Component.text(result.splits().size() - 1)),
                        Argument.component("time", TextFormatter.getTime(result.getTime()))
                );
            }
        } else {
            int compareIndex = Math.min(best.splits().size(), result.splits().size()) - 1;
            long delta = result.getSplitTime(compareIndex) - best.getSplitTime(compareIndex);
            if (is_full_run) {
                return Component.translatable("message.lap.complete",
                        Argument.component("track", track.getName()),
                        Argument.component("time", TextFormatter.getTime(result.getTime())),
                        Argument.component("delta", TextFormatter.getDelta(delta))
                );
            } else {
                return Component.translatable("message.lap.complete_checkpoint",
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

    public static class VanillaGeneralInterface extends AbstractGeneralInterface {
        public VanillaGeneralInterface(IceStom holder) {
            super(holder);

            eventNode().addListener(GenericMessageEvent.class, event -> {
                final Player player = event.getPlayer();

                player.sendMessage(event.getComponent());
            });

            eventNode().addListener(GenericErrorMessageEvent.class, event -> {
                final Player player = event.getPlayer();

                player.sendMessage(event.getComponent());
            });
        }
    }

    public static class VanillaTimetrialInterface extends AbstractTimeTrialInterface {

        public VanillaTimetrialInterface(TimeTrialingInstance holder) {
            super(holder);

            eventNode().addListener(TimedLapCheckpointAdvancedEvent.class, event -> {
                final Player player = event.getPlayer();
                final TimedLap lap = event.getLap();
                final TimedLapResultSource result = lap.getBestPreviousResult();

                final int checkpoint = lap.getLastReachedCheckpoint();

                if (result != null && result.splits().size() > checkpoint && checkpoint != 0) {
                    player.sendMessage(Component.translatable(
                            "message.timetrial.checkpoint_pass",
                            Argument.component("checkpoint", Component.text(checkpoint)),
                            Argument.component("time", TextFormatter.getTime(lap.getSplitTime(checkpoint))),
                            Argument.component("delta", TextFormatter.getDelta(lap.getRecentSplit()))
                    ));
                } else {
                    if (checkpoint != 0) {
                        player.sendMessage(Component.translatable(
                                "message.timetrial.checkpoint_pass_no_delta",
                                Argument.component("checkpoint", Component.text(checkpoint)),
                                Argument.component("time", TextFormatter.getTime(lap.getSplitTime(checkpoint)))

                        ));
                    }
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
                final @Nullable TimedLapResultSource best = event.getLap().getBestPreviousResult();

                player.playSound(PING);

                player.sendMessage(lapCompletedMessage(
                        track,
                        result,
                        best
                ));
            });

            eventNode().addListener(TimeTrialPracticePointCreateEvent.class, event -> {
                final Player player = event.getPlayer();

                player.sendMessage(Component.translatable("message.timetrial.practicepoint_place"));
            });

            eventNode().addListener(TimeTrialPracticePointDeleteEvent.class, event -> {
                final Player player = event.getPlayer();

                player.sendMessage(Component.translatable("message.timetrial.practicepoint_remove"));
            });
        }

        @Override
        public void startWatching(Player player) {
            super.startWatching(player);
            createSidebar(player);
        }

        @Override
        public void stopWatching(Player player) {
            super.stopWatching(player);
            destroySidebar(player);
        }
    }

    public static class VanillaRaceInterface extends AbstractRaceInterface {

        private final Map<TickCountdown, UUID> countdowns = new HashMap<>();
        private final Map<TickCountdown, Set<Player>> countdownViewers = new HashMap<>();

        public VanillaRaceInterface(RaceStage holder) {
            super(holder);

            Map<TickCountdown, String> translations = Map.of(
                    holder.getStartingCountdown(), "race.bossbar.starting_countdown",
                    holder.getChequeredFlagCountdown(), "race.bossbar.chequered_flag_countdown"
            );

            translations.keySet().forEach((tickCountdown) -> {
                countdowns.put(
                        tickCountdown,
                        UUID.randomUUID()
                );

                countdownViewers.put(tickCountdown, new HashSet<>());
            });

            eventNode().addListener(InstanceTickEvent.class, event -> {
                if (event.getInstance().getWorldAge() % 20 != 0) return;

                countdowns.forEach((tickCountdown, uuid) -> {
                    Set<Player> viewers = countdownViewers.get(tickCountdown);
                    boolean running = tickCountdown.isRunning();

                    if (running) {
                        float progress = (float) tickCountdown.getRemainingTicks() / tickCountdown.getDurationTicks();

                        Component translatable = Component.translatable(
                                translations.get(tickCountdown),
                                Argument.component("time", Component.text(Math.floor((double) tickCountdown.getRemainingTicks() / 20)))
                        );

                        for (Player player : getWatching()) {
                            if (viewers.add(player)) {
                                player.sendPacket(new BossBarPacket(
                                        uuid,
                                        addEmptyBossbar
                                ));
                            }

                            player.sendPacket(new BossBarPacket(
                                    uuid,
                                    new BossBarPacket.UpdateTitleAction(((IceStomPlayer) player).translate(translatable))
                            ));
                        }

                        PacketSendingUtils.sendGroupedPacket(viewers, new BossBarPacket(
                                uuid,
                                new BossBarPacket.UpdateHealthAction(progress)
                        ));
                    } else if (!viewers.isEmpty()) {
                        PacketSendingUtils.sendGroupedPacket(viewers, new BossBarPacket(
                                uuid,
                                new BossBarPacket.RemoveAction()
                        ));

                        viewers.clear();
                    }
                });
            });

            eventNode().addListener(RaceLeaderboardUpdateEvent.class, event -> {
                final @NotNull RaceStage raceStage = event.getInstance();
                final Track track = raceStage.getTrack();

                final List<RaceLeaderboardRow> rows = raceStage.getRaceLeaderboard().getSnapshot().getRows();

                for (Player player : raceStage.getPlayers()) {
                    @Nullable Sidebar sidebar = sidebars.get(player);

                    if (sidebar == null) continue;

                    sidebar.setTitle(Component.text("Race at ").append(track.getName()));

                    for (int i = 0; i < rows.size(); i++) {
                        if (sidebar.getLine(String.valueOf(i)) == null) {
                            sidebar.createLine(new Sidebar.ScoreboardLine(
                                    String.valueOf(i),
                                    Component.empty(),
                                    -i,
                                    Sidebar.NumberFormat.blank()
                            ));
                        }

                        RaceLeaderboardRow row = rows.get(i);

                        EventParticipant participant = raceStage.getParticipants().getParticipantFromId(raceStage.getParticipantId(
                                row.getParticipant()
                        ));

                        sidebar.updateLineContent(String.valueOf(i), sidebarLeaderboardEntry(row, participant));
                    }
                }
            });

            eventNode().addListener(RaceLapTimerEvent.class, event -> {
                final EventParticipant participant = event.getParticipant();
                final RaceStage.RaceParticipant racer = event.getRacer();
                final @NotNull RaceStage instance = event.getInstance();
                final RaceLeaderboard<RaceLeaderboardRow> leaderboard = instance.getRaceLeaderboard();
                long worldAge = instance.getWorldAge();

                TimedLap lap = racer.getCurrentLap();

                int pos = leaderboard.getSnapshot().getPosition(racer) + 1;

                TextComponent.Builder text = Component.text();

                text.append(Component.text("P")
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(pos, TextColor.color(0x4fd3ff))));

                text.append(Component.text(" "));
                text.append(Component.text(Math.max(0, racer.getCompletedLapCount()) + "/" + instance.getTotalLaps()));
                text.append(Component.text(" "));
                text.append(TextFormatter.getTimeRounded(lap.getCurrentTime(worldAge)).color(NamedTextColor.YELLOW));

                if (racer.getCompletedLapCount() > 0) {
                    text.append(Component.text(" - "));
                    text.append(TextFormatter.getDelta(lap.getRecentSplit()));
                }

                participant.getCurrentPlayer().sendActionBar(text);
            });

            eventNode().addListener(RaceTimedLapCompletedEvent.class, event -> {
                final Player player = event.getPlayer();
                final Track track = event.getInstance().getTrack();
                final TimedLapResultSource result = event.getResult();
                @Nullable final TimedLapResultSource best = event.getLap().getBestPreviousResult();

                player.sendMessage(lapCompletedMessage(
                        track,
                        result,
                        best
                ));
            });

            eventNode().addListener(RaceCompletedEvent.class, event -> {
                final Player player = event.getPlayer();
                final Track track = event.getInstance().getTrack();
                final RaceStage.RaceParticipant racer = event.getRacer();

                long time = racer.getCompletedLaps().stream().map(TimedLapResultSource::getTime).reduce(0L, Long::sum);

                player.sendMessage(Component.translatable(
                        "message.race.completed",
                        Argument.component("laps", Component.text(racer.getCompletedLapCount())),
                        Argument.component("track", track.getName()),
                        Argument.component("time", TextFormatter.getTime(time))
                ));
            });
        }

        private Component sidebarLeaderboardEntry(RaceLeaderboardRow row, EventParticipant eventParticipant) {
            UUID id = eventParticipant.getCurrentPlayer().getUuid();

            String username = UsernameCache.getUsernameCached(id);

            return Component.object(ObjectContents.playerHead(id))
                    .append(Component.text(" "))
                    .append(TextFormatter.getDelta(row.getDelta()))
                    .append(Component.text(" " + username));
        }

        @Override
        public void startWatching(Player player) {
            super.startWatching(player);
            createSidebar(player);
        }

        @Override
        public void stopWatching(Player player) {
            super.stopWatching(player);
            destroySidebar(player);

            countdownViewers.forEach((countdown, players) -> {
                if (players.remove(player)) {
                    player.sendPacket(new BossBarPacket(
                            countdowns.get(countdown),
                            new BossBarPacket.RemoveAction()
                    ));
                }
            });
        }
    }
}
