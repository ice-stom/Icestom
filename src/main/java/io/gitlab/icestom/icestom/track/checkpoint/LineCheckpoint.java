package io.gitlab.icestom.icestom.track.checkpoint;

import io.gitlab.icestom.icestom.track.TrackFormat;
import net.minestom.server.coordinate.Vec;

import java.io.IOException;

public class LineCheckpoint extends PlaneCheckpoint {

    private static final Vec up = new Vec(0, 1, 0);

    public LineCheckpoint(Vec a, Vec b, double height) {
        super(a, b, up,  height);
    }

    @Override
    public void write(TrackFormat.Writer writer) throws IOException {
        writer.writeVec(a);
        writer.writeVec(b);
        writer.writeString(Double.toString(height));
    }
}
