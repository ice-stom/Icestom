package io.gitlab.icestom.icestom.timetrial;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.command.BoatCommand;
import io.gitlab.icestom.icestom.config.IceStomConfig;
import io.gitlab.icestom.icestom.database.TimetrialDatabase;
import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.TimetrialLeaderboard;
import io.gitlab.icestom.icestom.instance.BoatedTrackInstance;
import io.gitlab.icestom.icestom.instance.SpawnLocation;
import io.gitlab.icestom.icestom.timetrial.event.*;
import io.gitlab.icestom.icestom.timetrial.lap.TimeTrialResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResult;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.gitlab.icestom.icestom.track.TickMovement;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerGameModeRequestEvent;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager.getHolder;

@SuppressWarnings("UnstableApiUsage")
public class TimeTrialingInstance extends BoatedTrackInstance implements SpawnLocation {
    private final InterfaceManager.InterfaceHolder interfaceHolder = getHolder(TimeTrialingInstance.class, this);

    private final Map<Player, TimedLap> timeTrials = new HashMap<>();

    private final Map<UUID, Pos> practicePoints = new HashMap<>();

    private final ItemStack RESET_ITEM = ItemStack.of(Objects.requireNonNull(Material.fromKey(IceStomConfig.getConfig().icestom.reset_item)))
            .withCustomName(Component.text("Reset", NamedTextColor.RED).style(Style.style().decoration(TextDecoration.ITALIC, false)));

    private final ItemStack BOAT_ITEM = ItemStack.of(Material.OAK_BOAT)
            .withCustomName(Component.text("Boat", NamedTextColor.GREEN).style(Style.style().decoration(TextDecoration.ITALIC, false)));

    private final ItemStack PRACTICE_ITEM = ItemStack.of(Material.APPLE)
            .withCustomName(Component.text("Practice Point", NamedTextColor.YELLOW).style(Style.style().decoration(TextDecoration.ITALIC, false)));

    private final ItemStack SPAWN_ITEM = ItemStack.of(Material.RED_BED)
            .withCustomName(Component.text("Return to spawn", NamedTextColor.RED).style(Style.style().decoration(TextDecoration.ITALIC, false)));

    private final TimetrialLeaderboard leaderboard;

    public TimeTrialingInstance(Track track) {
        super(track);

        eventNode().addListener(PlayerStartSneakingEvent.class, event -> {
            final Player player = event.getPlayer();

            endTimeTrial(player);
            removeBoat(player);
        });

        eventNode().addListener(PlayerGameModeRequestEvent.class, event -> {
            final Player player = event.getPlayer();
            GameMode requested = event.getRequestedGameMode();

            if (requested == GameMode.SURVIVAL) requested = GameMode.ADVENTURE;
            if (requested == GameMode.CREATIVE) requested = GameMode.ADVENTURE;

            endTimeTrial(player);
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

                endTimeTrial(player);

                if (practicePoint != null) {
                    createBoat(player, practicePoint);
                }
            } else if (itemStack == SPAWN_ITEM) {
                teleportToSpawn(player);
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
                    if (player.isOnGround()) {
                        practicePoints.put(player.getUuid(), player.getPosition());

                        MinecraftServer.getGlobalEventHandler()
                                .call(new TimeTrialPracticePointCreateEvent(player, this));
                    } else if (player.getVehicle() instanceof Boat boat) {
                        practicePoints.put(player.getUuid(), boat.getPosition());

                        MinecraftServer.getGlobalEventHandler()
                                .call(new TimeTrialPracticePointCreateEvent(player, this));
                    }
                }
            } else if (itemStack == SPAWN_ITEM) {
                teleportToSpawn(player);
            }
        });

        leaderboard = new TimetrialLeaderboard(track);
    }

    @Override
    public @Nullable Boat removeBoat(Player player) {
        Boat boat = super.removeBoat(player);

        player.setAllowFlying(true);

        return boat;
    }

    @Override
    public void start() {
        Pos leaderboard_pos = track.getLocations().getOrDefault(
                "icestom.leaderboard",
                track.getSpawnLocation().asVec().asPos() // remove pitch/yaw
        );

        leaderboard.setInstance(this, leaderboard_pos);

        subscribeRegionId("icestom.reset");
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
            @Nullable TimedLap lap = timeTrials.get(player);

            if (lap != null) {
                MinecraftServer.getGlobalEventHandler()
                        .call(new TimeTrialLapTimerEvent(this, lap, player));
            }
        }
    }

    @Override
    protected void onPlayerMovements(Map<Player, TickMovement> movements, Map<Player, Set<String>> inside_regions, Map<Player, Map<String, Long>> crossed_triggers) {
        if (movements.isEmpty()) return;

        List<CrossCollider> initial_checkpoints = track.getCheckpoints(0);

        Map<Player, TickMovement> not_started_tt = new HashMap<>();

        for (Map.Entry<Player, TickMovement> entry : movements.entrySet()) {
            Player player = entry.getKey();
            TickMovement movement = entry.getValue();

            Set<String> playerRegions = inside_regions.getOrDefault(player, Set.of());

            @Nullable TimedLap timedLap = getTimedLap(player);

            if (timedLap != null) {
                int next_no = track.wrapCheckpointIndex(timedLap.getLastReachedCheckpoint() + 1);
                Collection<CrossCollider> checkpoints = track.getCheckpoints(next_no);

                for (CrossCollider checkpoint : checkpoints) {
                    @Nullable Long tick_delta = checkpoint.detectCross(movement);

                    if (tick_delta != null) {
                        boolean finished = timedLap.advanceCheckpoint(new Split(
                                getWorldAge() * 50,
                                tick_delta,
                                next_no
                        ));

                        MinecraftServer.getGlobalEventHandler()
                                .call(new TimedLapCheckpointAdvancedEvent(timedLap, player));

                        if (finished) {
                            endTimeTrial(player);

                            not_started_tt.put(player, movement);
                        }
                    }
                }

                if (playerRegions.contains("icestom.reset")) {
                    Pos reset_point = track.getLocations().getOrDefault("icestom.reset_" + timedLap.getLastReachedCheckpoint(), track.getSpawnLocation());

                    createBoat(player, reset_point);
                }

            } else {
                not_started_tt.put(player, movement);
            }
        }

        Set<Player> crossed = new HashSet<>();

        for (CrossCollider initialCheckpoint : initial_checkpoints) {
            for (Map.Entry<Player, Long> entry : initialCheckpoint.detectCrosses(not_started_tt).entrySet()) {
                Player player = entry.getKey();

                if (crossed.contains(player)) continue;
                crossed.add(player);

                long tick_delta = entry.getValue();

                @Nullable TimeTrialResult best_result = IceStom.getInstance().getTimetrialDatabase().getBestAttempt(player.getUuid(), track.getId());

                TimedLap timedLap = new TimedLap(track, best_result, new Split(
                        getWorldAge() * 50,
                        tick_delta,
                        0
                ));

                MinecraftServer.getGlobalEventHandler()
                        .call(new TimedLapCheckpointAdvancedEvent(timedLap, player));

                timeTrials.put(player, timedLap);
            }
        }
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
        endTimeTrial(player);
        super.resetPlayer(player);

        PlayerInventory inventory = player.getInventory();

        inventory.clear();
        inventory.setItemStack(0, RESET_ITEM);
        inventory.setItemStack(1, BOAT_ITEM);
        inventory.setItemStack(2, PRACTICE_ITEM);
        inventory.setItemStack(8, SPAWN_ITEM);
    }

    public @Nullable TimedLap getTimedLap(Player player) {
        return timeTrials.get(player);
    }

    public void endTimeTrial(Player player) {
        @Nullable TimedLap timedLap = timeTrials.remove(player);

        if (timedLap != null) {
            TimetrialDatabase timetrialDatabase = IceStom.getInstance().getTimetrialDatabase();

            TimedLapResultSource result = TimedLapResult.freeze(timedLap);

            if (result.splits().size() == 1) return;

            @Nullable TimeTrialResult best = timetrialDatabase.getBestAttempt(player.getUuid(), track.getId());

            boolean is_first = best == null;
            boolean is_best_checkpoints = !is_first && result.splits().size() > best.splits().size();
            boolean is_best_time = !is_first && result.splits().size() == best.splits().size() && result.getTime() < best.getTime();

            if (is_first || is_best_checkpoints || is_best_time) {
                IceStom.getInstance().getTimetrialDatabase().newAttempt(TimeTrialResult.fromResult(player.getUuid(), track.getId(), result));
            }

            MinecraftServer.getGlobalEventHandler()
                    .call(new TimeTrialTimedLapEndedEvent(this, timedLap, player, best));

            leaderboard.updateLeaderboard();
        }

    }

    @Override
    public void consume(Player player) {
        super.consume(player);

        interfaceHolder.startWatching(player);
    }

    @Override
    public void drop(Player player) {
        super.drop(player);

        endTimeTrial(player);

        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(false);
        player.setFlying(false);

        player.getInventory().clear();

        interfaceHolder.stopWatching(player);
    }

    private void teleportToSpawn(Player player) {
        drop(player);
        IceStom.getInstance().getSpawnInstance().consume(player);
    }
}
