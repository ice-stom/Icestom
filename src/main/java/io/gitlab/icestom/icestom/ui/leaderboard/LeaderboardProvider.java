package io.gitlab.icestom.icestom.ui.leaderboard;

import net.minestom.server.entity.Player;

interface LeaderboardProvider {
    void startViewing(Player viewer);
    void stopViewing(Player viewer);
}
