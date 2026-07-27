package io.gitlab.icestom.icestom.track.colliders.types;

import io.gitlab.icestom.icestom.track.TickMovement;
import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.gitlab.icestom.icestom.track.colliders.InsideCollider;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;

public class CuboidCollider implements InsideCollider, CrossCollider {
    protected final Vec a;
    protected final Vec b;

    public CuboidCollider(Vec a, Vec b) {
        this.a = a;
        this.b = b;
    }

    public Vec getA() { return a; }
    public Vec getB() { return b; }

    public @Nullable Long detectCross(TickMovement movement) {
        Vec before  = movement.before();
        Vec current = movement.current();

        if (before == null) return null;
        if (isInside(before)) return null;
        if (!isInside(current)) return null;

        double dx = current.x() - before.x();
        double dy = current.y() - before.y();
        double dz = current.z() - before.z();

        double tMin = 0.0;

        if (Math.abs(dx) > 1e-10) {
            tMin = Math.max(tMin, Math.min((a.x() - before.x()) / dx, (b.x() - before.x()) / dx));
        } else if (before.x() < a.x() || before.x() > b.x()) {
            return null;
        }

        if (Math.abs(dy) > 1e-10) {
            tMin = Math.max(tMin, Math.min((a.y() - before.y()) / dy, (b.y() - before.y()) / dy));
        } else if (before.y() < a.y() || before.y() > b.y()) {
            return null;
        }

        if (Math.abs(dz) > 1e-10) {
            tMin = Math.max(tMin, Math.min((a.z() - before.z()) / dz, (b.z() - before.z()) / dz));
        } else if (before.z() < a.z() || before.z() > b.z()) {
            return null;
        }

        return (long) (Math.clamp(tMin, 0.0, 1.0) * 50.0);
    }

    @Override
    public boolean detectInside(TickMovement movement) {
        return isInside(movement.current());
    }

    protected boolean isInside(Vec p) {
        return p.x() >= a.x() && p.x() <= b.x()
                && p.y() >= a.y() && p.y() <= b.y()
                && p.z() >= a.z() && p.z() <= b.z();
    }
}
