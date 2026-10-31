package io.gitlab.icestom.icestom.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

final class Http {

    static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private Http() {}

    static byte[] readBody(HttpExchange exchange) throws IOException {
        return exchange.getRequestBody().readAllBytes();
    }

    static JsonObject readJson(HttpExchange exchange) throws IOException {
        String body = new String(readBody(exchange), StandardCharsets.UTF_8);

        if (body.isBlank()) return new JsonObject();

        JsonElement parsed = JsonParser.parseString(body);

        if (!parsed.isJsonObject()) throw new BadRequestException("Expected a JSON object body");

        return parsed.getAsJsonObject();
    }

    static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");

        if (status == 204 || "HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(status, body.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    static void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        send(exchange, status, "application/json; charset=utf-8",
                GSON.toJson(value).getBytes(StandardCharsets.UTF_8));
    }

    static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);

        sendJson(exchange, status, error);
    }

    static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();

        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isEmpty()) return result;

        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');

            if (eq < 0) {
                result.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            } else {
                result.put(
                        URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8)
                );
            }
        }

        return result;
    }

    static @Nullable String bearerToken(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");

        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }

        return query(exchange).get("token");
    }

    static String requireString(JsonObject object, String field) {
        JsonElement element = object.get(field);

        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new BadRequestException("Missing or non-string field '" + field + "'");
        }

        return element.getAsString();
    }

    static <T> T onTick(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();

        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Timed out waiting for the server tick thread", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;

            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for the server tick thread", e);
        }
    }

    static class BadRequestException extends RuntimeException {
        BadRequestException(String message) {
            super(message);
        }
    }
}
