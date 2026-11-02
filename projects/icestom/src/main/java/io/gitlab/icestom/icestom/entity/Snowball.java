package io.gitlab.icestom.icestom.entity;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.entity.EntityType;
import net.minestom.server.coordinate.Vec;

import java.util.function.Consumer;

public final class Snowball extends EntityProjectile {

    private final Consumer<Pos> onHit;

    public Snowball(Entity shooter, Vec velocity, Consumer<Pos> onHit) {
        super(shooter, EntityType.SNOWBALL);

        this.onHit = onHit;

        setVelocity(velocity);
        setBoundingBox(0.25, 0.25, 0.25);
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (isRemoved() || getInstance() == null) {
            return;
        }

        if (isOnGround()) {
            onHit.accept(getPosition());
            remove();
            return;
        }

        for (Entity target : getInstance().getEntities()) {
            if (target == this || target == getShooter()) {
                continue;
            }

            if (getDistanceSquared(target) <= 1) {
                onHit.accept(getPosition());
                remove();
                return;
            }
        }
    }
}