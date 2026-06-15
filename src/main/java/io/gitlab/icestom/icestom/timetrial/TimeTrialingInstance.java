package io.gitlab.icestom.icestom.timetrial;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.config.IceStomConfig;
import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.TimetrialLeaderboard;
import io.gitlab.icestom.icestom.instance.SpawnLocation;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialLapCompletedEvent;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialLapTimerEvent;
import io.gitlab.icestom.icestom.timetrial.lap.TimeTrialResult;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.checkpoint.TickMovement;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static io.gitlab.icestom.icestom.ui.interfaces.InterfaceManager.getHolder;

@SuppressWarnings("UnstableApiUsage")
public class TimeTrialingInstance extends TrackInstance implements SpawnLocation {
    private final InterfaceManager.InterfaceHolder interfaceHolder = getHolder(TimeTrialingInstance.class, this);

    private final Map<Player, TimedLap> timeTrials = new HashMap<>();

    private final ItemStack RESET_ITEM = ItemStack.of(Objects.requireNonNull(Material.fromKey(IceStomConfig.getConfig().icestom.reset_item)))
            .withCustomName(Component.text("Reset", NamedTextColor.RED));

    private final TimetrialLeaderboard leaderboard;

    public TimeTrialingInstance(Track track) {
        super(track);

        eventNode().addListener(PlayerStartSneakingEvent.class, event -> {
            final Player player = event.getPlayer();

            endTimeTrial(player);
        });

        eventNode().addListener(PlayerUseItemEvent.class, event -> {
            final Player player = event.getPlayer();
            final ItemStack itemStack = event.getItemStack();

            if (itemStack == RESET_ITEM) {
                resetPlayer(player);
            }
        });

        eventNode().addListener(PlayerStartSneakingEvent.class, event -> {
            final Player player = event.getPlayer();

            if (removeBoat(player) != null) {
                Pos position = player.getPosition();

                player.teleport(player.getPosition().withY(Math.ceil(position.y() + 1)));
            };
        });

        leaderboard = new TimetrialLeaderboard(track);
    }

    @Override
    public void start() {
        leaderboard.setInstance(this, track.getSpawnLocation());
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
    protected void onPlayerMovements(Map<Player, TickMovement> movements) {
        if (movements.isEmpty()) return;

        List<Checkpoint> initial_checkpoints = track.getCheckpoints(0);

        Map<Player, TickMovement> not_started_tt = new HashMap<>();

        for (Map.Entry<Player, TickMovement> entry : movements.entrySet()) {
            Player player = entry.getKey();
            TickMovement movement = entry.getValue();

            @Nullable TimedLap timedLap = getTimedLap(player);

            if (timedLap != null) {
                int next_no = track.wrapCheckpointIndex(timedLap.getLastReachedCheckpoint() + 1);
                Collection<Checkpoint> checkpoints = track.getCheckpoints(next_no);

                for (Checkpoint checkpoint : checkpoints) {
                    @Nullable Long tick_delta = checkpoint.detectCross(movement);

                    if (tick_delta != null) {
                        timedLap.advanceCheckpoint(new Split(
                                getWorldAge() * 50,
                                tick_delta,
                                next_no
                        ));

                        if (next_no == 0) {
                            TimedLap completed = endTimeTrial(player);

                            long time = completed.getTime();

                            @Nullable TimeTrialResult best = IceStom.getInstance().getTimetrialDatabase().getBestAttempt(player.getUuid(), track.getId());

                            boolean personal_record = best == null || time < best.getTime();

                            if (personal_record) IceStom.getInstance().getTimetrialDatabase().newAttempt(TimeTrialResult.fromResult(player.getUuid(), track.getId(), completed));

                            MinecraftServer.getGlobalEventHandler()
                                    .call(new TimeTrialLapCompletedEvent(this, timedLap, player, best == null ? null : time - best.getTime()));

                            leaderboard.updateLeaderboard();

                            not_started_tt.put(player, movement);
                        }
                    }
                }

            } else {
                not_started_tt.put(player, movement);
            }
        }

        Set<Player> crossed = new HashSet<>();

        for (Checkpoint initialCheckpoint : initial_checkpoints) {
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
    }

    public @Nullable TimedLap getTimedLap(Player player) {
        return timeTrials.get(player);
    }

    public TimedLap endTimeTrial(Player player) {
        @Nullable TimedLap timedLap = getTimedLap(player);

        if (timedLap != null) {
            timeTrials.remove(player);
        }

        return timedLap;
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

        interfaceHolder.stopWatching(player);
    }
}
