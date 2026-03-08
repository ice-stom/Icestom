package io.gitlab.icestom.icestom.util;

import java.util.HashMap;

public class CooldownManager<K> {
    private final HashMap<K, Long> expires_at = new HashMap<>();

    public void addCooldown(K k, long duration_ms) {
        expires_at.put(k, System.currentTimeMillis() + duration_ms);
    }

    public boolean isOnCooldown(K k) {
        Long expiry = expires_at.get(k);

        if (expiry == null) return false;

        return expiry > System.currentTimeMillis();
    }
}
