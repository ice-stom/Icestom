package io.gitlab.icestom.icestom.event.lua;

import io.gitlab.icestom.icestom.event.EventParticipant;
import io.gitlab.icestom.icestom.event.EventStage;
import io.gitlab.icestom.icestom.event.IceStomEvent;
import io.gitlab.icestom.icestom.event.Result;
import io.gitlab.icestom.icestom.event.lua.adapter.LuaFunction;
import io.gitlab.icestom.icestom.event.lua.adapter.UserdataWrapper;
import net.hollowcube.luau.BuilinLibrary;
import net.hollowcube.luau.LuaState;
import net.kyori.adventure.key.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static io.gitlab.icestom.icestom.event.lua.adapter.UserdataWrapper.erasedClass;

public class LuaEvent<Participant extends EventParticipant> extends IceStomEvent<Participant> {

    private static final LuaState vm;
    private static final Logger log = LoggerFactory.getLogger(LuaEvent.class);

    private static final UserdataWrapper<Result<EventParticipant>> resultUserdataWrapper = new UserdataWrapper<>(erasedClass(Result.class));
    private static final UserdataWrapper<LuaEvent<?>> eventUserdataWrapper = new UserdataWrapper<>(erasedClass(LuaEvent.class));
    private static final UserdataWrapper<LuaStage> stageUserdataWrapper = new UserdataWrapper<>(erasedClass(LuaStage.class));
    private static final UserdataWrapper<LuaStageFuture> stageFutureUserdataWrapper = new UserdataWrapper<>(erasedClass(LuaStageFuture.class));

    static {
        vm = LuaState.newState();

        vm.openRequire(new LuaModuleResolver());
        vm.openLibs(BuilinLibrary.BASE);

        resultUserdataWrapper.register(vm);
        eventUserdataWrapper.register(vm);
        stageUserdataWrapper.register(vm);
        stageFutureUserdataWrapper.register(vm);

        vm.newTable();

        vm.setReadOnly(-1, true);
        vm.requireRegisterModule("@icestom");

        vm.sandbox();
    }

    private final LuaState lua;
    private Integer eventFn;

    public LuaEvent(String filename, byte[] bytecode) {
        lua = vm.newThread();
        lua.sandboxThread();

        LuaPrint.register(lua, LoggerFactory.getLogger("LuaEvent." + filename));

        lua.load("@" + filename, bytecode);

        lua.newTable();
        lua.pushString("track");
        lua.pushString("icestom_test");
        lua.setTable(-3);

        lua.call(1, 2);

        int manifestIndex = lua.absIndex(-2);
        int eventIndex = lua.absIndex(-1);

        final Map<String, Object> manifest = new HashMap<>();

        lua.pushNil();

        while (lua.next(manifestIndex)) {
            String key = lua.checkString(-2);

            Object value = switch (lua.type(-1)) {
                case BOOLEAN -> lua.toBoolean(-1);
                case NUMBER -> lua.toNumber(-1);
                case INTEGER -> lua.toInteger(-1);
                case STRING -> lua.toString(-1);
                default -> throw new InvalidEventManifestException(
                        "Unsupported type for key " + key
                );
            };

            manifest.put(key, value);

            lua.pop(1);
        }

        eventFn = lua.ref(eventIndex);

        lua.pop(2);
        vm.pop(1);
    }

    @LuaFunction("makeStage")
    public LuaStage makeStage(String key, Map<String, Object> options) {
        Key namespacedKey = Key.key(key);

        EventStage stage = EventStage.makeStage(namespacedKey, options).join();

        return new LuaStage(stage);
    }

    @Override
    public CompletableFuture<List<Result<EventParticipant>>> begin(
            List<Result<EventParticipant>> results
    ) {
        return CompletableFuture.supplyAsync(() -> {

            lua.getRef(eventFn);

            log.info("Event FN: {}", eventFn);

            eventUserdataWrapper.push(lua, this);

            resultUserdataWrapper.pushList(
                    lua,
                    results
            );

            lua.call(2, 1);

            List<Result<EventParticipant>> output = resultUserdataWrapper.readList(
                    lua,
                    -1
            );

            lua.pop(1);

            return output;
        });
    }

    @Override
    public void cleanup() {
        // TODO!!!
    }

    @Override
    public void close() {
        if (eventFn != null) {
            lua.unref(eventFn);
            eventFn = null;
        }
    }

    public static class InvalidEventManifestException extends RuntimeException {
        public InvalidEventManifestException(String message) {
            super(message);
        }
    }
}
