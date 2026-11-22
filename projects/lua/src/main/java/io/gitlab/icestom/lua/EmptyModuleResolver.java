package io.gitlab.icestom.lua;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.require.RequireResolver;
import org.jetbrains.annotations.NotNull;

public final class EmptyModuleResolver implements RequireResolver {

    @Override
    public @NotNull Result reset(@NotNull LuaState state, @NotNull String requirerChunkName) {
        return Result.NOT_FOUND;
    }

    @Override
    public @NotNull Result toParent(@NotNull LuaState state) {
        return Result.NOT_FOUND;
    }

    @Override
    public @NotNull Result toChild(@NotNull LuaState state, @NotNull String name) {
        return Result.NOT_FOUND;
    }

    @Override
    public @NotNull Result jumpToAlias(@NotNull LuaState state, @NotNull String aliasPath) {
        return Result.NOT_FOUND;
    }

    @Override
    public @NotNull Result getConfigStatus(@NotNull LuaState state) {
        return Result.NOT_FOUND;
    }

    @Override
    public String resolveAlias(@NotNull LuaState state, @NotNull String alias) {
        return null;
    }

    @Override
    public Module getModule(@NotNull LuaState state) {
        return null;
    }

    @Override
    public int load(
            @NotNull LuaState state,
            @NotNull String path,
            @NotNull String chunkName,
            @NotNull String loadName
    ) {
        return 0;
    }
}