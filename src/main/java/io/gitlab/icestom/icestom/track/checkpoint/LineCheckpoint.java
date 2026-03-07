package io.gitlab.icestom.icestom.track.checkpoint;

import com.moandjiezana.toml.Toml;
import io.gitlab.icestom.icestom.track.format.TrackData;
import io.gitlab.icestom.icestom.track.format.serialization.PosAdapter;
import net.minestom.server.coordinate.Vec;

import java.util.Map;

import static io.gitlab.icestom.icestom.util.Expect.expect;

public class LineCheckpoint extends PlaneCheckpoint {

    private static final Vec up = new Vec(0, 1, 0);

    public LineCheckpoint(Vec a, Vec b, double height) {
        super(a, b, up,  height);
    }

    public static Checkpoint deserialize(Toml properties) throws TrackData.TrackDeserializationException {
        return new LineCheckpoint(
                PosAdapter.deserializeVec(properties, "a"),
                PosAdapter.deserializeVec(properties, "b"),
                expect(properties.getDouble("height"), new TrackData.TrackDeserializationException("Line Checkpoint missing 'height'")).floatValue()
        );
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "a", PosAdapter.serialize(a),
                "b", PosAdapter.serialize(b),
                "height", height
        );
    }
}
