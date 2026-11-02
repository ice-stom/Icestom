package io.gitlab.icestom.icestom.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.event.stage.EventStage;
import io.gitlab.icestom.icestom.event.event.IceStomEvent;
import io.gitlab.icestom.icestom.event.stage.StageOption;
import io.gitlab.icestom.icestom.event.stage.StageSchema;
import io.gitlab.icestom.icestom.event.Stateful;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class Snapshots {

    private Snapshots() {}

    static JsonObject state() {
        JsonObject root = new JsonObject();

        JsonArray events = new JsonArray();
        Set<Player> busy = new HashSet<>();

        for (IceStomEvent<EventParticipant> event : IceStom.getInstance().getEventManager().getActiveEvents()) {
            events.add(event(event, busy));
        }

        root.add("events", events);

        JsonArray players = new JsonArray();

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            JsonObject json = new JsonObject();
            json.addProperty("uuid", player.getUuid().toString());
            json.addProperty("name", player.getUsername());
            json.addProperty("inEvent", busy.contains(player));

            players.add(json);
        }

        root.add("players", players);

        return root;
    }

    private static JsonObject event(IceStomEvent<EventParticipant> event, Set<Player> busy) {
        JsonObject json = new JsonObject();
        json.addProperty("id", event.getId().toString());
        json.addProperty("name", event.getStageName());
        json.addProperty("running", event.isRunning());

        JsonArray stages = new JsonArray();

        for (EventStage stage : event.getLoadedStages()) {
            stages.add(stage(stage, busy));
        }

        json.add("stages", stages);

        return json;
    }

    private static JsonObject stage(EventStage stage, Set<Player> busy) {
        JsonObject json = new JsonObject();
        json.addProperty("name", stage.getStageName());

        Key type = IceStom.getInstance().getStageRegistry().getKey(stage.getClass());
        json.addProperty("type", type == null ? null : type.asString());

        JsonArray transitions = new JsonArray();

        if (stage instanceof Stateful<?> stateful) {
            Object current = stateful.getState();
            json.addProperty("state", String.valueOf(current));

            for (Stateful.StateChange<?> change : stateful.getStageChanges()) {
                if (change.before() != current) continue;

                JsonObject transition = new JsonObject();
                transition.addProperty("name", change.name());
                transition.addProperty("to", String.valueOf(change.after()));

                transitions.add(transition);
            }
        } else {
            json.add("state", null);
        }

        json.add("transitions", transitions);

        JsonArray players = new JsonArray();

        for (Player player : stage.getPlayers()) {
            busy.add(player);

            JsonObject entry = new JsonObject();
            entry.addProperty("uuid", player.getUuid().toString());
            entry.addProperty("name", player.getUsername());

            players.add(entry);
        }

        json.add("players", players);

        return json;
    }

    static JsonObject bootstrap(PanelSessions.Session session) {
        JsonObject root = new JsonObject();

        JsonObject server = new JsonObject();
        server.addProperty("version", IceStom.VERSION);
        server.addProperty("brand", MinecraftServer.getBrandName());
        root.add("server", server);

        JsonObject sessionJson = new JsonObject();
        sessionJson.addProperty("owner", session.ownerName());
        sessionJson.addProperty("expiresAt", session.expiresAt().toString());
        root.add("session", sessionJson);

        root.add("stages", stageSchemas());

        JsonArray tracks = new JsonArray();
        IceStom.getInstance().getTrackLibrary().getAvailableTracks()
                .stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(tracks::add);
        root.add("tracks", tracks);

        JsonArray definitions = new JsonArray();
        for (String name : IceStom.getInstance().getEventManager().getEventDefinitions()) {
            definitions.add(definition(name));
        }
        root.add("definitions", definitions);

        return root;
    }

    static JsonObject definition(String name) {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);

        EventDocument document = documentFor(name);

        json.addProperty("editable", document != null);
        json.add("document", document == null ? null : document.toJson());
        json.addProperty("title", document == null ? name : document.name());

        return json;
    }

    private static @Nullable EventDocument documentFor(String name) {
        try {
            return LuauWriter.read(IceStom.getInstance().getEventManager().readDefinition(name));
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonArray stageSchemas() {
        List<Key> keys = new ArrayList<>(IceStom.getInstance().getStageRegistry().getKeys());
        keys.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.asString(), b.asString()));

        JsonArray array = new JsonArray();

        for (Key key : keys) {
            StageSchema schema = IceStom.getInstance().getStageRegistry().getSchema(key);

            if (schema == null) continue;

            JsonObject json = new JsonObject();
            json.addProperty("type", key.asString());
            json.addProperty("label", schema.label());
            json.addProperty("description", schema.description());

            JsonArray options = new JsonArray();

            for (StageOption option : schema.options()) {
                options.add(option(option));
            }

            json.add("options", options);
            array.add(json);
        }

        return array;
    }

    private static JsonObject option(StageOption option) {
        JsonObject json = new JsonObject();
        json.addProperty("name", option.name());
        json.addProperty("type", option.type().name().toLowerCase(java.util.Locale.ROOT));
        json.addProperty("label", option.label());
        json.addProperty("description", option.description());
        json.addProperty("required", option.required());
        json.addProperty("min", option.min());
        json.addProperty("max", option.max());

        Object defaultValue = option.defaultValue();

        if (defaultValue instanceof Boolean bool) json.addProperty("default", bool);
        else if (defaultValue instanceof Number number) json.addProperty("default", number);
        else if (defaultValue != null) json.addProperty("default", String.valueOf(defaultValue));
        else json.add("default", null);

        return json;
    }
}
