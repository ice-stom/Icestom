package io.gitlab.icestom.icestom.timetrial;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TimeTrialManager {
    private static final Logger log = LoggerFactory.getLogger(TimeTrialManager.class);

    private final Map<String, CompletableFuture<TrackLibrary.Ticket>> loadTickets = new HashMap<>();
    private final Map<String, CompletableFuture<TimeTrialingInstance>> instances = new HashMap<>();

    private final Map<String, Integer> ttRefs = new HashMap<>();

    private final Map<Player, Ticket> ttTickets = new WeakHashMap<>();

    private final Set<TimeTrialingInstance> weakInstances = Collections.newSetFromMap(new WeakHashMap<>());

    private final Object lock = new Object();

    public void unprovisionInstance(TimeTrialingInstance instance) {
        String id = instance.getTrack().getId();
        synchronized (lock) {
            if (ttRefs.containsKey(id)) return;
            var f = instances.get(id);
            if (f == null || !f.isDone() || f.isCompletedExceptionally() || f.join() != instance) return;

            instances.remove(id);
            var load = loadTickets.remove(id);

            for (Player p : instance.getPlayers()) {
                instance.drop(p);
                IceStom.getInstance().getSpawnInstance().consume(p);
            }
            instance.getTicket().burn();
            MinecraftServer.getInstanceManager().unregisterInstance(instance);
            if (load != null) load.thenAccept(TrackLibrary.Ticket::burn);
        }
    }

    // create a tt ticket and create a loadTicket if there isn't already one
    private CompletableFuture<Ticket> makeTicket(String trackId) {
        final CompletableFuture<TrackLibrary.Ticket> load;
        synchronized (lock) {
            CompletableFuture<TrackLibrary.Ticket> existing = loadTickets.get(trackId);
            if (existing == null) {
                CompletableFuture<TrackLibrary.Ticket> fresh = IceStom.getInstance().getTrackLibrary()
                        .loadTrack(trackId)
                        .thenApply(opt -> opt.orElseThrow(() -> new IllegalArgumentException("Failed to load track")));
                loadTickets.put(trackId, fresh);   // put first, so a synchronous failure still gets evicted
                fresh.whenComplete((t, e) -> {
                    if (e != null) synchronized (lock) { loadTickets.remove(trackId, fresh); }
                });
                existing = fresh;
            }
            load = existing;
        }

        return load.thenCompose(loadTicket -> {
            synchronized (lock) {
                if (loadTickets.get(trackId) != load) {
                    return makeTicket(trackId);
                }
                @Nullable CompletableFuture<TimeTrialingInstance> inst = instances.get(trackId);

                if (inst == null) {
                    inst = loadTicket.getTrackMeta().thenApply(track -> {
                        synchronized (lock) {
                            TimeTrialingInstance created = new ManagedTimetrialInstance(loadTicket, track);
                            weakInstances.add(created);
                            MinecraftServer.getInstanceManager().registerSharedInstance(created);
                            return created;
                        }
                    });
                    instances.put(trackId, inst);
                }

                return CompletableFuture.completedFuture(new Ticket(trackId, loadTicket, inst));
            }
        });
    }

    // can only return tt tickets that a player can actually use (e.g: boatutils is checked)
    public CompletableFuture<Ticket> requestTimeTrial(@NotNull Player player, @NotNull String trackId) {
        synchronized (lock) {
            Ticket existing = ttTickets.get(player);
            if (existing != null && !existing.burnt && trackId.equals(existing.trackId)) {
                return CompletableFuture.completedFuture(existing);
            }
        }

        return makeTicket(trackId).thenCompose(ticket -> ticket.trackTicket.getTrackMeta().thenApply(track -> {
            if (!track.getOpenBoatUtilsPackets().isEmpty()
                    && ((IceStomPlayer) player).getOpenBoatUtilsVersion() == null) {
                throw new IllegalStateException("This track requires OpenBoatUtils");
            }
            if (!player.isOnline()) throw new IllegalStateException("Player left");

            ticket.trackTicket.promote();

            synchronized (lock) {
                Ticket previous = ttTickets.get(player);
                if (previous != null && !previous.burnt && previous.trackId.equals(trackId)) {
                    ticket.burn();
                    return previous;
                }
                ttTickets.put(player, ticket);
                if (previous != null) previous.burn();
            }
            return ticket;
        }).whenComplete((t, e) -> {
            if (e != null) {
                log.warn("Time trial request for {} failed", trackId, e);
                ticket.burn();
            }
        }));
    }

    public void release(Player player) {
        synchronized (lock) {
            Ticket t = ttTickets.remove(player);
            if (t != null) t.burn();
        }
    }

    public boolean isUnreferenced(String trackId) { return !ttRefs.containsKey(trackId); }

    public Map<String, Integer> getTtRefs() {
        return ttRefs;
    }

    public class Ticket {
        public boolean burnt = false;

        private final String trackId;
        private final TrackLibrary.Ticket trackTicket;
        private final CompletableFuture<TimeTrialingInstance> instance;

        Ticket(String trackId, TrackLibrary.Ticket trackTicket, CompletableFuture<TimeTrialingInstance> instance) {
            this.trackId = trackId;
            this.trackTicket = trackTicket;
            this.instance = instance;

            ttRefs.merge(trackId, 1, Integer::sum);
        }

        public void burn() {
            synchronized (lock) {
                if (burnt) return;
                burnt = true;
                ttRefs.computeIfPresent(trackId, (_, refs) -> refs <= 1 ? null : refs - 1);
            }
        }

        public TrackLibrary.Ticket getTrackTicket() {
            return trackTicket;
        }

        public String getTrackId() {
            return trackId;
        }

        public CompletableFuture<TimeTrialingInstance> instance() {
            return instance.thenApply(instance1 -> {
                trackTicket.readyForTeleport().join();
                return instance1;
            });
        }
    }
}
