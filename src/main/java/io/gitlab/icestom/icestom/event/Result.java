package io.gitlab.icestom.icestom.event;

import net.kyori.adventure.key.Key;

import java.util.HashMap;
import java.util.Map;

public class Result<Participant extends EventParticipant> {
    private final Participant participant;

    private final Map<Key, Object> fields = new HashMap<>();

    public Result(Participant participant) {
        this.participant = participant;
    }

    Participant getParticipant() {
        return participant;
    }

    public void set(Key key, Object value) {
        fields.put(key, value);
    }

    public Object get(Key key) {
        return fields.get(key);
    }
}
