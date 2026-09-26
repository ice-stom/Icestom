package io.gitlab.icestom.icestom.ui.interfaces;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventHandler;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class InterfaceManager {
    public static final EventNode<Event> EVENT_NODE = EventNode.all("interface");

    private static final Map<Class<?>, Set<InterfaceProvider>> registry = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(InterfaceManager.class);

    public static void register(Class<?> type, InterfaceProvider provider) {
        registry.computeIfAbsent(type, _ -> new HashSet<>()).add(provider);
    }

    public static <T> InterfaceHolder getHolder(Class<?> type, T holder) {
        @Nullable Set<InterfaceProvider> providers = registry.get(type);

        if (providers == null) {
            providers = Set.of();

            log.error("Unknown interface holder {}", type);
        }

        List<Interface<?, ?>> interfaces = new ArrayList<>();

        for (InterfaceProvider provider : providers) {
            Interface<?, ?> anInterface = provider.getInterface(holder);

            if (anInterface instanceof EventHandler<?> handler) {
                EVENT_NODE
                        .addChild(handler.eventNode());
            }

            interfaces.add(anInterface);
        }

        return new InterfaceHolder(interfaces);
    }

    public static class InterfaceHolder {
        private final List<Interface<?, ?>> interfaces;

        protected InterfaceHolder(List<Interface<?, ?>> interfaces) {
            this.interfaces = interfaces;
        }

        public List<Interface<?, ?>> getInterfaces() {
            return interfaces;
        }

        public void startWatching(Player player) {
            for (Interface<?, ?> anInterface : interfaces) {
                if (anInterface.supportsPlayer(player)) {
                    anInterface.startWatching(player);
                }
            }
        }

        public void stopWatching(Player player) {
            for (Interface<?, ?> anInterface : interfaces) {
                if (anInterface.supportsPlayer(player)) {
                    anInterface.stopWatching(player);
                }
            }
        }
    }
}
