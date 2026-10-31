package io.gitlab.icestom.icestom.track.colliders.types;

import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.track.PlayerMovement;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;

public class InvertedCuboidCollider extends CuboidCollider {
    public InvertedCuboidCollider(Vec a, Vec b) {
        super(a, b);
    }

    @Override
    public boolean detectInside(PlayerMovement movement) {
        return !super.detectInside(movement);
    }

    public @Nullable Long detectCross(PlayerMovement movement) {
        TrackInstance.TickLocation old = movement.before();

        if (old == null) return null;

        Vec before = old.pos();
        Vec current = movement.current().pos();

        if (before == null) return null;
        if (!isInside(before)) return null;
        if (isInside(current)) return null;

        double dx = current.x() - before.x();
        double dy = current.y() - before.y();
        double dz = current.z() - before.z();

        double tExit = 1.0;

        if (Math.abs(dx) > 1e-10) {
            tExit = Math.min(tExit, Math.max((a.x() - before.x()) / dx, (b.x() - before.x()) / dx));
        }

        if (Math.abs(dy) > 1e-10) {
            tExit = Math.min(tExit, Math.max((a.y() - before.y()) / dy, (b.y() - before.y()) / dy));
        }

        if (Math.abs(dz) > 1e-10) {
            tExit = Math.min(tExit, Math.max((a.z() - before.z()) / dz, (b.z() - before.z()) / dz));
        }

        return (long) (Math.clamp(tExit, 0.0, 1.0) * 50.0);
    }
}
