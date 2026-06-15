package io.gitlab.icestom.icestom.timetrial;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialStartEvent;
import io.gitlab.icestom.icestom.track.Track;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TimeTrialManager {
    private final Map<String, TimeTrialingInstance> trials = new HashMap<>();

    public void starTimeTrialing(Player player, Track track) {
        stopTimeTrialing(player);

        @Nullable TimeTrialingInstance instance = trials.get(track.getId());

        if (instance == null) {
            instance = new TimeTrialingInstance(track);

            MinecraftServer.getInstanceManager().registerInstance(instance);

            instance.start();

            trials.put(track.getId(), instance);
        }

        instance.consume(player);

        MinecraftServer.getGlobalEventHandler()
                .call(new TimeTrialStartEvent(instance, player));
    }

    public void stopTimeTrialing(Player player) {
        if (!(player.getInstance() instanceof TimeTrialingInstance timeTrialingInstance)) return;

        timeTrialingInstance.drop(player);

        if (timeTrialingInstance.getPlayers().isEmpty()) {
            destroyInstance(timeTrialingInstance);
        }
    }

    public void destroyInstance(@NotNull TimeTrialingInstance instance) {
        for (Player player : instance.getPlayers()) {
            instance.drop(player);
            IceStom.getInstance().getSpawnInstance().consume(player);
        }

        trials.remove(instance.getTrack().getId());

        MinecraftServer.getInstanceManager().unregisterInstance(instance);
    }
}
