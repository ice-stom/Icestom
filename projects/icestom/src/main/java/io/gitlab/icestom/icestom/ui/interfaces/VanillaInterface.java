package io.gitlab.icestom.icestom.ui.interfaces;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.ordering.ForwardOrdering;
import io.gitlab.icestom.icestom.ordering.Tower;
import io.gitlab.icestom.icestom.ordering.TowerHolder;
import io.gitlab.icestom.icestom.stages.qualifying.QualifyingStage;
import io.gitlab.icestom.icestom.stages.qualifying.QualifyingTimingTower;
import io.gitlab.icestom.icestom.stages.race.RaceTimingTower;
import io.gitlab.icestom.icestom.stages.race.RaceStage;
import io.gitlab.icestom.icestom.stages.race.event.RaceCompletedEvent;
import io.gitlab.icestom.icestom.stages.race.event.RaceLapTimerEvent;
import io.gitlab.icestom.icestom.stages.race.event.RaceLeaderboardUpdateEvent;
import io.gitlab.icestom.icestom.stages.race.event.RaceTimedLapCompletedEvent;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.event.*;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.ui.TickCountdown;
import io.gitlab.icestom.icestom.ui.event.GenericErrorMessageEvent;
import io.gitlab.icestom.icestom.ui.event.GenericMessageEvent;
import io.gitlab.icestom.icestom.ui.event.GenericWarningMessageEvent;
import io.gitlab.icestom.icestom.ui.event.MessageEvent;
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

    @Override
    public <H, I extends Interface<H, I>> I getInterface(H holder) {
        try {
            return (I) switch (holder) {
                case QualifyingStage instance -> new VanillaQualifyingInterface(instance);
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

    protected static void showStartingCountdown(TickCountdown countdown, Set<Player> players) {
        int remainingTicks = countdown.getRemainingTicks();

        if (remainingTicks == 0 || remainingTicks % 20 != 0) return;

        for (Player player : players) {
            player.showTitle(Title.title(
                    Component.text(remainingTicks / 20),
                    Component.empty(),
                    0, 20, 0
            ));

            player.playSound(COUNTDOWN, Sound.Emitter.self());
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
            eventNode().addListener(GenericWarningMessageEvent.class, this::genericMessageEvent);
            eventNode().addListener(GenericErrorMessageEvent.class, this::genericMessageEvent);
        }

        protected void genericMessageEvent(MessageEvent event) {
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

            eventNode().addListener(TimeTrialCheckpointAdvancedEvent.class, this::timedLapCheckpointAdvancedEvent);

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
            final @Nullable TimedLapResultSource best = event.getPreviousBest();

            boolean is_full_run = track.isLastCheckpoint(result.splits().size() - 1);

            if (is_full_run) {
                if (best == null || result.splits().size() > best.splits().size()) {
                    player.sendMessage(Component.translatable(
                            "message.timetrial.record_first_completion",
                            Argument.component("player", player.getName()),
                            Argument.component("track", track.getName()),
                            Argument.component("time", TextFormatter.getTime(result.getTime()))
                    ));
                } else {
                    player.sendMessage(Component.translatable(
                            "message.timetrial.record",
                            Argument.component("player", player.getName()),
                            Argument.component("track", track.getName()),
                            Argument.component("time", TextFormatter.getTime(result.getTime())),
                            Argument.component("delta", TextFormatter.getDelta(result.getTime() - best.getTime())),
                            Argument.component("old_time", TextFormatter.getTime(best.getTime()))
                    ));
                }
            } else {
                if (best == null || result.splits().size() > best.splits().size()) {
                    player.sendMessage(Component.translatable(
                            "message.timetrial.record_first_checkpoint",
                            Argument.component("player", player.getName()),
                            Argument.component("track", track.getName()),
                            Argument.component("checkpoints", Component.text(result.splits().size() - 1)),
                            Argument.component("time", TextFormatter.getTime(result.getTime()))
                    ));
                } else {
                    player.sendMessage(Component.translatable(
                            "message.timetrial.record_checkpoint_improvement",
                            Argument.component("player", player.getName()),
                            Argument.component("track", track.getName()),
                            Argument.component("checkpoints", Component.text(result.splits().size() - 1)),
                            Argument.component("time", TextFormatter.getTime(result.getTime())),
                            Argument.component("delta", TextFormatter.getDelta(result.getTime() - best.getTime())),
                            Argument.component("old_time", TextFormatter.getTime(best.getTime()))
                    ));
                }
            }
        }

        protected void timedLapCheckpointAdvancedEvent(TimeTrialCheckpointAdvancedEvent event) {
            final Player player = event.getPlayer();
            final TimedLap lap = event.getLap();
            final TimedLapResultSource result = lap.getBestPreviousResult();

            final int checkpoint = lap.getLastReachedCheckpoint();

            if (result != null && result.splits().size() > checkpoint && checkpoint != 0) {
                player.sendMessage(Component.translatable(
                        "message.timetrial.checkpoint_pass",
                        Argument.component("checkpoints", Component.text(checkpoint)),
                        Argument.component("time", TextFormatter.getTime(lap.getSplitTime(checkpoint))),
                        Argument.component("delta", TextFormatter.getDelta(lap.getRecentSplit()))
                ));
            } else {
                if (checkpoint != 0) {
                    player.sendMessage(Component.translatable(
                            "message.timetrial.checkpoint_pass_no_delta",
                            Argument.component("checkpoints", Component.text(checkpoint)),
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

        private static final int MAX_LINES = 15;

        private record CachedRow(Object[] values, Component rendered) {}

        protected record Relative(Object value, boolean ahead) {
            public NamedTextColor color() {
                return ahead ? NamedTextColor.RED : NamedTextColor.GREEN;
            }
        }

        private final H holder;

        private final Map<Player, List<CachedRow>> rows = new HashMap<>();

        public TowerInterface(H holder) {
            this.holder = holder;
        }

        protected abstract Component render(Key column, Object value);

        protected @Nullable R getFocus(Player viewer) {
            return null;
        }

        protected Object[] getRow(Tower<R> tower, ForwardOrdering<R>.Entry entry, @Nullable R focus) {
            return tower.getRow(entry);
        }

        public void update() {
            for (Player player : getWatching()) update(player);
        }

        protected static <R extends ForwardOrdering.Orderable<R>> int columnIndex(Tower<R> tower, Key identifier) {
            List<Tower.TowerColumn<R, ?>> columns = tower.getColumns();

            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).identifier().equals(identifier)) return i;
            }

            throw new IllegalArgumentException("Tower has no column " + identifier);
        }

        private void update(Player viewer) {
            Tower<R> tower = holder.getTower();
            List<Tower.TowerColumn<R, ?>> columns = tower.getColumns();
            List<CachedRow> viewerRows = rows.computeIfAbsent(viewer, _ -> new ArrayList<>());

            @Nullable R focus = getFocus(viewer);

            int focusIndex = -1;
            for (ForwardOrdering<R>.Entry entry : tower) {
                if (entry.getItem() == focus) {
                    focusIndex = entry.getIndex();
                    break;
                }
            }

            int start = Math.max(0, Math.min(focusIndex - MAX_LINES / 2, tower.size() - MAX_LINES));

            int i = 0;
            for (ForwardOrdering<R>.Entry entry : tower) {
                if (entry.getIndex() < start) continue;
                if (i >= MAX_LINES) break;

                Object[] values = getRow(tower, entry, focus);

                if (i < viewerRows.size() && Arrays.equals(viewerRows.get(i).values(), values)) {
                    i++;
                    continue;
                }

                CachedRow row = new CachedRow(values.clone(), renderRow(columns, values));

                if (i < viewerRows.size()) viewerRows.set(i, row);
                else viewerRows.add(row);

                viewer.sendPacket(scorePacket(i, row));
                i++;
            }

            removeLinesFrom(viewer, viewerRows, i);
        }

        private Component renderRow(List<Tower.TowerColumn<R, ?>> columns, Object[] values) {
            TextComponent.Builder builder = Component.text();

            for (int c = 0; c < columns.size(); c++) {
                if (c > 0) builder.append(Component.space());
                builder.append(render(columns.get(c).identifier(), values[c]));
            }

            return builder.build();
        }

        private static void removeLinesFrom(Player viewer, List<CachedRow> viewerRows, int count) {
            if (viewerRows.size() <= count) return;

            for (int i = count; i < viewerRows.size(); i++) {
                viewer.sendPacket(new ResetScorePacket(lineName(i), OBJECTIVE));
            }

            viewerRows.subList(count, viewerRows.size()).clear();
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
            player.sendPacket(CREATE_OBJECTIVE);
            player.sendPacket(SHOW_SIDEBAR);

            super.startWatching(player);

            rows.remove(player);
            update(player);
        }

        @Override
        public void stopWatching(Player player) {
            player.sendPacket(DESTROY_OBJECTIVE);
            rows.remove(player);

            super.stopWatching(player);
        }
    }

    public static class RaceTowerInterface extends TowerInterface<RaceStage.RaceParticipant, RaceStage> {
        public RaceTowerInterface(RaceStage holder) {
            super(holder);
        }

        @Override
        protected RaceStage.@Nullable RaceParticipant getFocus(Player viewer) {
            @Nullable EventParticipant participant = getHolder().getParticipants().getParticipantFromActivePlayer(viewer);

            return participant == null ? null : getHolder().getRacer(participant.getUuid());
        }

        @Override
        protected Object[] getRow(Tower<RaceStage.RaceParticipant> tower, ForwardOrdering<RaceStage.RaceParticipant>.Entry entry, RaceStage.@Nullable RaceParticipant focus) {
            Object[] row = super.getRow(tower, entry, focus);

            if (focus == null) return row;

            RaceStage.RaceParticipant racer = entry.getItem();
            @Nullable Long gap = racer == focus ? null : racer.getGapTo(focus);

            row = row.clone();
            row[columnIndex(tower, RaceTimingTower.DELTA)] = gap == null ? null : new Relative(gap, racer.betterThan(focus));

            return row;
        }

        @Override
        protected Component render(Key column, Object value) {
            return switch (column.toString()) {
                case "icestom:position" -> Component.text(String.format("%2s.", value), NamedTextColor.YELLOW);
                case "icestom:delta" -> switch (value) {
                    case null -> Component.text("   -   ", NamedTextColor.YELLOW);
                    case Relative relative when relative.value() instanceof Long gap -> Component.text(gap < 0 ? "-" : "+")
                            .append(TextFormatter.getTime(Math.abs(gap)))
                            .color(relative.color());
                    default -> TextFormatter.getDelta((Long) value);
                };
                case "icestom:name" -> (Component) value;
                default -> Component.text(value.toString());
            };
        }
    }

    public static class QualifyingTowerInterface extends TowerInterface<QualifyingStage.Qualifier, QualifyingStage> {
        public QualifyingTowerInterface(QualifyingStage holder) {
            super(holder);
        }

        @Override
        protected QualifyingStage.@Nullable Qualifier getFocus(Player viewer) {
            return getHolder().getQualifier(viewer.getUuid());
        }

        @Override
        protected Object[] getRow(Tower<QualifyingStage.Qualifier> tower, ForwardOrdering<QualifyingStage.Qualifier>.Entry entry, QualifyingStage.@Nullable Qualifier focus) {
            Object[] row = super.getRow(tower, entry, focus);

            int time = columnIndex(tower, QualifyingTimingTower.TIME);
            if (focus == null || entry.getItem() == focus || row[time] == null) return row;

            row = row.clone();
            row[time] = new Relative(row[time], entry.getItem().betterThan(focus));

            return row;
        }

        @Override
        protected Component render(Key column, Object value) {
            return switch (column.toString()) {
                case "icestom:position" -> Component.text(String.format("%2s.", value), NamedTextColor.YELLOW);
                case "icestom:time" -> switch (value) {
                    case null -> Component.text("   -   ", NamedTextColor.YELLOW);
                    case Relative relative when relative.value() instanceof Long time -> TextFormatter.getTime(time).color(relative.color());
                    default -> TextFormatter.getTime((Long) value);
                };
                case "icestom:name" -> (Component) value;
                default -> Component.text(String.valueOf(value));
            };
        }
    }

    public static class VanillaQualifyingInterface extends VanillaTimetrialInterface {

        private final QualifyingTowerInterface towerInterface;

        public VanillaQualifyingInterface(QualifyingStage holder) {
            super(holder);

            towerInterface = new QualifyingTowerInterface(holder);

            eventNode().addListener(InstanceTickEvent.class, this::instanceTickEvent);
        }

        protected void instanceTickEvent(InstanceTickEvent event) {
            final QualifyingStage instance = (QualifyingStage) event.getInstance();

            showStartingCountdown(instance.getStartingCountdown(), getWatching());

            if (instance.getState() != QualifyingStage.QualifyingState.RUNNING
                    && instance.getState() != QualifyingStage.QualifyingState.CHEQUERED_FLAG) return;

            for (Player player : getWatching()) {
                @Nullable QualifyingStage.Qualifier qualifier = instance.getQualifier(player.getUuid());

                if (qualifier != null) player.sendActionBar(qualifyingActionBar(instance, qualifier, player));
            }
        }

        private static Component qualifyingActionBar(QualifyingStage instance, QualifyingStage.Qualifier qualifier, Player player) {
            TextComponent.Builder text = Component.text();

            text.append(Component.text("P")
                    .decorate(TextDecoration.BOLD)
                    .append(Component.text(qualifier.getTowerEntry().getIndex() + 1, TextColor.color(0x4fd3ff))));

            text.append(Component.text(" "));

            @Nullable Integer lapsRemaining = qualifier.getLapsRemaining();

            if (lapsRemaining != null) {
                text.append(((IceStomPlayer) player).translate(Component.translatable("qualifying.actionbar.laps_remaining", Argument.numeric("laps", lapsRemaining))));
            } else {
                long seconds = instance.getSessionTimer().getRemainingTicks() / 20;
                text.append(Component.text(String.format("%d:%02d", seconds / 60, seconds % 60)));
            }

            @Nullable TimedLap lap = instance.getTimedLap(player.getUuid());

            if (lap != null) {
                text.append(Component.text(" "));
                text.append(TextFormatter.getTimeRounded(lap.getCurrentTime(instance.getPlayerTick(player.getUuid()))).color(NamedTextColor.YELLOW));

                if (lap.getBestPreviousResult() != null && lap.getLastReachedCheckpoint() > 0) {
                    text.append(Component.text(" - "));
                    text.append(TextFormatter.getDelta(lap.getRecentSplit()));
                }
            }

            return text.build();
        }

        @Override
        protected void TimeTrialLapTimerEvent(TimeTrialLapTimerEvent event) {
            // the qualifying action bar is sent every tick instead
        }

        @Override
        protected void timeTrialTimedLapEndedEvent(TimeTrialTimedLapEndedEvent event) {
            super.timeTrialTimedLapEndedEvent(event);

            towerInterface.update();
        }

        @Override
        public void startWatching(Player player) {
            super.startWatching(player);
            towerInterface.startWatching(player);
        }

        @Override
        public void stopWatching(Player player) {
            super.stopWatching(player);
            towerInterface.stopWatching(player);
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

            long tick = instance.getPlayerTick(participant.getCurrentPlayer().getUuid());

            TimedLap lap = racer.getCurrentLap();

            int pos = tower.indexOf(racer.getTowerEntry());

            TextComponent.Builder text = Component.text();

            text.append(Component.text("P")
                    .decorate(TextDecoration.BOLD)
                    .append(Component.text(pos + 1, TextColor.color(0x4fd3ff))));

            text.append(Component.text(" "));
            text.append(Component.text(racer.getCurrentLapNumber() + "/" + instance.getTotalLaps()));
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
            showStartingCountdown(((RaceStage) (event.getInstance())).getStartingCountdown(), getWatching());

            if (event.getInstance().getWorldAge() % 20 != 0) return;

            countdowns.forEach((tickCountdown, uuid) -> {
                Set<Player> viewers = countdownViewers.get(tickCountdown);
                boolean running = tickCountdown.isRunning();

                if (running) {
                    float progress = (float) tickCountdown.getRemainingTicks() / tickCountdown.getDurationTicks();

                    Component translatable = Component.translatable(
                            translations.get(tickCountdown),
                            Argument.component("time", Component.text(tickCountdown.getRemainingTicks() / 20))
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
