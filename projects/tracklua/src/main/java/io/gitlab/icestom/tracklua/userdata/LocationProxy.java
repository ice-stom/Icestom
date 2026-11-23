package io.gitlab.icestom.tracklua.userdata;

import io.gitlab.icestom.lua.LuaMethod;
import io.gitlab.icestom.lua.LuaMetaMethod;
import io.gitlab.icestom.stomtrack.type.Location;

public class LocationProxy {
    private final Location location;

    public LocationProxy(Location location) {
        this.location = location;
    }

    @LuaMethod("x")
    public double getX() {
        return location.x();
    }

    @LuaMethod("y")
    public double getY() {
        return location.y();
    }

    @LuaMethod("z")
    public double getZ() {
        return location.z();
    }

    @LuaMethod("yaw")
    public float getYaw() {
        return location.yaw();
    }

    @LuaMethod("pitch")
    public float getPitch() {
        return location.pitch();
    }

    @LuaMetaMethod("__tostring")
    public String toString() {
        return location.toString();
    }
}
