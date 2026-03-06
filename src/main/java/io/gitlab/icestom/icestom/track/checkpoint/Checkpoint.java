package io.gitlab.icestom.icestom.track.checkpoint;

import io.gitlab.icestom.icestom.track.TrackFormat;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;

import java.io.IOException;
import java.util.Map;

public interface Checkpoint {

    record IndexedCheckpoint(int index, Checkpoint checkpoint) {}

    Map<Player, Double> detectCrosses(Map<Player, TickMovement> movements);

    void write(TrackFormat.Writer writer) throws IOException;

    static IndexedCheckpoint read(TrackFormat.Reader reader) throws IOException {
        String typeName = reader.readUntilOrEof(':');
        if (typeName == null) return null;

        int index = Integer.parseInt(reader.readUntil(':', "checkpoint index"));

        return switch (typeName) {
            case "LineCheckpoint" -> {
                Vec side_a = reader.readVec();
                Vec side_b = reader.readVec();
                double height = Double.parseDouble(reader.readUntil('\n', "line height"));
                yield new IndexedCheckpoint(index, new LineCheckpoint(side_a, side_b, height));
            }
            case "PlaneCheckpoint" -> {
                Vec side_a = reader.readVec();
                Vec side_b = reader.readVec();
                Vec up = reader.readVec();
                double height = Double.parseDouble(reader.readUntil('\n', "plane height"));
                yield new IndexedCheckpoint(index, new PlaneCheckpoint(side_a, side_b, up, height));
            }
            default -> throw new IllegalArgumentException("Unknown checkpoint type: " + typeName);
        };
    }
}