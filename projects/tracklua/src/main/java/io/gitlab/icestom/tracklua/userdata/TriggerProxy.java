package io.gitlab.icestom.tracklua.userdata;

import io.gitlab.icestom.lua.LuaMethod;
import io.gitlab.icestom.stomtrack.TrackTrigger;

import java.util.List;

public class TriggerProxy {
    private final TrackTrigger trigger;
    private final List<String> tags;

    public TriggerProxy(TrackTrigger trigger, List<String> tags) {
        this.trigger = trigger;
        this.tags = tags;
    }

    @LuaMethod("tags")
    public List<String> getTags() {
        return tags;
    }
}
