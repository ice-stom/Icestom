package io.gitlab.icestom.icestom.web;

import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PanelSessions {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Duration lifetime;

    public PanelSessions(Duration lifetime) {
        this.lifetime = lifetime;
    }

    public Session create(@Nullable UUID owner, String ownerName) {
        expire();

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Session session = new Session(token, owner, ownerName, Instant.now().plus(lifetime));

        sessions.put(token, session);

        return session;
    }

    public @Nullable Session validate(@Nullable String token) {
        if (token == null) return null;

        expire();

        Session session = sessions.get(token);

        if (session == null || session.isExpired()) return null;

        return session;
    }

    public void revoke(String token) {
        sessions.remove(token);
    }

    public void revokeAll() {
        sessions.clear();
    }

    private void expire() {
        Iterator<Map.Entry<String, Session>> iterator = sessions.entrySet().iterator();

        while (iterator.hasNext()) {
            if (iterator.next().getValue().isExpired()) iterator.remove();
        }
    }

    public record Session(String token, @Nullable UUID owner, String ownerName, Instant expiresAt) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
