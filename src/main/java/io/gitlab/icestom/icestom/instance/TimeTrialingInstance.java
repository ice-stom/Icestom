package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.checkpoint.TickMovement;
import io.gitlab.icestom.icestom.trial.TimeTrial;
import io.gitlab.icestom.icestom.trial.TimetrialResultSource;
import io.gitlab.icestom.icestom.ui.ActionBarProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TimeTrialingInstance extends TrackInstance implements ActionBarProvider {
    private final Map<Player, TimeTrial> timeTrials = new HashMap<>();

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

            @Nullable TimeTrial timeTrial = getTimeTrial(player);

            if (timeTrial != null) {
                int next_no = track.wrapCheckpointIndex(timeTrial.getCheckpoint() + 1);
                Collection<Checkpoint> checkpoints = track.getCheckpoints(next_no);

                for (Checkpoint checkpoint : checkpoints) {
                    @Nullable Long tick_delta = checkpoint.detectCross(movement);

                    if (tick_delta != null) {
                        timeTrial.nextCheckpoint(new TimeTrial.Split(
                                getWorldAge() * 50,
                                tick_delta,
                                next_no
                        ));

                        if (next_no == 0) {
                            TimeTrial completed = endTimeTrial(player);

                            IceStom.getInstance().getTimetrialDatabase().updateBestTime(player, track.getId(), completed);

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

                @Nullable TimetrialResultSource best_result = IceStom.getInstance().getTimetrialDatabase().getBestTime(player, track.getId());

                TimeTrial timeTrial = new TimeTrial(player, track, best_result, new TimeTrial.Split(
                        getWorldAge() * 50,
                        tick_delta,
                        0
                ));
                timeTrials.put(player, timeTrial);
            }
        }
    }

    public void resetPlayer(Player player) {
        Pos spawn = track.getSpawnLocation();
        Entity vehicle = player.getVehicle();

        if (vehicle != null) {
            vehicle.removePassenger(player);

            boolean noPlayersRemaining = vehicle.getPassengers().stream()
                    .noneMatch(entity -> entity instanceof Player);

            if (noPlayersRemaining) {
                vehicle.getPassengers().forEach(Entity::remove);
                vehicle.remove();
            }
        }

        endTimeTrial(player);

        createBoat(player, spawn);
    }

    public @Nullable TimeTrial getTimeTrial(Player player) {
        return timeTrials.get(player);
    }

    public TimeTrial endTimeTrial(Player player) {
        @Nullable TimeTrial timeTrial = getTimeTrial(player);

        if (timeTrial != null) {
            timeTrials.remove(player);

            player.sendMessage(Component.text("You have finished a time trial."));
        }

        return timeTrial;
    }

    @Override
    public Component getActionBar(Player player) {
        @Nullable TimeTrial timeTrial = getTimeTrial(player);

        if (timeTrial != null) {
            long time = getWorldAge() * 50 - timeTrial.getMsStart();

            // we are actually sometimes in the past due to subtick delta
            // as a result we render the time roughly 1 tick in the future to make up for this.
            // this timer is just visual
            time++;

            return Component.text(String.format("%.2f", (float) Math.round((float) time / 50) * 50 / 1000));
        }

        return Component.text("At ").append(Component.text(track.getId(), NamedTextColor.GOLD));
    }

    public void consume(Player player) {
        if (player.getInstance() == this) {
            resetPlayer(player);
        } else {
            player.setInstance(this, track.getSpawnLocation())
                    .thenRun(() -> resetPlayer(player));
        }
    }

    public void drop(Player player) {
        endTimeTrial(player);
    }
}
