package io.gitlab.icestom.icestom.track.format.serialization;

import com.moandjiezana.toml.Toml;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;

import java.util.List;

public class PosAdapter {
    public static Pos deserializePos(Toml toml, String key) {
        List<Double> e = toml.getList(key);

        return new Pos(
                e.get(0),
                e.get(1),
                e.get(2),
                e.get(3).floatValue(),
                e.get(4).floatValue()
        );
    }

    public static List<Double> serialize(Pos pos) {
        return List.of(
                pos.x(),
                pos.y(),
                pos.z(),
                (double) pos.yaw(),
                (double) pos.pitch()
        );
    }

    public static Vec deserializeVec(Toml toml, String key) {
        List<Double> e = toml.getList(key);

        return new Vec(
                e.get(0),
                e.get(1),
                e.get(2)
        );
    }

    public static List<Double> serialize(Vec pos) {
        return List.of(
                pos.x(),
                pos.y(),
                pos.z()
        );
    }
}
