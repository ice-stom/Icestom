package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.entity.Boat;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.world.DimensionType;

import java.util.UUID;

public abstract class BoatInstance extends InstanceContainer {
    public BoatInstance() {
        super(UUID.randomUUID(), DimensionType.OVERWORLD);
    }

    public void putPlayerInBoat(Player player, Pos pos) {
        Boat boat = new Boat();

        boat.setInstance(this, pos);

        boat.addPassenger(player);
    }
}
