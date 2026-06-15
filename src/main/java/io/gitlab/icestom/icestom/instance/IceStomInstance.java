package io.gitlab.icestom.icestom.instance;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public abstract class IceStomInstance extends InstanceContainer {
    public IceStomInstance(UUID uuid, RegistryKey<DimensionType> dimensionType, Key dimensionName) {
        super(uuid, dimensionType, dimensionName);

        setTimeRate(0);
        setTime(6000);

        if (this instanceof SpawnLocation spawnLocation) {
            eventNode().addListener(PlayerRespawnEvent.class, playerRespawnEvent -> {
                final Player player = playerRespawnEvent.getPlayer();

                playerRespawnEvent.setRespawnPosition(spawnLocation.spawnLocation(player));
            });
        }
    }

    public void start() {}
}
