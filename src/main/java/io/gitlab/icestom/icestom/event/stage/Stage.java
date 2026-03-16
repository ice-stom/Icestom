package io.gitlab.icestom.icestom.event.stage;

import io.gitlab.icestom.icestom.instance.PlayerHolder;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;

public interface Stage<I extends InstanceContainer & PlayerHolder> {
    I getInstance(Player player);
}
