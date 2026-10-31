package io.gitlab.icestom.icestom.track.colliders.types;

import net.minestom.server.coordinate.Vec;

public class LineCollider extends PlaneCollider {
    private static final Vec up = new Vec(0, 1, 0);

    public LineCollider(Vec a, Vec b, double height) {
        super(a, b, up,  height);
    }
}
