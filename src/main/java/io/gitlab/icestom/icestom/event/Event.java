package io.gitlab.icestom.icestom.event;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.stage.Stage;
import io.gitlab.icestom.icestom.instance.PlayerHolder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Event {
    private final String id;
    private final List<Stage<?>> stages;
    private final Set<UUID> participants = new HashSet<>();

    private int currentStage = -1;

    public Event(String id, List<Stage<?>> stages) {
        this.id = id;
        this.stages = new ArrayList<>(stages);
    }

    public @Nullable Stage<?> getCurrentStage() {
        if (currentStage < 0) return null;
        if (currentStage >= stages.size()) return null;

        return stages.get(currentStage);
    }

    public void addPlayer(Player player) {
        participants.add(player.getUuid());

        @Nullable Stage<?> currentStage = getCurrentStage();

        if (currentStage != null) {
            currentStage.getInstance().consume(player);
        }
    }

    public List<Player> getOnlineParticipants() {
        List<Player> online = new ArrayList<>();

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (participants.contains(player.getUuid())) {
                online.add(player);
            }
        }

        return online;
    }

    public void removePlayer(Player player) {
        participants.remove(player.getUuid());

        @Nullable Stage<?> currentStage = getCurrentStage();

        if (player.getInstance() == currentStage && currentStage != null) {
            currentStage.getInstance().drop(player);
        }

        IceStom.getInstance().getSpawnInstance().consume(player);
    }

    public void nextStage() {
        @Nullable Stage<?> current = getCurrentStage();

        if (current != null) {
            Instance instance = current.getInstance();

            for (Player player : instance.getPlayers()) {
                current.getInstance().drop(player);
            }

            MinecraftServer.getInstanceManager().unregisterInstance(instance);
        }

        currentStage++;

        @Nullable Stage<?> next = getCurrentStage();

        if (next == null) return;

        MinecraftServer.getInstanceManager().registerInstance(next.getInstance());

        PlayerHolder holder = next.getInstance();
        for (Player participant : getOnlineParticipants()) {
            holder.consume(participant);
        }
    }

    public String getId() {
        return id;
    }

    public boolean hasParticipant(UUID uuid) {
        return participants.contains(uuid);
    }

    public Set<UUID> getParticipants() {
        return participants;
    }
}
