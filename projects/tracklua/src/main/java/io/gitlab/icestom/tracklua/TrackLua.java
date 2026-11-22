package io.gitlab.icestom.tracklua;

import io.gitlab.icestom.lua.LuaPrint;
import io.gitlab.icestom.lua.SealedUserdataCodec;
import io.gitlab.icestom.lua.UserdataWrapper;
import io.gitlab.icestom.tracklua.userdata.*;
import net.hollowcube.luau.*;
import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static io.gitlab.icestom.lua.UserdataWrapper.erasedClass;

public class TrackLua implements Closeable {

    private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(2);

    private static final Logger log = LoggerFactory.getLogger(TrackLua.class);

    private static final LuauCompiler compiler = LuauCompiler.builder().build();

    private static final UserdataWrapper<TrackProxy> trackUserdataWrapper = new UserdataWrapper<>(erasedClass(TrackProxy.class));
    private static final UserdataWrapper<TrackDataProxy> trackDataUserdataWrapper = new UserdataWrapper<>(erasedClass(TrackDataProxy.class));
    private static final UserdataWrapper<LocationProxy> locationUserDataWrapper = new UserdataWrapper<>(erasedClass(LocationProxy.class));
    private static final UserdataWrapper<WorldProxy> worldUserDataWrapper = new UserdataWrapper<>(erasedClass(WorldProxy.class));
    private static final SealedUserdataCodec<LuaEventProxy> luaEventUserDataWrapper = SealedUserdataCodec.of(LuaEventProxy.class);

    private static final LuaState vm;

    static {
        vm = LuaState.newState();
        vm.openLibs(
                BuilinLibrary.BASE,
                BuilinLibrary.DEBUG,
                BuilinLibrary.COROUTINE,
                BuilinLibrary.MATH,
                BuilinLibrary.STRING,
                BuilinLibrary.TABLE,
                BuilinLibrary.BIT32
        );

        vm.newTable();
        vm.getGlobal("coroutine");
        vm.getField(-1, "yield");
        vm.setField(-3, "pullEvent");
        vm.pop(1);

        vm.setReadOnly(-1, true);
        vm.requireRegisterModule("@tracklua");

        trackUserdataWrapper.register(vm);
        trackDataUserdataWrapper.register(vm);
        locationUserDataWrapper.register(vm);
        luaEventUserDataWrapper.register(vm);
        worldUserDataWrapper.register(vm);

        vm.sandbox();
    }

    private final TrackLuaHandle handle;
    private final LuaState lua;
    private final int threadRef;

    private long deadline;
    private boolean running;
    private boolean finished;

    private final BatchQueue<LuaEventProxy> eventQueue = new BatchQueue<>();

    public boolean bump() {
        for (LuaEventProxy event : eventQueue.drain()) {
            if (resume(event)) return true;
        }

        return false;
    }

    public boolean isFinished() {
        return finished;
    }

    public void queueEvent(@NotNull LuaEventProxy event) {
        if (finished) return;
        eventQueue.add(event);
    }

    private boolean resume(@Nullable LuaEventProxy event) {
        if (finished) return true;
        try {
            if (event == null) {
                lua.pushNil();
            } else {
                luaEventUserDataWrapper.push(lua, event);
            }

            LuaStatus status;
            try {
                deadline = System.nanoTime() + TIMEOUT_NANOS;
                running = true;
                 status = lua.resume(null, 1);
            } catch (LuaError error) {
                finished = true;
                log.error("track script failed\n{}", LuaPrint.traceback(lua, error.getMessage()));
                return true;
            }

            switch (status) {
                case YIELD -> lua.top(0);
                case OK -> finished = true;
                default -> {
                    finished = true;
                    log.error("track script failed hard: \n{}", LuaPrint.traceback(lua, null));
                }
            }
        } finally {
            running = false;
        }

        return finished;
    }

    public TrackLua(TrackLuaHandle handle) throws LuauCompileException, IOException {

        this.handle = handle;

        this.lua = vm.newThread();
        this.threadRef = vm.ref(-1);
        vm.pop(1);

        lua.sandboxThread();

        lua.callbacks().preempt(LuaCallbacks.Preempt.allocate((state, gc) -> {
            if (running && System.nanoTime() >= deadline) {
                throw state.error("Script execution timed out (2 seconds)");
            }

            return false;
        }, Arena.global()));

        String trackId = handle.getTrackFile().getId();

        LuaPrint.register(lua, LoggerFactory.getLogger("LuaTrack@" + trackId));

        trackUserdataWrapper.push(lua, new TrackProxy(handle));
        lua.setGlobal("track");

        TrackRequireResolver trackRequireResolver = new TrackRequireResolver(handle.getFileSystem(), compiler, trackId);
        lua.openRequire(trackRequireResolver);

        Path entrypoint = handle.getFileSystem().root().resolve(trackId + ".luau");

        byte[] bytecode;
        try {
            bytecode = compiler.compile(Files.readAllBytes(entrypoint));
        } catch (LuauCompileException e) {
            log.error("Failed to compile entrypoint {}", entrypoint, e);
            close();
            return;
        }

        lua.load("@" + entrypoint, bytecode);

        resume(null);
    }

    @Override
    public void close() {
        vm.unref(threadRef);
    }
}
