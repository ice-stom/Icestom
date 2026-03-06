package io.gitlab.icestom.icestom;

import io.gitlab.icestom.icestom.instance.TimeTrialInstance;
import io.gitlab.icestom.icestom.track.Track;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TimeTrialManager {
    private final Map<String, TimeTrialInstance> trials = new HashMap<>();

    public void startTimeTrial(Player player, Track track) {
        endTimeTrial(player);

        @Nullable TimeTrialInstance instance = trials.get(track.getId());

        if (instance == null) {
            instance = new TimeTrialInstance(track);

            MinecraftServer.getInstanceManager().registerInstance(instance);
        }

        instance.consume(player);
    }

    public void endTimeTrial(Player player) {
        if (!(player.getInstance() instanceof TimeTrialInstance timeTrialInstance)) return;

        IceStom.getInstance().getSpawnInstance().consume(player);

        if (timeTrialInstance.getPlayers().isEmpty()) {
            destroyTimeTrial(timeTrialInstance);
        }
    }

    public void destroyTimeTrial(@NotNull TimeTrialInstance instance) {
        for (Player player : instance.getPlayers()) {
            IceStom.getInstance().getSpawnInstance().consume(player);
        }

        MinecraftServer.getInstanceManager().unregisterInstance(instance);
    }

    public void destroyTimeTrial(String id) {
        @Nullable TimeTrialInstance instance = trials.remove(id);

        if (instance != null) {
            destroyTimeTrial(instance);
        }
    }
}
