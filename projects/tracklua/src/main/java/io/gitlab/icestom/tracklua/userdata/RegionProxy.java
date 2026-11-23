package io.gitlab.icestom.tracklua.userdata;

import io.gitlab.icestom.lua.LuaMethod;
import io.gitlab.icestom.stomtrack.TrackRegion;

import java.util.List;

public class RegionProxy {
    private final TrackRegion region;
    private final List<String> tags;

    public RegionProxy(TrackRegion region, List<String> tags) {
        this.region = region;
        this.tags = tags;
    }

    @LuaMethod("tags")
    public List<String> getTags() {
        return tags;
    }
}
