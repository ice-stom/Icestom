package io.gitlab.icestom.icestom.event.lua;

import net.hollowcube.luau.LuaFunc;
import net.hollowcube.luau.LuaState;
import org.slf4j.Logger;

public final class LuaPrint {
    private LuaPrint() {}

    public static void register(LuaState state, Logger logger) {
        state.pushFunction(LuaFunc.wrap(s -> {
            int n = s.top();
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= n; i++) {
                if (i > 1) sb.append('\t');
                sb.append(s.toStringRepr(i));
            }
            logger.info(sb.toString());
            return 0;
        }, "print"));
        state.setGlobal("print");
    }
}