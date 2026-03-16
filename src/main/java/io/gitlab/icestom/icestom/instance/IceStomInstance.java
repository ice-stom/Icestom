package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.ui.ActionBarProvider;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public abstract class IceStomInstance extends InstanceContainer {
    public IceStomInstance(UUID uuid, RegistryKey<DimensionType> dimensionType, Key dimensionName) {
        super(uuid, dimensionType, dimensionName);

        if (this instanceof SpawnLocation spawnLocation) {
            eventNode().addListener(PlayerRespawnEvent.class, playerRespawnEvent -> {
                final Player player = playerRespawnEvent.getPlayer();

                playerRespawnEvent.setRespawnPosition(spawnLocation.spawnLocation(player));
            });
        }
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (this instanceof ActionBarProvider actionBarProvider) {
            for (Player player : getPlayers()) {
                @Nullable Component text = actionBarProvider.getActionBar(player);

                if (text != null) player.sendActionBar(text);
            }
        }
    }
}
