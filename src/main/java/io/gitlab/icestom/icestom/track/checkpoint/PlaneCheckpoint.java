package io.gitlab.icestom.icestom.track.checkpoint;

import com.moandjiezana.toml.Toml;
import io.gitlab.icestom.icestom.track.format.TrackData;
import io.gitlab.icestom.icestom.track.format.serialization.PosAdapter;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static io.gitlab.icestom.icestom.util.Expect.expect;

public class PlaneCheckpoint implements Checkpoint {

    protected final Vec a;
    protected final Vec b;

    private final Vec edge;
    private final Vec normal;
    private final Vec upNorm;

    private final double edgeLenSq;
    private final double planeD;
    private final double upD;
    protected final double height;

    public PlaneCheckpoint(Vec a, Vec b, Vec up, double height) {
        this.a = a;
        this.b = b;
        this.height = height;

        this.edge = b.sub(a);
        this.upNorm = up.normalize();
        this.normal = edge.cross(upNorm).normalize();

        this.edgeLenSq = edge.dot(edge);
        this.planeD = -normal.dot(a);
        this.upD = -upNorm.dot(a);
    }

    public Vec getA() { return a; }
    public Vec getB() { return b; }

    @Override
    public Map<Player, Double> detectCrosses(Map<Player, TickMovement> movements) {
        final Map<Player, Double> crossed = new HashMap<>(movements.size() * 2);

        final double nx = normal.x();
        final double ny = normal.y();
        final double nz = normal.z();

        final double ux = upNorm.x();
        final double uy = upNorm.y();
        final double uz = upNorm.z();

        final double ax = a.x();
        final double ay = a.y();
        final double az = a.z();

        final double ex = edge.x();
        final double ey = edge.y();
        final double ez = edge.z();

        final double edgeLenSq = this.edgeLenSq;
        final double planeD = this.planeD;
        final double upD = this.upD;
        final double height = this.height;

        for (Map.Entry<Player, TickMovement> movement : movements.entrySet()) {

            @Nullable final Vec before = movement.getValue().before();

            if (before == null) continue;

            final Vec current = movement.getValue().current();

            final double bx = before.x();
            final double by = before.y();
            final double bz = before.z();

            final double cx = current.x();
            final double cy = current.y();
            final double cz = current.z();

            final double d0 = nx * bx + ny * by + nz * bz + planeD;
            final double d1 = nx * cx + ny * cy + nz * cz + planeD;

            // both on one side
            if (d0 * d1 > 0.0) continue;

            // moving parallel
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
                if (hNum < 0.0 || hNum > height * denom) continue;
            } else {
                if (hNum > 0.0 || hNum < height * denom) continue;
            }

            crossed.put(movement.getKey(), d0 / denom);
        }

        return crossed;
    }

    public static Checkpoint deserialize(Toml properties) throws TrackData.TrackDeserializationException {
        return new PlaneCheckpoint(
                PosAdapter.deserializeVec(properties, "a"),
                PosAdapter.deserializeVec(properties, "b"),
                PosAdapter.deserializeVec(properties, "normal"),
                expect(properties.getDouble("height"), new TrackData.TrackDeserializationException("Plane Checkpoint missing 'height'")).floatValue()
        );
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "a", PosAdapter.serialize(a),
                "b", PosAdapter.serialize(a),
                "normal", normal,
                "height", height
        );
    }
}