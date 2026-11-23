package io.gitlab.icestom.icestom.instance.proxy;

import io.gitlab.icestom.tracklua.WorldLuaHandle;
import net.minestom.server.instance.Instance;

public class LuaInstanceContainer implements WorldLuaHandle {

    public final Instance instance;

    public LuaInstanceContainer(Instance instance) {
        this.instance = instance;
    }

    @Override
    public boolean getWriteable() {
        return false;
    }

    @Override
    public String getBlock(int x, int y, int z) {
        return instance.getBlock(x, y, z).key().toString();
    }

    @Override
    public void setBlock(int x, int y, int z, String material) throws UnwritableWorldException {
        throw new UnwritableWorldException();
    }
}
