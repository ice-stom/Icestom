package io.gitlab.icestom.lua;

import net.hollowcube.luau.LuaState;

import java.util.ArrayList;
import java.util.List;

public final class SealedUserdataCodec<T> implements LuaCodec<T> {

    private final Class<T> base;
    private final List<UserdataWrapper<? extends T>> variants;

    private SealedUserdataCodec(Class<T> base, List<UserdataWrapper<? extends T>> variants) {
        this.base = base;
        this.variants = variants;
        LuaCodecs.register(base, this);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> SealedUserdataCodec<T> of(Class<T> base) {
        if (!base.isSealed()) {
            throw new IllegalArgumentException(base + " is not sealed");
        }
        List<UserdataWrapper<? extends T>> variants = new ArrayList<>();
        for (Class<?> sub : base.getPermittedSubclasses()) {
            variants.add(new UserdataWrapper(sub));
        }
        return new SealedUserdataCodec<>(base, variants);
    }

    public void register(LuaState lua) {
        for (var v : variants) v.register(lua);
    }

    @Override
    public T read(LuaState lua, int index) {
        for (var v : variants) {
            T t = v.tryRead(lua, index);
            if (t != null) return t;
        }
        throw new IllegalArgumentException(
                "Argument " + index + " is not a " + base.getSimpleName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void push(LuaState lua, T value) {
        for (var v : variants) {
            if (v.tClass().isInstance(value)) {
                ((UserdataWrapper<T>) v).push(lua, value);
                return;
            }
        }
        throw new IllegalArgumentException(
                "No wrapper registered for " + value.getClass().getName() + " under " + base.getSimpleName());
    }
}