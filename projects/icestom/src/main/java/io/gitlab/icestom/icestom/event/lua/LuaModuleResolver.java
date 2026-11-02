package io.gitlab.icestom.icestom.event.lua;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.require.RequireResolver;

public final class LuaModuleResolver implements RequireResolver {

    @Override
    public Result reset(LuaState state, String requirerChunkName) {
        return Result.NOT_FOUND;
    }

    @Override
    public Result toParent(LuaState state) {
        return Result.NOT_FOUND;
    }

    @Override
    public Result toChild(LuaState state, String name) {
        return Result.NOT_FOUND;
    }

    @Override
    public Result jumpToAlias(LuaState state, String aliasPath) {
        return Result.NOT_FOUND;
    }

    @Override
    public Result getConfigStatus(LuaState state) {
        return Result.NOT_FOUND;
    }

    @Override
    public String resolveAlias(LuaState state, String alias) {
        return null;
    }

    @Override
    public Module getModule(LuaState state) {
        return null;
    }

    @Override
    public int load(
            LuaState state,
            String path,
            String chunkName,
            String loadName
    ) {
        return 0;
    }
}