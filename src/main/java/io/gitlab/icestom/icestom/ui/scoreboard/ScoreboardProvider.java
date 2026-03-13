package io.gitlab.icestom.icestom.ui.scoreboard;

import net.minestom.server.entity.Player;

public interface ScoreboardProvider {
    void startViewing(Player viewer);
    void stopViewing(Player viewer);
}
