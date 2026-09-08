package io.gitlab.icestom.icestom.event;

import io.gitlab.icestom.icestom.event.lua.LuaEvent;
import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class EventManager {

    private static final Pattern DEFINITION_NAME = Pattern.compile("[A-Za-z0-9_-]{1,64}[.]luau?");

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

    public @Nullable IceStomEvent<EventParticipant> getActiveEvent(UUID id) {
        return events.stream()
                .filter(event -> event.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public LuaEvent<EventParticipant> loadLuauEvent(String name) throws IOException, LuauCompileException {
        byte[] source = Files.readAllBytes(resolve(name));
        byte[] bytecode = compiler.compile(source);

        LuaEvent<EventParticipant> luaEvent = new LuaEvent<>(name, bytecode);

        events.add(luaEvent);

        luaEvent.futureResultsFuture.thenAccept(resultsFuture -> resultsFuture.thenRun(() -> {
            luaEvent.cleanup();
            events.remove(luaEvent);
        }));

        return luaEvent;
    }

    public boolean cancelEvent(UUID id) {
        IceStomEvent<EventParticipant> event = getActiveEvent(id);

        if (event == null || !event.isRunning()) return false;

        event.cleanup();
        events.remove(event);

        return true;
    }

    public List<String> getEventDefinitions() {
        File[] files = folder.toFile().listFiles((file, name) ->
                name.endsWith(".lua") || name.endsWith(".luau")
        );

        if (files == null) return List.of();

        return Arrays.stream(files).map(File::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public String readDefinition(String name) throws IOException {
        return Files.readString(resolve(name), StandardCharsets.UTF_8);
    }

    public void writeDefinition(String name, String source) throws IOException {
        Files.writeString(resolve(name), source, StandardCharsets.UTF_8);
    }

    public boolean deleteDefinition(String name) throws IOException {
        return Files.deleteIfExists(resolve(name));
    }

    public boolean definitionExists(String name) {
        return DEFINITION_NAME.matcher(name).matches() && Files.isRegularFile(folder.resolve(name));
    }

    public void compile(String source) throws LuauCompileException {
        byte[] _ = compiler.compile(source.getBytes(StandardCharsets.UTF_8));
    }

    private Path resolve(String name) {
        if (!DEFINITION_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Illegal event definition name: " + name);
        }

        return folder.resolve(name);
    }
}
