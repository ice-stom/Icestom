package io.gitlab.icestom.lua;

import net.hollowcube.luau.LuaFunc;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class UserdataWrapper<T> implements LuaCodec<T> {

    private final Class<T> tClass;
    private final String metatable;
    private final Map<String, LuaFunc> library = new LinkedHashMap<>();
    private final Map<String, LuaFunc> metaLibrary = new LinkedHashMap<>();

    private static final Set<String> RESERVED = Set.of("__index", "__type");

    public UserdataWrapper(Class<T> tClass) {
        this.tClass = tClass;
        this.metatable = "icestom:" + tClass.getName().toLowerCase(Locale.ROOT);

        for (var e : collect(tClass, LuaMethod.class).entrySet()) {
            Method method = e.getKey();
            String name = e.getValue().value().isEmpty() ? method.getName() : e.getValue().value();
            requireInstance(method, "@LuaMethod");
            method.setAccessible(true);

            if (library.put(name, wrap(method, name)) != null) {
                throw new IllegalStateException("Duplicate @LuaMethod name '" + name + "' on " + tClass);
            }
        }

        for (var e : collect(tClass, LuaMetaMethod.class).entrySet()) {
            Method method = e.getKey();
            String name = e.getValue().value().isEmpty() ? method.getName() : e.getValue().value();
            requireInstance(method, "@LuaMetaMethod");

            if (!name.startsWith("__")) {
                throw new IllegalStateException("@LuaMetaMethod name must start with '__': " + method);
            }
            if (RESERVED.contains(name)) {
                throw new IllegalStateException(name + " is managed by UserdataWrapper: " + method);
            }
            method.setAccessible(true);

            if (metaLibrary.put(name, wrap(method, name)) != null) {
                throw new IllegalStateException("Duplicate @LuaMetaMethod name '" + name + "' on " + tClass);
            }
        }

        LuaCodecs.register(tClass, this);
    }

    public Class<T> tClass() {
        return tClass;
    }

    private static <A extends Annotation> Map<Method, A> collect(Class<?> root, Class<A> annotationType) {
        Map<String, Method> bySignature = new LinkedHashMap<>();
        Set<Class<?>> seen = new HashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            Class<?> c = queue.poll();
            if (c == Object.class || !seen.add(c)) continue;

            for (Method m : c.getDeclaredMethods()) {
                if (m.isBridge() || m.isSynthetic()) continue;
                if (m.getAnnotation(annotationType) == null) continue;
                bySignature.putIfAbsent(m.getName() + Arrays.toString(m.getParameterTypes()), m);
            }

            if (c.getSuperclass() != null) queue.add(c.getSuperclass());
            queue.addAll(Arrays.asList(c.getInterfaces()));
        }

        Map<Method, A> result = new LinkedHashMap<>();
        for (Method m : bySignature.values()) {
            result.put(m, m.getAnnotation(annotationType));
        }
        return result;
    }

    private static void requireInstance(Method method, String annotationName) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException(annotationName + " " + method + " must be an instance method");
        }
    }

    private LuaFunc wrap(Method method, String name) {
        return isRaw(method)
                ? LuaFunc.wrap(state -> invokeRaw(method, state), name)
                : LuaFunc.wrap(state -> invokeMarshalled(method, state), name);
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

        RuntimeException wrapped = new RuntimeException(String.valueOf(cause), cause);
        if (cause != null) wrapped.setStackTrace(cause.getStackTrace());
        return wrapped;
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

        if (value == null) {
            state.pushNil();
            return 1;
        }

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
        throw new IllegalStateException("Unsupported @LuaMethod type: " + type);
    }

    public void register(LuaState lua) {
        if (!lua.newMetaTable(metatable)) {
            lua.pop(1);
            return;
        }

        lua.pushString(tClass.getSimpleName());
        lua.setField(-2, "__type");

        for (var entry : metaLibrary.entrySet()) {
            lua.pushFunction(entry.getValue());
            lua.setField(-2, entry.getKey());
        }

        lua.newTable();
        lua.register(library);
        lua.setField(-2, "__index");

        lua.pop(1);
    }

    @Override
    public void push(LuaState lua, T value) {
        lua.newUserData(value);
        if (lua.getMetaTable(metatable) == LuaType.NIL) {
            lua.pop(2); // nil + userdata
            throw new IllegalStateException("Metatable " + metatable + " not registered; call register() first");
        }
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