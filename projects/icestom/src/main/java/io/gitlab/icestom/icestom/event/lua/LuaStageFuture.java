package io.gitlab.icestom.icestom.event.lua;

import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.event.Result;
import io.gitlab.icestom.lua.LuaMethod;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LuaStageFuture {

    private final CompletableFuture<List<Result<EventParticipant>>> future;

    LuaStageFuture(CompletableFuture<List<Result<EventParticipant>>> future) {
        this.future = future;
    }

    @LuaMethod
    public List<Result<EventParticipant>> join() {
        return future.join();
    }
}
