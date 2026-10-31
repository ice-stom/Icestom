package io.gitlab.icestom.icestom.event;

import io.gitlab.icestom.icestom.event.lua.ParticipantStore;

public interface ParticipantStoreHolder {
    ParticipantStore getParticipants();
}
