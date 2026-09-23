package io.gitlab.icestom.icestom.timetrial;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialStartEvent;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TimeTrialManager {
    private final Map<UUID, TimeTrialingInstance> trials = new HashMap<>();

    private final Object lock = new Object();

    public void startTimeTrialing(Player player, TrackLibrary.Ticket ticket) {

        UUID instance_id = ticket.getTrackInstanceId();

//        if (!ticket.getOpenBoatUtilsPackets().isEmpty()) {
//            if (((IceStomPlayer) player).getOpenBoatUtilsVersion() == null) {
//                player.sendMessage(Component.translatable("message.timetrial.requires_open_boat_utils"));
//                return;
//            }
//        }

        synchronized (lock) {
            stopTimeTrialing(player);

            @Nullable TimeTrialingInstance instance = trials.get(instance_id);

            if (instance == null) {
                instance = new TimeTrialingInstance(ticket);

                MinecraftServer.getInstanceManager().registerSharedInstance(instance);

                instance.initialize();

                trials.put(instance_id, instance);
            }

            instance.consume(player);

            MinecraftServer.getGlobalEventHandler()
                    .call(new TimeTrialStartEvent(instance, player));
        }
    }

    public void stopTimeTrialing(Player player) {
        if (!(player.getInstance() instanceof TimeTrialingInstance timeTrialingInstance)) return;

        timeTrialingInstance.drop(player);

        if (timeTrialingInstance.getPlayers().isEmpty()) {
            destroyInstance(timeTrialingInstance);
        }
    }

    // there is a billion ways that people can leave a tt instance, this will catch the few edge cases
    // things like /spawn will hook this class directly
    public void cullDeadTimetrialInstances() {
        synchronized (lock) {
            for (TimeTrialingInstance trial : trials.values()) {
                if (trial.getPlayers().isEmpty()) {
                    destroyInstance(trial);
                }
            }
        }
    }

    public void destroyInstance(@NotNull TimeTrialingInstance instance) {
        for (Player player : instance.getPlayers()) {
            instance.drop(player);
            IceStom.getInstance().getSpawnInstance().consume(player);
        }

        TrackLibrary.Ticket ticket = instance.getTicket();

        trials.remove(ticket.getTrackInstanceId());

        ticket.burn();

        MinecraftServer.getInstanceManager().unregisterInstance(instance);
    }
}
