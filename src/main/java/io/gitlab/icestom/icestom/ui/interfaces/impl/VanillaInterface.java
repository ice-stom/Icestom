package io.gitlab.icestom.icestom.ui.interfaces.impl;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.ordering.ForwardOrdering;
import io.gitlab.icestom.icestom.ordering.Tower;
import io.gitlab.icestom.icestom.ordering.TowerHolder;
import io.gitlab.icestom.icestom.race.RaceTimingTower;
import io.gitlab.icestom.icestom.race.event.*;
import io.gitlab.icestom.icestom.race.RaceStage;
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
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.title.Title;
import net.minestom.server.adventure.AdventurePacketConvertor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.utils.PacketSendingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VanillaInterface implements InterfaceProvider {

    private static final Logger log = LoggerFactory.getLogger(VanillaInterface.class);

    private static final BossBarPacket.Action addEmptyBossbar = new BossBarPacket.AddAction(Component.empty(), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, AdventurePacketConvertor.getBossBarFlagValue(List.of()));

    private static final Sound PING = Sound.sound(
            Key.key("entity.experience_orb.pickup"),
            Sound.Source.MASTER,
            1f,
            1f
    );

    private static final Sound CHALLENGE_COMPLETE = Sound.sound(
            Key.key("ui.toast.challenge_complete"),
            Sound.Source.MASTER,
            0.7f,
            1f
    );

    private static final Sound COUNTDOWN_END = Sound.sound(
            Key.key("block.note_block.bit"),
            Sound.Source.MASTER,
            1f,
            2f
    );

    private static final Sound COUNTDOWN = Sound.sound(
            Key.key("block.note_block.bass"),
            Sound.Source.MASTER,
            0.5f,
            1f
    );

    private static final Sound TRACK_FIRST_COMPLETION = Sound.sound(
            Key.key("ui.toast.challenge_complete"),
            Sound.Source.MASTER,
            1f,
            1f
    );

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

    protected static @NotNull Component lapCompletedMessage(Track track, TimedLapResultSource result, @Nullable TimedLapResultSource best) {
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

    public static class VanillaGeneralInterface extends AbstractGeneralInterface {

        @Override
        public boolean supportsPlayer(Player player) {
            return true;
        }

        public VanillaGeneralInterface(IceStom holder) {
            super(holder);

            eventNode().addListener(GenericMessageEvent.class, this::genericMessageEvent);
            eventNode().addListener(GenericErrorMessageEvent.class, this::genericErrorMessageEvent);
        }

        protected void genericErrorMessageEvent(GenericErrorMessageEvent event) {
            final Player player = event.getPlayer();

            player.sendMessage(event.getComponent());
        }

        protected void genericMessageEvent(GenericMessageEvent event) {
            final Player player = event.getPlayer();

            player.sendMessage(event.getComponent());
        }
    }

    public static class VanillaTimetrialInterface extends AbstractTimeTrialInterface {

        @Override
        public boolean supportsPlayer(Player player) {
            return true;
        }

        public VanillaTimetrialInterface(TimeTrialingInstance holder) {
            super(holder);

            eventNode().addListener(TimedLapCheckpointAdvancedEvent.class, this::timedLapCheckpointAdvancedEvent);

            eventNode().addListener(TimeTrialStartEvent.class, this::timeTrialStartEvent);
            eventNode().addListener(TimeTrialLapTimerEvent.class, this::TimeTrialLapTimerEvent);
            eventNode().addListener(TimeTrialTimedLapEndedEvent.class, this::timeTrialTimedLapEndedEvent);

            eventNode().addListener(TimeTrialPracticePointCreateEvent.class, this::timeTrialPracticePointCreateEvent);
            eventNode().addListener(TimeTrialPracticePointDeleteEvent.class, this::timeTrialPracticePointCreateEvent);

            eventNode().addListener(TimeTrialNewRecordEvent.class, this::timeTrialNewRecordEvent);
        }

        protected void timeTrialTimedLapEndedEvent(TimeTrialTimedLapEndedEvent event) {
            final Player player = event.getPlayer();
            final Track track = event.getInstance().getTrack();
            final TimedLapResultSource result = event.getResult();
            final @Nullable TimedLapResultSource best = event.getLap().getBestPreviousResult();

            player.sendMessage(lapCompletedMessage(
                    track,
                    result,
                    best
            ));
            if (result.isBetterThan(best)) {
                player.playSound(PING, Sound.Emitter.self());
            }
        }

        protected void TimeTrialLapTimerEvent(TimeTrialLapTimerEvent event) {
            final Player player = event.getPlayer();
            final TimeTrialingInstance instance = event.getInstance();

            long tick = instance.getPlayerTick(player.getUuid());

            Component actionBar = event.getLap().getActionBar(tick);

            player.sendActionBar(actionBar);
        }

        protected void timeTrialStartEvent(TimeTrialStartEvent event) {
            final Player player = event.getPlayer();
            final Track track = event.getInstance().getTrack();

            player.sendMessage(Component.translatable("message.timetrial.start", Argument.component("track", track.getName())));
        }

        protected void timeTrialNewRecordEvent(TimeTrialNewRecordEvent event) {
            final Player player = event.getPlayer();
            final Track track = event.getInstance().getTrack();
            final TimedLapResultSource result = event.getResult();
            final TimedLapResultSource oldResult = event.getOldResult();

            player.sendMessage(Component.translatable(
                    "message.timetrial.get_record",
                    Argument.component("player", player.getName()),
                    Argument.component("track", track.getName()),
                    Argument.component("time", TextFormatter.getTime(result.getTime())),
                    Argument.component("delta", TextFormatter.getDelta(result.getTime() - oldResult.getTime())),
                    Argument.component("oldtime", TextFormatter.getTime(oldResult.getTime()))
            ));
        }

        protected void timedLapCheckpointAdvancedEvent(TimedLapCheckpointAdvancedEvent event) {
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
        }

        protected void timeTrialPracticePointCreateEvent(TimeTrialPracticePointCreateEvent event) {
            final Player player = event.getPlayer();

            player.sendMessage(Component.translatable("message.timetrial.practicepoint_place"));
        }

        protected void timeTrialPracticePointCreateEvent(TimeTrialPracticePointDeleteEvent event) {
            final Player player = event.getPlayer();

            player.sendMessage(Component.translatable("message.timetrial.practicepoint_remove"));
        }
    }

    // don't look at the typedef too hard
    public static abstract class TowerInterface<R extends ForwardOrdering.Orderable<R>, H extends TowerHolder<R>> extends Interface<H, TowerInterface<R, H>> {

        private static final String OBJECTIVE = "tower";

        private static final Sidebar.NumberFormat NO_NUMBERS = Sidebar.NumberFormat.blank();

        private static final ScoreboardObjectivePacket CREATE_OBJECTIVE = new ScoreboardObjectivePacket(
                OBJECTIVE,
                (byte) 0, // create
                Component.text("Tower"),
                ScoreboardObjectivePacket.Type.INTEGER,
                NO_NUMBERS
        );

        private static final DisplayScoreboardPacket SHOW_SIDEBAR = new DisplayScoreboardPacket(
                (byte) 1, // sidebar
                OBJECTIVE
        );

        private static final ScoreboardObjectivePacket DESTROY_OBJECTIVE = new ScoreboardObjectivePacket(
                OBJECTIVE,
                (byte) 1, // destroy
                null,
                null,
                null
        );

        private record CachedRow(Object[] values, Component rendered) {}

        private final H holder;
        private final Tower<R> tower;

        private final List<CachedRow> rows = new ArrayList<>();

        public TowerInterface(H holder) {
            this.holder = holder;
            this.tower = holder.getTower();
        }

        protected abstract Component render(Key column, Object value);

        public void update() {
            int limit = tower.size();
            List<Tower.TowerColumn<R, ?>> columns = tower.getColumns();

            int i = 0;
            for (ForwardOrdering<R>.Entry entry : tower) {
                if (i >= limit) break;

                Object[] values = tower.getRow(entry);

                if (i < rows.size() && Arrays.equals(rows.get(i).values(), values)) {
                    i++;
                    continue;
                }

                CachedRow row = new CachedRow(values.clone(), renderRow(columns, values));

                if (i < rows.size()) rows.set(i, row);
                else rows.add(row);

                PacketSendingUtils.sendGroupedPacket(getWatching(), scorePacket(i, row));
                i++;
            }

            removeLinesFrom(i);
        }

        private Component renderRow(List<Tower.TowerColumn<R, ?>> columns, Object[] values) {
            TextComponent.Builder builder = Component.text();

            for (int c = 0; c < columns.size(); c++) {
                if (c > 0) builder.append(Component.space());
                builder.append(render(columns.get(c).identifier(), values[c]));
            }

            return builder.build();
        }

        private void removeLinesFrom(int count) {
            if (rows.size() <= count) return;

            var viewers = getWatching();
            for (int i = count; i < rows.size(); i++) {
                PacketSendingUtils.sendGroupedPacket(viewers, new ResetScorePacket(lineName(i), OBJECTIVE));
            }

            rows.subList(count, rows.size()).clear();
        }

        private static String lineName(int index) {
            return "tower_" + index;
        }

        private static UpdateScorePacket scorePacket(int index, CachedRow row) {
            return new UpdateScorePacket(
                    lineName(index),
                    OBJECTIVE,
                    -index,
                    row.rendered(),
                    NO_NUMBERS
            );
        }

        @Override
        public H getHolder() {
            return holder;
        }

        @Override
        public boolean supportsPlayer(Player player) {
            return true;
        }

        @Override
        public void startWatching(Player player) {
            update();

            player.sendPacket(CREATE_OBJECTIVE);
            player.sendPacket(SHOW_SIDEBAR);

            for (int i = 0; i < rows.size(); i++) {
                player.sendPacket(scorePacket(i, rows.get(i)));
            }

            super.startWatching(player);
        }

        @Override
        public void stopWatching(Player player) {
            player.sendPacket(DESTROY_OBJECTIVE);

            super.stopWatching(player);
        }
    }

    public static class RaceTowerInterface extends TowerInterface<RaceStage.RaceParticipant, RaceStage> {
        public RaceTowerInterface(RaceStage holder) {
            super(holder);
        }

        @Override
        protected Component render(Key column, Object value) {
            return switch (column.toString()) {
                case "icestom:name" -> (Component) value;
                case "icestom:delta" -> value != null ? TextFormatter.getDelta((Long) value) : Component.text("-", NamedTextColor.YELLOW);
                default -> Component.text(value.toString());
            };
        }
    }

    public static class VanillaRaceInterface extends AbstractRaceInterface {

        private final Map<TickCountdown, UUID> countdowns = new HashMap<>();
        private final Map<TickCountdown, Set<Player>> countdownViewers = new HashMap<>();

        private final Map<TickCountdown, String> translations;

        private final RaceTowerInterface towerInterface;

        @Override
        public boolean supportsPlayer(Player player) {
            return true;
        }

        public VanillaRaceInterface(RaceStage holder) {
            super(holder);

            towerInterface = new RaceTowerInterface(holder);

            translations = Map.of(
                    holder.getChequeredFlagCountdown(), "race.bossbar.chequered_flag_countdown"
            );

            translations.keySet().forEach((tickCountdown) -> {
                countdowns.put(
                        tickCountdown,
                        UUID.randomUUID()
                );

                countdownViewers.put(tickCountdown, new HashSet<>());
            });

            eventNode().addListener(InstanceTickEvent.class, this::instanceTickEvent);
            eventNode().addListener(RaceLeaderboardUpdateEvent.class, this::raceLeaderboardUpdateEvent);
            eventNode().addListener(RaceLapTimerEvent.class, this::raceLapTimerEvent);
            eventNode().addListener(RaceTimedLapCompletedEvent.class, this::raceTimedLapCompletedEvent);
            eventNode().addListener(RaceCompletedEvent.class, this::raceCompletedEvent);
        }

        protected void raceCompletedEvent(RaceCompletedEvent event) {
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

            player.playSound(CHALLENGE_COMPLETE, Sound.Emitter.self());
        }

        protected void raceTimedLapCompletedEvent(RaceTimedLapCompletedEvent event) {
            final Player player = event.getPlayer();
            final Track track = event.getInstance().getTrack();
            final TimedLapResultSource result = event.getResult();
            @Nullable final TimedLapResultSource best = event.getLap().getBestPreviousResult();

            player.sendMessage(lapCompletedMessage(
                    track,
                    result,
                    best
            ));
        }

        protected void raceLapTimerEvent(RaceLapTimerEvent event) {
            final EventParticipant participant = event.getParticipant();
            final RaceStage.RaceParticipant racer = event.getRacer();
            final @NotNull RaceStage instance = event.getInstance();

            final RaceTimingTower tower = instance.getTower();

            long tick = instance.getWorldAge();

            TimedLap lap = racer.getCurrentLap();

            int pos = tower.indexOf(racer.getTowerEntry());

            TextComponent.Builder text = Component.text();

            text.append(Component.text("P")
                    .decorate(TextDecoration.BOLD)
                    .append(Component.text(pos, TextColor.color(0x4fd3ff))));

            text.append(Component.text(" "));
            text.append(Component.text(Math.max(0, racer.getCompletedLapCount()) + "/" + instance.getTotalLaps()));
            text.append(Component.text(" "));
            text.append(TextFormatter.getTimeRounded(lap.getCurrentTime(tick)).color(NamedTextColor.YELLOW));

            if (racer.getCompletedLapCount() > 0) {
                text.append(Component.text(" - "));
                text.append(TextFormatter.getDelta(lap.getRecentSplit()));
            }

            participant.getCurrentPlayer().sendActionBar(text);
        }

        protected void raceLeaderboardUpdateEvent(RaceLeaderboardUpdateEvent event) {
            towerInterface.update();
        }

        protected void instanceTickEvent(InstanceTickEvent event) {
            int remainingTicks = ((RaceStage) (event.getInstance())).getStartingCountdown().getRemainingTicks();

            if (remainingTicks % 20 == 0) {
                for (Player player : getWatching()) {
                    if (remainingTicks != 0) {
                        player.showTitle(Title.title(
                                Component.text(remainingTicks / 20),
                                Component.empty(),
                                0, 20, 0
                        ));

                        player.playSound(COUNTDOWN, Sound.Emitter.self());
                    }
                }
            }

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
        }

        @Override
        public void startWatching(Player player) {
            super.startWatching(player);
            if (towerInterface.supportsPlayer(player)) towerInterface.startWatching(player);
        }

        @Override
        public void stopWatching(Player player) {
            super.stopWatching(player);
            if (towerInterface.supportsPlayer(player)) towerInterface.stopWatching(player);

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
