package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.ui.ActionBarProvider;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

import java.util.UUID;

public abstract class IceStomInstance extends InstanceContainer {
    public IceStomInstance(UUID uuid, RegistryKey<DimensionType> dimensionType, Key dimensionName) {
        super(uuid, dimensionType, dimensionName);
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (this instanceof ActionBarProvider actionBarProvider) {
            for (Player player : getPlayers()) {
                player.sendActionBar(actionBarProvider.getActionBar(player));
            }
        }
    }
}
