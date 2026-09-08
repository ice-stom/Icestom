package io.gitlab.icestom.icestom.event.lua.adapter;

import net.hollowcube.luau.LuaState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LuaCodecs {
    private static final Map<Class<?>, LuaCodec<?>> CODECS = new ConcurrentHashMap<>();

    static {
        register(String.class, new LuaCodec<String>() {
            public String read(LuaState state, int index) { return state.checkString(index); }
            public void push(LuaState state, String value) { state.pushString(value); }
        });

        LuaCodec<Boolean> bool = new LuaCodec<>() {
            public Boolean read(LuaState state, int index) { return state.checkBoolean(index); }
            public void push(LuaState state, Boolean value) { state.pushBoolean(value); }
        };
        register(boolean.class, bool);
        register(Boolean.class, bool);

        LuaCodec<Integer> integer = new LuaCodec<>() {
            public Integer read(LuaState state, int index) { return state.checkInteger(index); }
            public void push(LuaState state, Integer value) { state.pushInteger(value); }
        };
        register(int.class, integer);
        register(Integer.class, integer);

        LuaCodec<Long> longCodec = new LuaCodec<>() {
            public Long read(LuaState state, int index) { return state.checkInteger64(index); }
            public void push(LuaState state, Long value) { state.pushInteger64(value); }
        };
        register(long.class, longCodec);
        register(Long.class, longCodec);

        LuaCodec<Double> doubleCodec = new LuaCodec<>() {
            public Double read(LuaState state, int index) { return state.checkNumber(index); }
            public void push(LuaState state, Double value) { state.pushNumber(value); }
        };
        register(double.class, doubleCodec);
        register(Double.class, doubleCodec);

        register(Map.class, new LuaCodec<>() {
            @Override
            public Map<String, Object> read(LuaState state, int index) {
                int absIndex = state.absIndex(index);
                Map<String, Object> map = new HashMap<>();

                state.pushNil();
                while (state.next(absIndex)) {
                    String key = state.checkString(-2);

                    Object value = switch (state.type(-1)) {
                        case BOOLEAN -> state.toBoolean(-1);
                        case NUMBER -> state.toNumber(-1);
                        case INTEGER -> state.toInteger(-1);
                        case STRING -> state.toString(-1);
                        default -> throw new IllegalStateException("Unsupported Map value type: " + state.type(-1));
                    };

                    map.put(key, value);
                    state.pop(1);
                }
                return map;
            }

            @Override
            public void push(LuaState state, Map map) {
                state.newTable();
                for (Object entryObj : map.entrySet()) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObj;
                    state.pushString(String.valueOf(entry.getKey()));

                    Object val = entry.getValue();
                    if (val instanceof Boolean b) state.pushBoolean(b);
                    else if (val instanceof Integer i) state.pushInteger(i);
                    else if (val instanceof Long l) state.pushInteger64(l);
                    else if (val instanceof Double d) state.pushNumber(d);
                    else if (val instanceof Float f) state.pushNumber(f);
                    else if (val instanceof String s) state.pushString(s);
                    else throw new IllegalStateException("Unsupported Java Map value type: " + (val == null ? "null" : val.getClass()));

                    state.setTable(-3);
                }
            }
        });
    }

    private LuaCodecs() {}

    public static <J> void register(Class<J> type, LuaCodec<J> codec) {
        CODECS.put(type, codec);
    }

    @SuppressWarnings("unchecked")
    static LuaCodec<Object> forClass(Class<?> type) {
        LuaCodec<?> codec = CODECS.get(type);
        if (codec == null) {
            throw new IllegalStateException(
                    "No LuaCodec for " + type.getName()
                            + " -- wrap it with a UserdataWrapper (it registers itself as a codec), "
                            + "or add one manually via LuaCodecs.register(...)");
        }
        return (LuaCodec<Object>) codec;
    }
}