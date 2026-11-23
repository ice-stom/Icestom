package io.gitlab.icestom.tracklua.userdata;

import io.gitlab.icestom.lua.LuaMethod;
import io.gitlab.icestom.stomtrack.TrackFile;
import io.gitlab.icestom.stomtrack.type.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class TrackDataProxy {
    private final TrackFile trackFile;

    public TrackDataProxy(TrackFile trackFile) {
        this.trackFile = trackFile;
    }

    @LuaMethod("id")
    public @NotNull String getId() {
        return trackFile.getId();
    }

    @LuaMethod("looped")
    public boolean isLooped() {
        return trackFile.isLooped();
    }

    @LuaMethod("spawn_location")
    public @NotNull LocationProxy getSpawnLocation() {
        return new LocationProxy(trackFile.getSpawnLocation());
    }

    @LuaMethod("grid")
    public @NotNull List<LocationProxy> getGrid() {
        return trackFile.getGrid().stream().map(LocationProxy::new).toList();
    };

    @LuaMethod("location")
    public @Nullable LocationProxy getLocation(String name) {
        Location location = trackFile.getLocations().get(name);
        return location == null ? null : new LocationProxy(location);
    }

    @LuaMethod("tags")
    public @NotNull Map<String, String> getTags() {
        return trackFile.getTags();
    }
}
