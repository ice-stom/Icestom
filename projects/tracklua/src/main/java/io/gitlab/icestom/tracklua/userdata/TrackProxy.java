package io.gitlab.icestom.tracklua.userdata;

import io.gitlab.icestom.lua.LuaMethod;
import io.gitlab.icestom.stomtrack.type.Location;
import io.gitlab.icestom.tracklua.TrackHandle;

import java.util.UUID;

public class TrackProxy {
    private final TrackHandle handle;

    public TrackProxy(TrackHandle handle) {
        this.handle = handle;
    }

    @LuaMethod("data")
    public TrackDataProxy getData() {
        return new TrackDataProxy(handle.getTrackFile());
    }
}
