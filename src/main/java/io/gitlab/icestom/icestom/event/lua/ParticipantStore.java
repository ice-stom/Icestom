package io.gitlab.icestom.icestom.event.lua;

import io.gitlab.icestom.icestom.event.event.EventParticipant;
import net.minestom.server.entity.Player;

import java.util.*;

public class ParticipantStore {
    private final List<EventParticipant> participants;

    private final Map<UUID, EventParticipant> weakParticipantMap = new HashMap<>();

    private final Map<Player, EventParticipant> weakActiveParticipants = new HashMap<>();
    private final Map<UUID, EventParticipant> weakParticipants = new HashMap<>();

    private final Map<EventParticipant, Player> weakPreviousActiveParticipant = new HashMap<>();

    public ParticipantStore() {
        this(List.of());
    }

    ParticipantStore(List<EventParticipant> initial) {
        participants = new ArrayList<>(initial);
        for (EventParticipant participant : participants) {
            weakParticipantMap.put(participant.getUuid(), participant);
        }
    }

    public void addParticipant(EventParticipant participant) {
        for (UUID participantPlayer : participant.getPlayers()) {
            if (weakParticipants.containsKey(participantPlayer)) {
                throw new UnsupportedOperationException("Participant has a player that is already participating!");
            }
        }

        participants.add(participant);
        weakParticipantMap.put(participant.getUuid(), participant);

        participant.getPlayers().forEach(player -> weakParticipants.put(player, participant));

        weakActiveParticipants.put(participant.getCurrentPlayer(), participant);
        weakPreviousActiveParticipant.put(participant, participant.getCurrentPlayer());
    }

    public void removeParticipant(EventParticipant participant) {
        participants.remove(participant);
        weakParticipantMap.remove(participant.getUuid());

        participant.getPlayers().forEach(weakParticipants::remove);

        weakActiveParticipants.remove(participant.getCurrentPlayer());
        weakPreviousActiveParticipant.remove(participant);
    }

    public void updateActiveParticipant(EventParticipant eventParticipant) {
        if (!participants.contains(eventParticipant)) {
            throw new UnsupportedOperationException("Can't update a participant that isn't participating!");
        }

        Player old = weakPreviousActiveParticipant.get(eventParticipant);

        weakActiveParticipants.remove(old);
        weakActiveParticipants.put(eventParticipant.getCurrentPlayer(), eventParticipant);
        weakPreviousActiveParticipant.put(eventParticipant, eventParticipant.getCurrentPlayer());
    }

    public List<Player> getActivelyParticipatingPlayers() {
        return List.copyOf(weakActiveParticipants.keySet());
    }

    public List<EventParticipant> getParticipants() {
        return participants;
    }

    public EventParticipant getParticipantFromId(UUID uuid) {
        return weakParticipantMap.get(uuid);
    }

    public boolean isPlayerParticipating(Player player) {
        return weakParticipants.containsKey(player.getUuid());
    }

    public boolean isPlayerActivelyParticipating(Player player) {
        return weakActiveParticipants.containsKey(player);
    }

    public EventParticipant getParticipantFromActivePlayer(Player player) {
        return weakActiveParticipants.get(player);
    }

    public EventParticipant getParticipantFromPlayer(Player player) {
        return weakParticipants.get(player.getUuid());
    }

    public int getIndexOfParticipant(EventParticipant participant) {
        return participants.indexOf(participant);
    }
}