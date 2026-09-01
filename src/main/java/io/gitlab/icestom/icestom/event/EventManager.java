package io.gitlab.icestom.icestom.event;

import io.gitlab.icestom.icestom.event.lua.LuaEvent;
import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class EventManager {

    private final Path folder;
    private final LuauCompiler compiler = LuauCompiler.builder().build();

    private final Set<IceStomEvent<EventParticipant>> events = new HashSet<>();

    public EventManager(Path folder) {
        this.folder = folder;

        boolean _ = folder.toFile().mkdirs();
    }

    public Set<IceStomEvent<EventParticipant>> getActiveEvents() {
        return events;
    }

    public LuaEvent<EventParticipant> loadLuauEvent(String name) throws IOException, LuauCompileException {
        byte[] source = Files.readAllBytes(folder.resolve(name));
        byte[] bytecode = compiler.compile(source);

        LuaEvent<EventParticipant> luaEvent = new LuaEvent<>(name, bytecode);

        events.add(luaEvent);

        luaEvent.futureResultsFuture.thenAccept(resultsFuture -> resultsFuture.thenRun(() -> {
            luaEvent.cleanup();
            events.remove(luaEvent);
        }));

        return luaEvent;
    }

    public List<String> getEventDefinitions() {
        return Arrays.stream(Objects.requireNonNull(folder.toFile().listFiles((file, name) ->
                name.endsWith(".lua") || name.endsWith(".luau")
        ))).map(File::getName).toList();
    }
}
