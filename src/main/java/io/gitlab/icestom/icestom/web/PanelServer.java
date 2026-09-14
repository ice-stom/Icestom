package io.gitlab.icestom.icestom.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.gitlab.icestom.icestom.config.IceStomConfig;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class PanelServer {

    private static final Logger log = LoggerFactory.getLogger(PanelServer.class);

    private static final String CLASSPATH_ROOT = "web";
    private static final int BROADCAST_MILLIS = 500;
    private static final int HEARTBEAT_EVERY = 20;
    private static final int MAX_STREAMS = 16;

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "json", "application/json; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "ico", "image/x-icon",
            "woff2", "font/woff2"
    );

    private final IceStomConfig.WebConfigSection config;
    private final PanelSessions sessions;
    private final HttpServer http;
    private final ExecutorService httpExecutor;
    private final ExecutorService deliveryExecutor;
    private final @Nullable Path devRoot;

    private final List<StreamClient> clients = new CopyOnWriteArrayList<>();

    private @Nullable Task broadcastTask;
    private @Nullable String lastState;
    private volatile boolean forcePush;
    private int idleBroadcasts;

    public PanelServer(IceStomConfig.WebConfigSection config) throws IOException {
        this.config = config;
        this.sessions = new PanelSessions(Duration.ofMinutes(Math.max(1, config.session_minutes)));

        String devRootSetting = config.dev_root == null ? "" : config.dev_root.trim();
        this.devRoot = devRootSetting.isEmpty() ? null : Path.of(devRootSetting);

        if (devRoot != null && !Files.isDirectory(devRoot)) {
            log.warn("web.dev_root '{}' isn't a directory, falling back to the bundled panel", devRoot);
        }

        this.httpExecutor = Executors.newCachedThreadPool(named("panel-http"));

        this.deliveryExecutor = Executors.newSingleThreadExecutor(named("panel-broadcast"));

        String bind = config.bind == null || config.bind.isBlank() ? "0.0.0.0" : config.bind;

        this.http = HttpServer.create(new InetSocketAddress(bind, config.port), 0);
        this.http.setExecutor(httpExecutor);
        this.http.createContext("/api/", new PanelApi(this));
        this.http.createContext("/", this::serveStatic);
    }

    private static java.util.concurrent.ThreadFactory named(String prefix) {
        AtomicInteger counter = new AtomicInteger();

        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    public void start() {
        http.start();

        broadcastTask = MinecraftServer.getSchedulerManager().submitTask(() -> {
            broadcast();
            return TaskSchedule.millis(BROADCAST_MILLIS);
        });

        log.info("Event panel listening on {}", baseUrl());

        if (config.public_url == null || config.public_url.isBlank()) {
            log.info("Set web.public_url in config.toml to the address players should actually open.");
        }
    }

    public void stop() {
        if (broadcastTask != null) broadcastTask.cancel();

        for (StreamClient client : clients) client.close();
        clients.clear();

        sessions.revokeAll();

        http.stop(0);
        deliveryExecutor.shutdownNow();
        httpExecutor.shutdownNow();
    }

    PanelSessions sessions() {
        return sessions;
    }

    public String createLink(@Nullable UUID owner, String ownerName) {
        PanelSessions.Session session = sessions.create(owner, ownerName);

        return baseUrl() + "/#" + session.token();
    }

    public String baseUrl() {
        String configured = config.public_url;

        if (configured != null && !configured.isBlank()) {
            String trimmed = configured.trim();

            return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        }

        String host = config.bind == null || config.bind.isBlank() || config.bind.equals("0.0.0.0")
                ? "localhost"
                : config.bind;

        return "http://" + host + ":" + config.port;
    }

    public boolean isOperator(@Nullable UUID uuid, String name) {
        List<String> operators = config.operators;

        if (operators == null || operators.isEmpty()) return false;

        for (String entry : operators) {
            if (entry == null) continue;

            String trimmed = entry.trim();

            if (trimmed.equalsIgnoreCase(name)) return true;
            if (uuid != null && trimmed.equalsIgnoreCase(uuid.toString())) return true;
        }

        return false;
    }

    void stream(HttpExchange exchange) throws IOException {
        if (clients.size() >= MAX_STREAMS) {
            Http.sendError(exchange, 503, "Too many panels are already connected to this server.");
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-transform");
        exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
        exchange.sendResponseHeaders(200, 0);

        StreamClient client = new StreamClient(exchange);

        if (!client.send("state", Http.GSON.toJson(Http.onTick(Snapshots::state)))) {
            client.close();
            return;
        }

        clients.add(client);

        client.await();

        clients.remove(client);
    }

    void pushStateSoon() {
        forcePush = true;
    }

    private void broadcast() {
        if (clients.isEmpty()) {
            lastState = null;
            return;
        }

        String state;
        try {
            state = Http.GSON.toJson(Snapshots.state());
        } catch (Exception e) {
            log.error("Failed to snapshot server state for the panel", e);
            return;
        }

        boolean changed = forcePush || !state.equals(lastState);
        forcePush = false;

        if (changed) {
            lastState = state;
            idleBroadcasts = 0;

            deliveryExecutor.execute(() -> deliver(client -> client.send("state", state)));
            return;
        }

        if (++idleBroadcasts < HEARTBEAT_EVERY) return;

        idleBroadcasts = 0;
        deliveryExecutor.execute(() -> deliver(StreamClient::heartbeat));
    }

    private void deliver(java.util.function.Predicate<StreamClient> write) {
        for (StreamClient client : clients) {
            if (write.test(client)) continue;

            clients.remove(client);
            client.close();
        }
    }

    private void serveStatic(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET") && !exchange.getRequestMethod().equals("HEAD")) {
                Http.sendError(exchange, 405, "Method not allowed");
                return;
            }

            String path = exchange.getRequestURI().getPath();

            if (path.isEmpty() || path.equals("/")) path = "/index.html";

            byte[] body = read(path);

            if (body == null) {
                body = read("/index.html");
                path = "/index.html";
            }

            if (body == null) {
                Http.send(exchange, 404, "text/plain; charset=utf-8",
                        "The panel site isn't bundled in this build. See IcestomSite/README.md."
                                .getBytes(StandardCharsets.UTF_8));
                return;
            }

            exchange.getResponseHeaders().set("Cache-Control", "no-cache");

            Http.send(exchange, 200, contentType(path), body);
        } catch (Exception e) {
            log.error("Failed to serve {}", exchange.getRequestURI(), e);
        } finally {
            exchange.close();
        }
    }

    private @Nullable byte[] read(String path) throws IOException {
        String relative = normalise(path);

        if (relative == null) return null;

        if (devRoot != null && Files.isDirectory(devRoot)) {
            Path root = devRoot.toAbsolutePath().normalize();
            Path file = root.resolve(relative).normalize();

            if (!file.startsWith(root)) return null;

            return Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
        }

        try (InputStream in = PanelServer.class.getClassLoader()
                .getResourceAsStream(CLASSPATH_ROOT + "/" + relative)) {

            return in == null ? null : in.readAllBytes();
        }
    }

    private static @Nullable String normalise(String path) {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;

        if (trimmed.isEmpty()) return null;

        for (String segment : trimmed.split("/")) {
            if (segment.equals("..") || segment.equals(".") || segment.isEmpty()) return null;
        }

        return trimmed;
    }

    private static String contentType(String path) {
        int dot = path.lastIndexOf('.');

        if (dot < 0) return "application/octet-stream";

        return CONTENT_TYPES.getOrDefault(
                path.substring(dot + 1).toLowerCase(Locale.ROOT),
                "application/octet-stream"
        );
    }

    private static final class StreamClient {

        private final HttpExchange exchange;
        private final OutputStream out;
        private final Object lock = new Object();

        private volatile boolean open = true;

        StreamClient(HttpExchange exchange) {
            this.exchange = exchange;
            this.out = exchange.getResponseBody();
        }

        boolean send(String event, String data) {
            return write("event: " + event + "\ndata: " + data + "\n\n");
        }

        boolean heartbeat() {
            return write(": keepalive\n\n");
        }

        private boolean write(String frame) {
            if (!open) return false;

            try {
                out.write(frame.getBytes(StandardCharsets.UTF_8));
                out.flush();
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        void await() {
            synchronized (lock) {
                while (open) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        void close() {
            open = false;

            synchronized (lock) {
                lock.notifyAll();
            }

            try {
                out.close();
            } catch (IOException ignored) {
            }

            exchange.close();
        }
    }
}
