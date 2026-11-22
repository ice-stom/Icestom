package io.gitlab.icestom.tracklua.userdata;

import io.gitlab.icestom.lua.LuaMethod;
import io.gitlab.icestom.tracklua.TrackLuaHandle;

public class TrackProxy {
    private final TrackLuaHandle handle;

    public TrackProxy(TrackLuaHandle handle) {
        this.handle = handle;
    }

    @LuaMethod("data")
    public TrackDataProxy getData() {
        return new TrackDataProxy(handle.getTrackFile());
    }

    @LuaMethod("world")
    public WorldProxy getWorld() {
        return new WorldProxy(handle.getWorld());
    }

    @LuaMethod("subscribe_trigger")
    public void subscribeTrigger(String tag) {
        handle.subscribeTrigger(tag);
    }

    @LuaMethod("subscribe_region")
    public void subscribeRegion(String tag) {
        handle.subscribeRegion(tag);
    }

    @LuaMethod("unsubscribe_trigger")
    public void unsubscribeTrigger(String tag) {
        handle.unsubscribeTrigger(tag);
    }

    @LuaMethod("unsubscribe_region")
    public void unsubscribeRegion(String tag) {
        handle.unsubscribeRegion(tag);
    }
}
