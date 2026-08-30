package io.gitlab.icestom.icestom.event.lua.adapter;

import net.hollowcube.luau.LuaState;

import java.util.ArrayList;
import java.util.List;

public interface LuaCodec<J> {
    J read(LuaState state, int index);

    void push(LuaState state, J value);

    default void pushList(LuaState state, List<? extends J> values) {
        state.createTable(values.size(), 0);

        for (int i = 0; i < values.size(); i++) {
            push(state, values.get(i));
            state.rawSetI(-2, i + 1);
        }
    }

    default List<J> readList(LuaState state, int index) {
        int n = state.len(index);
        List<J> values = new ArrayList<>(n);

        for (int i = 1; i <= n; i++) {
            state.rawGetI(index, i);
            values.add(read(state, -1));
            state.pop(1);
        }

        return values;
    }
}