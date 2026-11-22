package io.gitlab.icestom.tracklua.userdata;

import io.gitlab.icestom.lua.LuaMetaMethod;
import io.gitlab.icestom.lua.LuaMethod;

import java.util.UUID;

public sealed interface LuaEventProxy permits LuaEventProxy.CheckpointEventProxy {

    @LuaMethod("event")
    String event();

    @LuaMetaMethod("__tostring")
    default String __tostring() {
        // this is probably terrible (stealing from the record tostring)
        return this.event() + "[" + this.toString().split("\\[")[1];
    }

    record CheckpointEventProxy(UUID player, int index) implements LuaEventProxy {
        @Override
        public String event() {
            return "icestom:checkpoint";
        }

        @LuaMethod("player")
        @Override
        public UUID player() {
            return player;
        }

        @LuaMethod("index")
        @Override
        public int index() {
            return index;
        }
    }
}