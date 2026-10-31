package io.gitlab.icestom.icestom.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.event.EventManager;
import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.event.stage.EventStage;
import io.gitlab.icestom.icestom.event.event.IceStomEvent;
import io.gitlab.icestom.icestom.event.event.Result;
import io.gitlab.icestom.icestom.event.Stateful;
import io.gitlab.icestom.icestom.event.lua.LuaEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class PanelApi implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(PanelApi.class);

    private final PanelServer server;

    PanelApi(PanelServer server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            PanelSessions.Session session = server.sessions().validate(Http.bearerToken(exchange));

            if (session == null) {
                Http.sendError(exchange, 401, "That panel link has expired. Run /panel in game for a new one.");
                return;
            }

            route(exchange, session);
        } catch (Http.BadRequestException e) {
            Http.sendError(exchange, 400, e.getMessage());
        } catch (IllegalArgumentException e) {
            Http.sendError(exchange, 400, String.valueOf(e.getMessage()));
        } catch (Exception e) {
            log.error("Panel request {} {} failed", exchange.getRequestMethod(), exchange.getRequestURI(), e);

            try {
                Http.sendError(exchange, 500, "The server failed to handle that. Check the console for details.");
            } catch (IOException ignored) {
            }
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange, PanelSessions.Session session) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.startsWith("/api/")) path = path.substring("/api/".length());

        String method = exchange.getRequestMethod();
        String[] segments = path.isEmpty() ? new String[0] : path.split("/");

        for (int i = 0; i < segments.length; i++) {
            segments[i] = URLDecoder.decode(segments[i], StandardCharsets.UTF_8);
        }

        if (segments.length == 1 && segments[0].equals("bootstrap") && method.equals("GET")) {
            Http.sendJson(exchange, 200, Snapshots.bootstrap(session));
            return;
        }

        if (segments.length == 1 && segments[0].equals("state") && method.equals("GET")) {
            Http.sendJson(exchange, 200, Http.onTick(Snapshots::state));
            return;
        }

        if (segments.length == 1 && segments[0].equals("stream") && method.equals("GET")) {
            server.stream(exchange);
            return;
        }

        if (segments.length == 1 && segments[0].equals("definitions") && method.equals("POST")) {
            saveDefinition(exchange);
            return;
        }

        if (segments.length == 2 && segments[0].equals("definitions")) {
            switch (method) {
                case "GET" -> readDefinition(exchange, segments[1]);
                case "DELETE" -> deleteDefinition(exchange, segments[1]);
                default -> Http.sendError(exchange, 405, "Method not allowed");
            }
            return;
        }

        if (segments.length == 1 && segments[0].equals("events") && method.equals("POST")) {
            runEvent(exchange);
            return;
        }

        if (segments.length == 2 && segments[0].equals("events") && method.equals("DELETE")) {
            cancelEvent(exchange, segments[1]);
            return;
        }

        if (segments.length == 3 && segments[0].equals("events") && segments[2].equals("transition")
                && method.equals("POST")) {
            transition(exchange, segments[1]);
            return;
        }

        Http.sendError(exchange, 404, "No such endpoint");
    }

    private void saveDefinition(HttpExchange exchange) throws IOException {
        JsonObject body = Http.readJson(exchange);

        String name = normaliseName(Http.requireString(body, "name"));

        JsonElement documentElement = body.get("document");

        if (documentElement == null || !documentElement.isJsonObject()) {
            throw new Http.BadRequestException("Missing 'document' object");
        }

        EventDocument document = EventDocument.fromJson(documentElement.getAsJsonObject());
        String source = LuauWriter.write(document);

        EventManager events = IceStom.getInstance().getEventManager();

        try {
            events.compile(source);
        } catch (Exception e) {
            throw new Http.BadRequestException("The generated event didn't compile: " + e.getMessage());
        }

        events.writeDefinition(name, source);

        JsonObject response = Snapshots.definition(name);
        response.addProperty("source", source);

        Http.sendJson(exchange, 200, response);
    }

    private void readDefinition(HttpExchange exchange, String rawName) throws IOException {
        String name = normaliseName(rawName);
        EventManager events = IceStom.getInstance().getEventManager();

        if (!events.definitionExists(name)) {
            Http.sendError(exchange, 404, "No event definition called " + name);
            return;
        }

        JsonObject response = Snapshots.definition(name);
        response.addProperty("source", events.readDefinition(name));

        Http.sendJson(exchange, 200, response);
    }

    private void deleteDefinition(HttpExchange exchange, String rawName) throws IOException {
        String name = normaliseName(rawName);

        if (!IceStom.getInstance().getEventManager().deleteDefinition(name)) {
            Http.sendError(exchange, 404, "No event definition called " + name);
            return;
        }

        Http.send(exchange, 204, "application/json", new byte[0]);
    }

    private static String normaliseName(String name) {
        String trimmed = name.trim();

        if (trimmed.isEmpty()) throw new Http.BadRequestException("An event needs a file name");

        return trimmed.endsWith(".lua") || trimmed.endsWith(".luau") ? trimmed : trimmed + ".luau";
    }

    private void runEvent(HttpExchange exchange) throws IOException {
        JsonObject body = Http.readJson(exchange);

        String definition = normaliseName(Http.requireString(body, "definition"));
        List<UUID> participantIds = uuids(body.get("participants"));

        EventManager events = IceStom.getInstance().getEventManager();

        if (!events.definitionExists(definition)) {
            Http.sendError(exchange, 404, "No event definition called " + definition);
            return;
        }

        UUID id = Http.onTick(() -> {
            List<Result<EventParticipant>> results = new ArrayList<>();

            for (UUID uuid : participantIds) {
                Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);

                if (!(player instanceof EventParticipant participant)) {
                    throw new Http.BadRequestException("Player " + uuid + " is not online");
                }

                results.add(new Result<>(participant));
            }

            LuaEvent<EventParticipant> event;
            try {
                event = events.loadLuauEvent(definition);
            } catch (Exception e) {
                throw new Http.BadRequestException("Failed to load " + definition + ": " + e.getMessage());
            }

            event.begin(results).whenComplete((ignored, throwable) -> {
                if (throwable == null) return;

                Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();

                if (cause instanceof IceStomEvent.EventCancelledException) return;

                log.error("Event {} failed", definition, cause);
            });

            return event.getId();
        });

        JsonObject response = new JsonObject();
        response.addProperty("id", id.toString());

        Http.sendJson(exchange, 200, response);
    }

    private void cancelEvent(HttpExchange exchange, String rawId) throws IOException {
        UUID id = uuid(rawId);

        if (!Http.onTick(() -> IceStom.getInstance().getEventManager().cancelEvent(id))) {
            Http.sendError(exchange, 404, "No running event with that id");
            return;
        }

        Http.send(exchange, 204, "application/json", new byte[0]);
    }

    private void transition(HttpExchange exchange, String rawId) throws IOException {
        JsonObject body = Http.readJson(exchange);

        UUID id = uuid(rawId);
        String stageName = Http.requireString(body, "stage");
        String transitionName = Http.requireString(body, "transition");

        String failure = Http.onTick(() -> {
            IceStomEvent<EventParticipant> event = IceStom.getInstance().getEventManager().getActiveEvent(id);

            if (event == null) return "No running event with that id";

            EventStage stage = event.getLoadedStages().stream()
                    .filter(candidate -> candidate.getStageName().equalsIgnoreCase(stageName))
                    .findFirst()
                    .orElse(null);

            if (stage == null) return "That event has no stage called " + stageName;
            if (!(stage instanceof Stateful<?> stateful)) return stageName + " has no states to move between";

            Object current = stateful.getState();

            Stateful.StateChange<?> change = stateful.getStageChanges().stream()
                    .filter(candidate -> candidate.before() == current && candidate.name().equals(transitionName))
                    .findFirst()
                    .orElse(null);

            if (change == null) return transitionName + " isn't available from " + current;

            change.run().run();

            return null;
        });

        if (failure != null) {
            Http.sendError(exchange, 409, failure);
            return;
        }

        server.pushStateSoon();

        Http.send(exchange, 204, "application/json", new byte[0]);
    }

    private static UUID uuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new Http.BadRequestException("'" + raw + "' isn't a valid id");
        }
    }

    private static List<UUID> uuids(JsonElement element) {
        if (element == null || element.isJsonNull()) return List.of();

        if (!element.isJsonArray()) throw new Http.BadRequestException("'participants' must be an array");

        List<UUID> result = new ArrayList<>();

        for (JsonElement entry : element.getAsJsonArray()) {
            result.add(uuid(entry.getAsString()));
        }

        return result;
    }
}
