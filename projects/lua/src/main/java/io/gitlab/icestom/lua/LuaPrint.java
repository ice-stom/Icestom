package io.gitlab.icestom.lua;

import net.hollowcube.luau.LuaError;
import net.hollowcube.luau.LuaFunc;
import net.hollowcube.luau.LuaState;
import org.slf4j.Logger;

public final class LuaPrint {
    private LuaPrint() {}

    public static void register(LuaState state, Logger logger) {
        int infoRef = -1;
        int base = state.top();
        state.getGlobal("debug");
        if (state.isTable(-1)) {
            state.getField(-1, "info");
            if (state.isFunction(-1)) infoRef = state.ref(-1);
        }
        state.top(base);
        final int ref = infoRef;

        state.pushFunction(LuaFunc.wrap(s -> {
            int n = s.top();

            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= n; i++) {
                if (i > 1) sb.append(' ');
                sb.append(s.toStringRepr(i));
            }

            String location = null;
            if (ref != -1) {
                s.getRef(ref);
                s.pushInteger(2);
                s.pushString("sl");
                s.call(2, 2);
                String src = s.toString(-2);
                int line = s.toInteger(-1);
                if (src != null && line > 0) location = src + ":" + line;
                s.top(n);
            }

            if (location != null) {
                logger.info("{} {}", location, sb);
            } else {
                logger.info(sb.toString());
            }
            return 0;
        }, "print"));
        state.setGlobal("print");
    }

    public static String traceback(LuaState co) {
        String msg = co.toStringRepr(-1);
        LuaState main = co.mainThread();
        int base = main.top();
        try {
            main.getGlobal("debug");
            main.getField(-1, "traceback");
            main.remove(-2);
            main.pushThread(co);
            main.pushString(msg);
            main.call(2, 1);
            String trace = main.toString(-1);
            return trace != null ? trace : msg;
        } catch (LuaError e) {
            return msg;
        } finally {
            main.top(base);
        }
    }
}