package io.gitlab.icestom.icestom.ui.leaderboard;

import net.minestom.server.entity.Player;

interface LeaderboardProvider {
    void playerAdded(Player player);
    void playerRemoved(Player player);
}
