package io.gitlab.icestom.icestom.ui.scoreboard.manager;

import io.gitlab.icestom.icestom.ui.scoreboard.ScoreboardProvider;
import io.gitlab.icestom.icestom.ui.scoreboard.VanillaScoreboard;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerPluginMessageEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerScoreboardManager {

    private final Map<Player, Map<ScoreboardProviderType, Boolean>> scoreboards = new HashMap<>();

    public enum ScoreboardProviderType {
        VANILLA("Vanilla Leaderboard", VanillaScoreboard.class);

        private final String name;
        private final Class<? extends ScoreboardProvider> provider;

        ScoreboardProviderType(String name, Class<? extends ScoreboardProvider> provider) {
            this.name = name;
            this.provider = provider;
        }

        public String getName() {  return name; }

        public boolean matches(Class<?> obj) {
            return obj.isAssignableFrom(provider);
        }

        public ScoreboardProvider newProvider() {
            try {
                return provider.getConstructor().newInstance();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public PlayerScoreboardManager() {
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(PlayerPluginMessageEvent.class, this::onPlayerPluginMessage);
    }

    private void onPlayerPluginMessage(PlayerPluginMessageEvent event) {
        final Player player = event.getPlayer();

        final Map<ScoreboardProviderType, Boolean> providers = scoreboards.computeIfAbsent(player, _ -> new HashMap<>());

        providers.put(ScoreboardProviderType.VANILLA, true);

        // TODO: add bodkin protocol
    }

    public Set<ScoreboardProviderType> getScoreboardTypes(Player player) {
        Set<ScoreboardProviderType> types = new HashSet<>();

        scoreboards.get(player).forEach((providerType, enabled) ->  {
            if (enabled) types.add(providerType);
        });

        return types;
    }
}
