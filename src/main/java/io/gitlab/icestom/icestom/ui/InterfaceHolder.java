package io.gitlab.icestom.icestom.ui;

import io.gitlab.icestom.icestom.IceStom;
import net.minestom.server.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InterfaceHolder<T extends InterfaceProvider> {

    private static final InterfaceManager manager = IceStom.getInstance().getPlayerLeaderboardManager();

    private final Class<T> heldType;
    private final Map<InterfaceManager.InterfaceType, InterfaceProvider> providers = new HashMap<>();

    public InterfaceHolder(Class<T> heldType) {
        this.heldType = heldType;
    }

    public void addViewer(Player player) {
        for (InterfaceManager.InterfaceType interfaceType : manager.getActiveInterfaces(player)) {
            if (interfaceType.matches(heldType)) {
                providers.computeIfAbsent(interfaceType, InterfaceManager.InterfaceType::newProvider);
            }
        }

        for (InterfaceProvider provider : providers.values()) {
            provider.startViewing(player);
        }
    }

    public void removeViewer(Player player) {
        for (InterfaceProvider provider : providers.values()) {
            provider.stopViewing(player);
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<T> getProviders() {
        return (Collection<T>) providers.values();
    }
}
