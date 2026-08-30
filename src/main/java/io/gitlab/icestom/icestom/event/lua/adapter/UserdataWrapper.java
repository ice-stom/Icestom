package io.gitlab.icestom.icestom.event.lua.adapter;

import io.gitlab.icestom.icestom.IceStom;
import net.hollowcube.luau.LuaFunc;
import net.hollowcube.luau.LuaState;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserdataWrapper<T> implements LuaCodec<T> {

    private final Class<T> tClass;
    private final String metatable;
    private final Map<String, LuaFunc> library = new HashMap<>();

    public UserdataWrapper(Class<T> tClass) {
        this.tClass = tClass;
        this.metatable = Key.key(IceStom.NAMESPACE, tClass.getSimpleName().toLowerCase(Locale.ROOT)).asString();

        for (Method method : tClass.getDeclaredMethods()) {
            LuaFunction annotation = method.getAnnotation(LuaFunction.class);
            if (annotation == null) continue;

            if (Modifier.isStatic(method.getModifiers())) {
                throw new IllegalStateException("@LuaFunction " + method + " must be an instance method");
            }
            method.setAccessible(true);
            String name = annotation.value().isEmpty() ? method.getName() : annotation.value();

            LuaFunc func = isRaw(method)
                    ? LuaFunc.wrap(state -> invokeRaw(method, state), name)
                    : LuaFunc.wrap(state -> invokeMarshalled(method, state), name);

            if (library.put(name, func) != null) {
                throw new IllegalStateException("Duplicate @LuaFunction name '" + name + "' on " + tClass);
            }
        }

        LuaCodecs.register(tClass, this);
    }

    private static boolean isRaw(Method method) {
        return method.getReturnType() == int.class
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == LuaState.class;
    }

    private int invokeRaw(Method method, LuaState state) {
        T self = check(state, 1);
        try {
            return (int) method.invoke(self, state);
        } catch (InvocationTargetException e) {
            throw unwrap(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private int invokeMarshalled(Method method, LuaState state) {
        T self = check(state, 1);

        Type[] paramTypes = method.getGenericParameterTypes();
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            args[i] = readArg(state, paramTypes[i], i + 2);
        }

        Object result;
        try {
            result = method.invoke(self, args);
        } catch (InvocationTargetException e) {
            throw unwrap(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return pushResult(state, method.getGenericReturnType(), result);
    }

    private static RuntimeException unwrap(InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException re) return re;
        if (cause instanceof Error err) throw err;
        return new RuntimeException(cause);
    }

    private static Object readArg(LuaState state, Type type, int index) {
        if (type instanceof ParameterizedType pt && pt.getRawType() == List.class) {
            Class<?> element = rawClass(pt.getActualTypeArguments()[0]);
            LuaCodec<Object> codec = LuaCodecs.forClass(element);

            int n = state.len(index);
            List<Object> list = new ArrayList<>(n);
            for (int i = 1; i <= n; i++) {
                state.rawGetI(index, i);
                list.add(codec.read(state, -1));
                state.pop(1);
            }
            return list;
        }

        Class<?> raw = rawClass(type);
        if (raw == LuaState.class) {
            throw new IllegalStateException(
                    "A LuaState parameter only works in raw mode (int method(LuaState)); "
                            + "this method's return type isn't int, so it fell into marshalled mode instead");
        }
        return LuaCodecs.forClass(raw).read(state, index);
    }

    private static int pushResult(LuaState state, Type type, Object value) {
        if (type == void.class) return 0;

        if (type instanceof ParameterizedType pt && pt.getRawType() == List.class) {
            Class<?> element = rawClass(pt.getActualTypeArguments()[0]);
            LuaCodec<Object> codec = LuaCodecs.forClass(element);

            List<?> list = (List<?>) value;
            state.createTable(list.size(), 0);
            for (int i = 0; i < list.size(); i++) {
                codec.push(state, list.get(i));
                state.rawSetI(-2, i + 1);
            }
            return 1;
        }

        LuaCodecs.forClass(rawClass(type)).push(state, value);
        return 1;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
        throw new IllegalStateException("Unsupported @LuaFunction type: " + type);
    }

    public void register(LuaState lua) {
        if (!lua.newMetaTable(metatable)) {
            lua.pop(1);
            return;
        }

        lua.pushString(tClass.getSimpleName());
        lua.setField(-2, "__type");

        lua.newTable();
        lua.register(library);
        lua.setField(-2, "__index");

        lua.pop(1);
    }

    @Override
    public void push(LuaState lua, T value) {
        lua.newUserData(value);
        lua.getMetaTable(metatable);
        lua.setMetaTable(-2);
    }

    @Override
    public T read(LuaState lua, int index) {
        return check(lua, index);
    }

    @SuppressWarnings("unchecked")
    public T check(LuaState lua, int index) {
        return (T) lua.checkUserData(index, metatable);
    }

    @SuppressWarnings("unchecked")
    public @Nullable T tryRead(LuaState lua, int index) {
        if (!lua.isUserData(index)) return null;
        int abs = lua.absIndex(index);

        if (!lua.getMetaTable(abs)) return null;
        lua.getMetaTable(metatable);
        boolean match = lua.rawEqual(-1, -2);
        lua.pop(2);

        return match ? (T) lua.toUserData(abs) : null;
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> erasedClass(Class<?> type) {
        return (Class<T>) type;
    }
}