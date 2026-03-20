package io.gitlab.icestom.icestom.event.stage;

import io.gitlab.icestom.icestom.instance.PlayerHolder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;

public interface SingleInstanceStage<I extends InstanceContainer & PlayerHolder> extends Stage<I> {

    I getInstance();

    @Override
    default I getInstance(Player player) {
        return getInstance();
    }

    @Override
    default void register() {
        MinecraftServer.getInstanceManager().registerInstance(getInstance());
    }

    @Override
    default void unregister() {
        MinecraftServer.getInstanceManager().unregisterInstance(getInstance());
    }
}
