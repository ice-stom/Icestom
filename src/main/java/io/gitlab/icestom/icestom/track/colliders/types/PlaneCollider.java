package io.gitlab.icestom.icestom.track.colliders.types;

import io.gitlab.icestom.icestom.track.TickMovement;
import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PlaneCollider implements CrossCollider {

    protected final Vec a;
    protected final Vec b;

    private final Vec edge;
    private final Vec up;
    private final Vec normal;

    private final double edgeLenSq;
    private final double planeD;
    private final double upD;
    protected final double height;

    public PlaneCollider(Vec a, Vec b, Vec up, double height) {
        this.a = a;
        this.b = b;
        this.height = height;

        this.edge = b.sub(a);
        this.up = up.normalize();
        this.normal = edge.cross(this.up).normalize();

        this.edgeLenSq = edge.dot(edge);
        this.planeD = -normal.dot(a);
        this.upD = -this.up.dot(a);
    }

    public Vec getA() { return a; }
    public Vec getB() { return b; }
    public Vec getUp() { return up; }
    public double getHeight() { return height; }

    @Override
    public @Nullable Long detectCross(TickMovement movement) {
        final Vec before = movement.before();
        if (before == null) return null;

        final Vec current = movement.current();

        final double nx = normal.x(), ny = normal.y(), nz = normal.z();
        final double ux = up.x(), uy = up.y(), uz = up.z();
        final double ax = a.x(), ay = a.y(), az = a.z();
        final double ex = edge.x(), ey = edge.y(), ez = edge.z();

        final double halfHeight = height * 0.5;

        final double bx = before.x(), by = before.y(), bz = before.z();
        final double cx = current.x(), cy = current.y(), cz = current.z();

        final double d0 = nx * bx + ny * by + nz * bz + planeD;
        final double d1 = nx * cx + ny * cy + nz * cz + planeD;

        if (d0 * d1 > 0.0) return null;

        final double denom = d0 - d1;
        if (denom == 0.0) return null;

        final double baDotEdge = (bx - ax) * ex + (by - ay) * ey + (bz - az) * ez;
        final double cbDotEdge = (cx - bx) * ex + (cy - by) * ey + (cz - bz) * ez;

        final double uNum = baDotEdge * denom + d0 * cbDotEdge;
        final double uDen = edgeLenSq * denom;

        if (denom > 0.0) {
            if (uNum < 0.0 || uNum > uDen) return null;
        } else {
            if (uNum > 0.0 || uNum < uDen) return null;
        }

        final double hB = ux * bx + uy * by + uz * bz + upD;
        final double hC = ux * cx + uy * cy + uz * cz + upD;
        final double hNum = hB * denom + d0 * (hC - hB);

        if (denom > 0.0) {
            if (hNum < -halfHeight * denom || hNum > halfHeight * denom) return null;
        } else {
            if (hNum > -halfHeight * denom || hNum < halfHeight * denom) return null;
        }

        return (long) (50 * (d0 / denom));
    }

    // yes this is duplicated, it's faster to do it like this for a collection of movements
    @Override
    public <T> Map<T, Long> detectCrosses(Map<T, TickMovement> movements) {
        final Map<T, Long> crosses = new HashMap<>();

        final double nx = normal.x(), ny = normal.y(), nz = normal.z();
        final double ux = up.x(), uy = up.y(), uz = up.z();
        final double ax = a.x(), ay = a.y(), az = a.z();
        final double ex = edge.x(), ey = edge.y(), ez = edge.z();

        final double halfHeight = height * 0.5;

        for (Map.Entry<T, TickMovement> entry : movements.entrySet()) {
            final Vec before = entry.getValue().before();
            if (before == null) continue;

            final Vec current = entry.getValue().current();

            final double bx = before.x(), by = before.y(), bz = before.z();
            final double cx = current.x(), cy = current.y(), cz = current.z();

            final double d0 = nx * bx + ny * by + nz * bz + planeD;
            final double d1 = nx * cx + ny * cy + nz * cz + planeD;

            if (d0 * d1 > 0.0) continue;

            final double denom = d0 - d1;
            if (denom == 0.0) continue;

            final double baDotEdge = (bx - ax) * ex + (by - ay) * ey + (bz - az) * ez;
            final double cbDotEdge = (cx - bx) * ex + (cy - by) * ey + (cz - bz) * ez;

            final double uNum = baDotEdge * denom + d0 * cbDotEdge;
            final double uDen = edgeLenSq * denom;

            if (denom > 0.0) {
                if (uNum < 0.0 || uNum > uDen) continue;
            } else {
                if (uNum > 0.0 || uNum < uDen) continue;
            }

            final double hB = ux * bx + uy * by + uz * bz + upD;
            final double hC = ux * cx + uy * cy + uz * cz + upD;
            final double hNum = hB * denom + d0 * (hC - hB);

            if (denom > 0.0) {
                if (hNum < -halfHeight * denom || hNum > halfHeight * denom) continue;
            } else {
                if (hNum > -halfHeight * denom || hNum < halfHeight * denom) continue;
            }

            crosses.put(entry.getKey(), (long) (50 * (d0 / denom)));
        }

        return crosses;
    }
}