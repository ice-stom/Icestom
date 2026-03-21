package io.gitlab.icestom.icestom.ui;

import io.gitlab.icestom.icestom.ui.impl.VanillaInterface;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerPluginMessageEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InterfaceManager {

    private final Map<Player, Map<InterfaceType, Boolean>> supportedInterfaces = new HashMap<>();

    public enum InterfaceType {
        VANILLA("Vanilla", VanillaInterface.class);

        private final String name;
        private final Class<? extends InterfaceProvider> provider;

        InterfaceType(String name, Class<? extends InterfaceProvider> provider) {
            this.name = name;
            this.provider = provider;
        }

        public String getName() {  return name; }

        public boolean matches(Class<?> obj) {
            return obj.isAssignableFrom(provider);
        }

        public InterfaceProvider newProvider() {
            try {
                return provider.getConstructor().newInstance();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public InterfaceManager() {
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(PlayerPluginMessageEvent.class, this::onPlayerPluginMessage);
    }

    private void onPlayerPluginMessage(PlayerPluginMessageEvent event) {
        final Player player = event.getPlayer();

        final Map<InterfaceType, Boolean> providers = supportedInterfaces.computeIfAbsent(player, _ -> new HashMap<>());

        providers.put(InterfaceType.VANILLA, true);

        // TODO: add bodkin protocol
    }

    public Set<InterfaceType> getActiveInterfaces(Player player) {
        Set<InterfaceType> types = new HashSet<>();

        var interfaceProviders = supportedInterfaces.get(player);
        assert interfaceProviders != null;

        interfaceProviders.forEach((interfaceProvider, enabled) ->  {
            if (enabled) types.add(interfaceProvider);
        });

        return types;
    }
}
