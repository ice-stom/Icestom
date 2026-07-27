package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.stomtrack.type.Location;
import net.minestom.server.coordinate.Pos;

import java.util.HashMap;
import java.util.Map;

public interface LocationConverter {
    static Map<String, Pos> fromLocationDef(Map<String, Location> defs) {
        Map<String, Pos> map = new HashMap<>();

        defs.forEach((id, location) -> {
            map.put(
                    id,
                    PositionConverter.fromLocation(location)
            );
        });

        return map;
    }
}
