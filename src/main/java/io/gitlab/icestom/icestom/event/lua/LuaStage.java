package io.gitlab.icestom.icestom.event.lua;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.EventParticipant;
import io.gitlab.icestom.icestom.event.EventStage;
import io.gitlab.icestom.icestom.event.Result;
import io.gitlab.icestom.icestom.event.lua.adapter.LuaFunction;

import java.util.List;

public class LuaStage {

    private final EventStage stage;

    LuaStage(EventStage stage) {
        this.stage = stage;
    }

    @LuaFunction("get_key")
    public String getKey() {
        return IceStom.getInstance().getStageRegistry().getKey(getClass()).toString();
    }

    @LuaFunction("begin")
    public LuaStageFuture begin(List<Result<EventParticipant>> results) {
        return new LuaStageFuture(stage.begin(results));
    }
}
