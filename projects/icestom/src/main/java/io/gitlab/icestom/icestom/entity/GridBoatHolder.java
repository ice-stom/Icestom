package io.gitlab.icestom.icestom.entity;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Instance;

public class GridBoatHolder extends Entity {
    public GridBoatHolder(Instance instance, Pos pos) {
        super(EntityType.ARMOR_STAND);

        hasPhysics = false;
        setNoGravity(true);

        getEntityMeta().setInvisible(true);

        setInstance(instance, pos.sub(0, 1.975, 0));
    }
}
