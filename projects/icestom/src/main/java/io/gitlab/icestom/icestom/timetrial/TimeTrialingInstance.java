package io.gitlab.icestom.icestom.timetrial;

import com.google.protobuf.ByteString;
import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.command.BoatCommand;
import io.gitlab.icestom.icestom.command.LonelyCommand;
import io.gitlab.icestom.icestom.database.TimetrialDatabase;
import io.gitlab.icestom.icestom.database.preference.PreferenceKey;
import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.entity.TimetrialLeaderboard;
import io.gitlab.icestom.icestom.instance.BoatedTrackInstance;
import io.gitlab.icestom.icestom.instance.SpawnLocation;
import io.gitlab.icestom.icestom.timetrial.event.*;
import io.gitlab.icestom.icestom.timetrial.lap.*;
import io.gitlab.icestom.icestom.track.PlayerMovement;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import io.gitlab.icestom.icestom.ui.event.GenericErrorMessageEvent;
import io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager;
import io.gitlab.icestom.linefold.LZ4SinglePositionWriter;
import io.gitlab.icestom.protocol.ISChannel;
import io.gitlab.icestom.protocol.packets.timetrial.ClientboundTimetrial;
import io.gitlab.icestom.protocol.packets.timetrial.clientbound.ServerPBLine;
import io.gitlab.icestom.tracklua.userdata.LuaEventProxy;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerGameModeRequestEvent;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static io.gitlab.icestom.icestom.IceStom.RECORDINGS_PATH;
import static io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager.getHolder;

@SuppressWarnings("UnstableApiUsage")
public class TimeTrialingInstance extends BoatedTrackInstance implements SpawnLocation {

    private static final Logger log = LoggerFactory.getLogger(TimeTrialingInstance.class);

    private static final Key MOD_FEATURE_TIMETRIAL = Key.key(IceStom.NAMESPACE, "timetrial");

    public static final PreferenceKey<Boolean> LONELY = IceStom.getInstance().getPreferenceRegistry().register(new PreferenceKey<>(
            Key.key(IceStom.NAMESPACE, "lonely"),
            Boolean.class,
            false
    ));

    private static final ByteString linefoldEmpty;

    static {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            new LZ4SinglePositionWriter(os).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        linefoldEmpty = ByteString.copyFrom(os.toByteArray());
    }

    private final InterfaceManager.InterfaceHolder interfaceHolder;

    private final Map<UUID, TimedLap> timeTrials = new HashMap<>();
    private final Map<UUID, Recording> recordings = new HashMap<>();

    private final Map<UUID, Pos> practicePoints = new HashMap<>();

    private final ItemStack BOAT_ITEM = ItemStack.of(Material.OAK_BOAT)
            .withCustomName(Component.text("Boat", NamedTextColor.GREEN).style(Style.style().decoration(TextDecoration.ITALIC, false)));

    private final ItemStack PRACTICE_ITEM = ItemStack.of(Material.APPLE)
            .withCustomName(Component.text("Practice Point", NamedTextColor.YELLOW).style(Style.style().decoration(TextDecoration.ITALIC, false)));

    private final ItemStack SPAWN_ITEM = ItemStack.of(Material.RED_BED)
            .withCustomName(Component.text("Return to spawn", NamedTextColor.RED).style(Style.style().decoration(TextDecoration.ITALIC, false)));

    private final ItemStack SHOW_PLAYERS_ITEM = ItemStack.of(Material.LIME_DYE)
            .withCustomName(Component.text("Players: Visible", NamedTextColor.GREEN).style(Style.style().decoration(TextDecoration.ITALIC, false)));

    private final ItemStack HIDE_PLAYERS_ITEM = ItemStack.of(Material.RED_DYE)
            .withCustomName(Component.text("Players: Hidden", NamedTextColor.RED).style(Style.style().decoration(TextDecoration.ITALIC, false)));

    private final TimetrialLeaderboard leaderboard;


    public TimeTrialingInstance(TrackLibrary.Ticket ticket, Track track) {
        super(ticket, track);

        eventNode().addListener(PlayerStartSneakingEvent.class, event -> {
            final Player player = event.getPlayer();

            endTimeTrial(player.getUuid());
            removeBoat(player);
        });

        eventNode().addListener(PlayerGameModeRequestEvent.class, event -> {
            final Player player = event.getPlayer();
            GameMode requested = event.getRequestedGameMode();

            if (requested == GameMode.SURVIVAL) requested = GameMode.ADVENTURE;
            if (requested == GameMode.CREATIVE) requested = GameMode.ADVENTURE;

            endTimeTrial(player.getUuid());
            removeBoat(player);

            player.setGameMode(requested);
        });

        eventNode().addListener(PlayerUseItemEvent.class, event -> {
            final Player player = event.getPlayer();
            final ItemStack itemStack = event.getItemStack();

            if (itemStack == RESET_ITEM) {
                resetPlayer(player);
            } else if (itemStack == BOAT_ITEM) {
                BoatCommand.spawnBoat(player);
            } else if (itemStack == PRACTICE_ITEM) {
                @Nullable Pos practicePoint = practicePoints.get(player.getUuid());

                endTimeTrial(player.getUuid());

                if (practicePoint != null) {
                    createBoat(player, practicePoint);
                }
            } else if (itemStack == SPAWN_ITEM) {
                teleportToSpawn(player);
            } else if (itemStack == SHOW_PLAYERS_ITEM || itemStack == HIDE_PLAYERS_ITEM) {
                LonelyCommand.toggleLonely(player);
            }
        });

        eventNode().addListener(ItemDropEvent.class, event -> {
            final Player player = event.getPlayer();
            final ItemStack itemStack = event.getItemStack();

            event.setCancelled(true);

            if (itemStack == RESET_ITEM) {
                resetPlayer(player);
            } else if (itemStack == BOAT_ITEM) {
                BoatCommand.spawnBoat(player);
            } else if (itemStack == PRACTICE_ITEM) {
                if (practicePoints.containsKey(player.getUuid())) {
                    practicePoints.remove(player.getUuid());

                    MinecraftServer.getGlobalEventHandler()
                            .call(new TimeTrialPracticePointDeleteEvent(player, this));
                } else {
                    if (!player.isOnGround()) return;

                    Pos point = player.getVehicle() instanceof Boat boat ? boat.getPosition() : player.getPosition();
                    practicePoints.put(player.getUuid(), point);

                    MinecraftServer.getGlobalEventHandler()
                            .call(new TimeTrialPracticePointCreateEvent(player, this));
                }
            } else if (itemStack == SPAWN_ITEM) {
                teleportToSpawn(player);
            } else if (itemStack == SHOW_PLAYERS_ITEM || itemStack == HIDE_PLAYERS_ITEM) {
                LonelyCommand.toggleLonely(player);
            }
        });

        leaderboard = new TimetrialLeaderboard(track);
        interfaceHolder = getHolder(TimeTrialingInstance.class, this);
    }

    @Override
    public boolean resetToLastCheckpoint(Player player) {
        @Nullable TimedLap lap = getTimedLap(player.getUuid());

        if (lap == null) return false;

        createBoat(player, getCheckpointReset(lap.getLastReachedCheckpoint()));
        return true;
    }

    @Override
    public @Nullable Boat removeBoat(Player player) {
        Boat boat = super.removeBoat(player);

        player.setAllowFlying(true);

        return boat;
    }

    public void initialize() {
        Pos leaderboard_pos = track.getLocations().getOrDefault(
                "icestom.leaderboard",
                track.getSpawnLocation().asVec().asPos() // remove pitch/yaw
        );

        leaderboard.setInstance(this, leaderboard_pos);
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        for (Player player : getPlayers()) {
            if (player.getPosition().y() < -64) {
                resetPlayer(player);
            }
        }

        for (Player player : getPlayers()) {
            @Nullable TimedLap lap = timeTrials.get(player.getUuid());

            if (lap != null) {
                MinecraftServer.getGlobalEventHandler()
                        .call(new TimeTrialLapTimerEvent(this, lap, player));
            }
        }
    }

    private TimedLap startTimedLap(UUID player, @Nullable TickLocation previous, TickLocation location, long tick_delta, int checkpoint) {
        TimedLap timedLap = new TimedLap(track, getPersonalBest(player), new Split(
                location.tick() * 50,
                tick_delta,
                checkpoint
        ));

        Recording recording = new Recording();

        if (previous != null) {
            recording.writePacket(previous.pos().x(), previous.pos().y(), previous.pos().z(), previous.yaw());
        }

        recording.writePacket(location.pos().x(), location.pos().y(), location.pos().z(), location.yaw());
        recordings.put(player, recording);

        timeTrials.put(player, timedLap);

        MinecraftServer.getGlobalEventHandler()
                .call(new TimeTrialCheckpointAdvancedEvent(this, timedLap, Objects.requireNonNull(getPlayerByUuid(player))));

        if (lua != null) lua.queueEvent(new LuaEventProxy.CheckpointEventProxy(player, 0));

        return timedLap;
    }

    @Override
    protected void handleMovements(List<TickLocation> movements) {
        if (movements.isEmpty()) return;

        Map<Integer, LinkedHashMap<UUID, ArrayDeque<TickLocation>>> pending = new HashMap<>();
        ArrayDeque<Integer> checkpointQueue = new ArrayDeque<>();

        for (TickLocation movement : movements) {
            UUID player = movement.player();

            if (isBeforeTeleport(movement)) continue;

            int next_no = 0;
            @Nullable TimedLap lap = getTimedLap(player);
            if (lap != null) next_no = track.wrapCheckpointIndex(lap.getLastReachedCheckpoint() + 1);

            @Nullable Recording recording = recordings.get(player);

            if (recording != null) {
                recording.writePacket(movement.pos().x(), movement.pos().y(), movement.pos().z(), movement.yaw());
            }

            pending.computeIfAbsent(next_no, _ -> new LinkedHashMap<>())
                    .computeIfAbsent(player, _ -> new ArrayDeque<>())
                    .add(movement);

            checkpointQueue.add(next_no);
        }

        Integer checkpoint;
        while ((checkpoint = checkpointQueue.poll()) != null) {
            LinkedHashMap<UUID, ArrayDeque<TickLocation>> bucket = pending.get(checkpoint);
            if (bucket == null || bucket.isEmpty()) continue;

            LinkedHashMap<UUID, TickLocation> candidates = new LinkedHashMap<>();
            for (Map.Entry<UUID, ArrayDeque<TickLocation>> e : bucket.entrySet()) {
                TickLocation head = e.getValue().peek();
                if (head != null) candidates.put(e.getKey(), head);
            }

            if (candidates.isEmpty()) {
                pending.remove(checkpoint);
                continue;
            }

            List<PlayerMovement> moves = new ArrayList<>();

            List<TickLocation> now_locations = new ArrayList<>(candidates.values());
            List<TickLocation> old_locations = new ArrayList<>(now_locations.size());
            for (TickLocation now : now_locations) {
                @Nullable TickLocation old = lastPosition.getOrDefault(now.player(), null);

                old_locations.add(old);

                moves.add(new PlayerMovement(now.tick(), old, now));
            }

            dispatchTrackEvents(moves);

            Map<TickLocation, Long> hits = new HashMap<>();
            for (CrossCollider trackCheckpoint : track.getCheckpoints(checkpoint)) {
                for (Map.Entry<TickLocation, Long> hit : trackCheckpoint.detectCrosses(old_locations, now_locations).entrySet()) {
                    hits.putIfAbsent(hit.getKey(), hit.getValue());
                }
            }

            for (Map.Entry<UUID, TickLocation> entry : candidates.entrySet()) {
                UUID player = entry.getKey();
                TickLocation location = entry.getValue();

                ArrayDeque<TickLocation> playerQueue = bucket.get(player);
                playerQueue.poll();

                if (isBeforeTeleport(location)) {
                    bucket.remove(player);
                    continue;
                }

                TickLocation previous = lastPosition.put(player, location);

                Long tick_delta = hits.get(location);

                if (tick_delta == null) {
                    if (playerQueue.isEmpty()) bucket.remove(player);
                    continue;
                }


                bucket.remove(player);

                @Nullable TimedLap lap = getTimedLap(player);

                if (lap == null) {
                    startTimedLap(player, previous, location, tick_delta, 0);
                } else {
                    boolean finished = lap.advanceCheckpoint(new Split(
                            location.tick() * 50,
                            tick_delta,
                            checkpoint
                    ));

                    MinecraftServer.getGlobalEventHandler()
                            .call(new TimeTrialCheckpointAdvancedEvent(this, lap, Objects.requireNonNull(getPlayerByUuid(player))));

                    if (lua != null) lua.queueEvent(new LuaEventProxy.CheckpointEventProxy(player, checkpoint));

                    if (finished) {
                        completeLap(player, lap, () -> {
                            startTimedLap(player, previous, location, tick_delta, 0);
                            lastPosition.put(player, location);
                        });
                    }
                }

                int next_checkpoint = track.wrapCheckpointIndex(checkpoint + 1);

                if (!playerQueue.isEmpty()) {
                    LinkedHashMap<UUID, ArrayDeque<TickLocation>> nextBucket =
                            pending.computeIfAbsent(next_checkpoint, _ -> new LinkedHashMap<>());
                    nextBucket.put(player, playerQueue);
                    checkpointQueue.add(next_checkpoint);
                }
            }

            if (bucket.isEmpty()) {
                pending.remove(checkpoint);
            } else {
                checkpointQueue.add(checkpoint);
            }
        }
    }

    protected void completeLap(UUID player, TimedLap lap, Runnable startNextLap) {
        endTimeTrial(player);

        if (track.isLooped()) startNextLap.run();
    }

    public boolean allowsManualReset() {
        return true;
    }

    protected @Nullable TimedLapResultSource getPersonalBest(UUID player) {
        return IceStom.getInstance().getTimetrialDatabase().getBestAttempt(player, track.getId());
    }

    protected boolean savesToLeaderboard() {
        return true;
    }

    @Override
    protected boolean shouldTrackPlayer(Player player) {
        return player.getVehicle() instanceof Boat;
    }

    @Override
    public Pos spawnLocation(Player player) {
        return track.getSpawnLocation();
    }

    @Override
    public void resetPlayer(Player player) {
        endTimeTrial(player.getUuid());
        super.resetPlayer(player);

        player.sendPacket(no_collision_packet);

        PlayerInventory inventory = player.getInventory();

        inventory.clear();
        inventory.setItemStack(1, BOAT_ITEM);
        inventory.setItemStack(8, SPAWN_ITEM);

        if (allowsManualReset()) {
            inventory.setItemStack(0, RESET_ITEM);
            inventory.setItemStack(2, PRACTICE_ITEM);
        }

        updateLonely(player);
    }

    public void updateLonely(Player player) {
        boolean lonely = ((IceStomPlayer) player).preference(LONELY);

        player.getInventory().setItemStack(7, lonely ? HIDE_PLAYERS_ITEM : SHOW_PLAYERS_ITEM);
        player.updateViewerRule(lonely ? entity -> !isSomeoneElses(player, entity) : null);
    }

    private boolean isSomeoneElses(Player viewer, Entity entity) {
        if (entity instanceof Player) return entity != viewer;
        if (entity instanceof Boat boat) return boat != getBoat(viewer);

        return false;
    }

    public @Nullable TimedLap getTimedLap(UUID player) {
        return timeTrials.get(player);
    }

    public void endTimeTrial(UUID player) {
        @Nullable TimedLap timedLap = timeTrials.remove(player);

        lastPosition.remove(player);

        final Player player_instance = getPlayerByUuid(player);
        if (player_instance == null) {
            log.error("Unknown player: {}, this is probably bad", player);
            return;
        }

        if (timedLap != null) {
            TimetrialDatabase timetrialDatabase = IceStom.getInstance().getTimetrialDatabase();

            TimedLapResultSource result = TimedLapResult.freeze(timedLap);

            @Nullable Recording recording = recordings.remove(player);

            if (result.splits().size() == 1) {
                if (recording != null) {
                    try {
                        recording.discard();
                    } catch (IOException e) {
                        log.warn("Failed to discard recording", e);
                        MinecraftServer.getGlobalEventHandler()
                                .call(new GenericErrorMessageEvent(player_instance, Component.translatable("message.lap.error.recording_discard_fail")));
                    }
                }
                return;
            };

            @Nullable TimeTrialResult best = timetrialDatabase.getBestAttempt(player, track.getId());

            boolean is_first = best == null;
            boolean is_best_checkpoints = !is_first && result.splits().size() > best.splits().size();
            boolean is_best_time = !is_first && result.splits().size() == best.splits().size() && result.getTime() < best.getTime();

            boolean is_improvement = savesToLeaderboard() && (is_first || is_best_checkpoints || is_best_time);

            if (is_improvement) {

                if (recording != null) {
                    MinecraftServer.getSchedulerManager().execute(() -> {
                        try {
                            recording.dump(RECORDINGS_PATH.resolve(result.uuid() + ".lf"));
                            tryUpdatePBLine(player_instance);
                        } catch (IOException e) {
                            log.warn("Failed to save recording", e);
                            MinecraftServer.getGlobalEventHandler()
                                    .call(new GenericErrorMessageEvent(player_instance, Component.translatable("message.lap.error.recording_save_fail")));
                        }
                    });
                }

                List<TimeTrialResult> bestRuns = IceStom.getInstance().getTimetrialDatabase().getBestAttempts(track.getId(), 1);
                if (bestRuns.isEmpty()) {
                    MinecraftServer.getGlobalEventHandler()
                            .call(new TimeTrialNewRecordEvent(timedLap, player_instance, this, result, null));
                } else {
                    if (result.isBetterThan(bestRuns.getFirst())) {
                        MinecraftServer.getGlobalEventHandler()
                                .call(new TimeTrialNewRecordEvent(timedLap, player_instance, this, result, bestRuns.getFirst()));
                    }
                }

                IceStom.getInstance().getTimetrialDatabase().newAttempt(TimeTrialResult.fromResult(player, track.getId(), result));
            }

            if (recording != null) {
                try {
                    recording.discard();
                } catch (IOException e) {
                    log.warn("Failed to discard recording", e);
                    MinecraftServer.getGlobalEventHandler()
                            .call(new GenericErrorMessageEvent(player_instance, Component.translatable("message.lap.error.recording_discard_fail")));
                }
            }

            MinecraftServer.getGlobalEventHandler()
                    .call(new TimeTrialTimedLapEndedEvent(this, timedLap, player_instance, best));

            leaderboard.updateLeaderboard();
        }
    }

    @Override
    public void consume(Player player) {
        super.consume(player);

        interfaceHolder.startWatching(player);

        MinecraftServer.getGlobalEventHandler()
                .call(new TimeTrialStartEvent(this, player));

        tryUpdatePBLine(player);
    }

    private void tryUpdatePBLine(Player player) {
        if (!((IceStomPlayer) player).supportsModFeature(MOD_FEATURE_TIMETRIAL, 1)) return;

        MinecraftServer.getSchedulerManager().execute(() -> {
            @Nullable TimeTrialResult result = IceStom.getInstance().getTimetrialDatabase().getBestAttempt(player.getUuid(), track.getId());

            if (result == null) {
                MinecraftServer.getSchedulerManager().execute(() -> player.sendPacket(new PluginMessagePacket(
                        ISChannel.TIMETRIAL.getIdentifier(),
                        ClientboundTimetrial.newBuilder()
                                .setPbLine(ServerPBLine.newBuilder()
                                        .setLine(linefoldEmpty)
                                        .build())
                                .build()
                                .toByteArray())));
                return;
            }

            Path recording = RECORDINGS_PATH.resolve(result.uuid() + ".lf");

            if (!recording.toFile().exists()) return;

            try (InputStream input = Files.newInputStream(recording)) {
                ByteString bytes = ByteString.readFrom(input);

                MinecraftServer.getSchedulerManager().execute(() -> player.sendPacket(new PluginMessagePacket(
                        ISChannel.TIMETRIAL.getIdentifier(),
                        ClientboundTimetrial.newBuilder()
                                .setPbLine(ServerPBLine.newBuilder()
                                        .setLine(bytes)
                                        .build())
                                .build()
                                .toByteArray())));
            } catch (IOException e) {
                log.warn("Failed to read pb line", e);
            }
        });
    }

    @Override
    public void drop(Player player) {
        super.drop(player);

        endTimeTrial(player.getUuid());

        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(false);
        player.setFlying(false);

        player.getInventory().clear();

        player.sendPacket(no_collision_packet);
        player.updateViewerRule(null);

        interfaceHolder.stopWatching(player);
    }

    private void teleportToSpawn(Player player) {
        drop(player);
        IceStom.getInstance().getSpawnInstance().consume(player);
    }
}
