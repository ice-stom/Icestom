package io.gitlab.icestom.icestom.timetrial;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.instance.SpawnLocation;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialLapCompletedEvent;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.checkpoint.TickMovement;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import io.gitlab.icestom.icestom.ui.ActionBarProvider;
import io.gitlab.icestom.icestom.ui.InterfaceHolder;
import io.gitlab.icestom.icestom.timetrial.ui.TimeTrialInterfaceProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TimeTrialingInstance extends TrackInstance implements SpawnLocation, ActionBarProvider {
    private static final InterfaceHolder<TimeTrialInterfaceProvider> INTERFACE_HOLDER = new InterfaceHolder<>(TimeTrialInterfaceProvider.class);

    private final Map<Player, TimedLap> timeTrials = new HashMap<>();

    public TimeTrialingInstance(Track track) {
        super(track);
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        for (Player player : getPlayers()) {
            if (player.getPosition().y() < -64) {
                resetPlayer(player);
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

                            @Nullable TimedLapResultSource best = IceStom.getInstance().getTimetrialDatabase().getBestTime(player.getUuid(), track.getId());

                            boolean personal_record = best == null || time < best.getTime();

                            if (personal_record) IceStom.getInstance().getTimetrialDatabase().updateBestTime(player.getUuid(), track.getId(), completed);

                            MinecraftServer.getGlobalEventHandler()
                                    .call(new TimeTrialLapCompletedEvent(this, timedLap, player, best == null ? null : time - best.getTime()));

                            INTERFACE_HOLDER.getProviders().forEach(provider -> provider.dispatchTimeTrialLeaderboard(this));

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

                @Nullable TimedLapResultSource best_result = IceStom.getInstance().getTimetrialDatabase().getBestTime(player.getUuid(), track.getId());

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
        return true;
    }

    @Override
    public Pos spawnLocation(Player player) {
        return track.getSpawnLocation();
    }

    @Override
    public void resetPlayer(Player player) {
        endTimeTrial(player);
        super.resetPlayer(player);
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
    public @Nullable Component getActionBar(Player player) {
        @Nullable TimedLap timedLap = getTimedLap(player);

        if (timedLap != null) {
            return timedLap.getActionBar(player);
        }

        return Component.text("At ").append(Component.text(track.getId(), NamedTextColor.GOLD));
    }

    @Override
    public void consume(Player player) {
        super.consume(player);

        INTERFACE_HOLDER.addViewer(player);
        INTERFACE_HOLDER.getProviders().forEach(provider -> provider.dispatchTimeTrialLeaderboard(this));
    }

    @Override
    public void drop(Player player) {
        super.drop(player);

        endTimeTrial(player);

        INTERFACE_HOLDER.removeViewer(player);
    }
}
