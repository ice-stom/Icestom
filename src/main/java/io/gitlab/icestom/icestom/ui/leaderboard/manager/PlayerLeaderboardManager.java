package io.gitlab.icestom.icestom.ui.leaderboard.manager;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerPluginMessageEvent;

public class PlayerLeaderboardManager {
    public PlayerLeaderboardManager() {
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(PlayerPluginMessageEvent.class, this::onPlayerPluginMessage);
    }

    private void onPlayerPluginMessage(PlayerPluginMessageEvent playerPluginMessageEvent) {

    }
}
