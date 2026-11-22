package io.gitlab.icestom.tracklua.userdata;

import io.gitlab.icestom.lua.LuaMethod;
import io.gitlab.icestom.tracklua.TrackLuaHandle;
import io.gitlab.icestom.tracklua.WorldLuaHandle;
import net.hollowcube.luau.LuaError;

public class WorldProxy {
    private final WorldLuaHandle handle;

    public WorldProxy(WorldLuaHandle handle) {
        this.handle = handle;
    }

    @LuaMethod("writeable")
    public boolean getWriteable() {
        return handle.getWriteable();
    }

    @LuaMethod("get_block")
    public String getBlock(int x, int y, int z) {
        return handle.getBlock(x, y, z);
    }

    @LuaMethod("set_block")
    public void setBlock(int x, int y, int z, String material) {
        try {
            handle.setBlock(x, y, z, material);
        } catch (WorldLuaHandle.UnwritableWorldException e) {
            throw new LuaError("Attempt to write to unwritable world.");
        }
    }
}
