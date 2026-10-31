package io.gitlab.icestom.icestom.event;

import io.gitlab.icestom.icestom.event.event.EventParticipant;

public interface LateJoinable {
    void joinLate(EventParticipant participant);
}
