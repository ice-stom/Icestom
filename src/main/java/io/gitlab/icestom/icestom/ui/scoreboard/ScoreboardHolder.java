package io.gitlab.icestom.icestom.ui.scoreboard;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.ui.scoreboard.manager.PlayerScoreboardManager;
import net.minestom.server.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ScoreboardHolder<T extends ScoreboardProvider> {

    private static final PlayerScoreboardManager manager = IceStom.getInstance().getPlayerLeaderboardManager();

    private final Class<T> type;
    private final Map<PlayerScoreboardManager.ScoreboardProviderType, ScoreboardProvider> providers = new HashMap<>();

    public ScoreboardHolder(Class<T> type) {
        this.type = type;
    }

    public void init(Player player) {
        for (PlayerScoreboardManager.ScoreboardProviderType leaderboardType : manager.getScoreboardTypes(player)) {
            if (leaderboardType.matches(type)) {
                providers.computeIfAbsent(leaderboardType, PlayerScoreboardManager.ScoreboardProviderType::newProvider);
            }
        }

        for (ScoreboardProvider provider : providers.values()) {
            provider.startViewing(player);
        }
    }

    public void uninit(Player player) {
        for (ScoreboardProvider provider : providers.values()) {
            provider.stopViewing(player);
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<T> getProviders() {
        return (Collection<T>) providers.values();
    }
}
