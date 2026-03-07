package io.gitlab.icestom.icestom.track.format;

import com.moandjiezana.toml.Toml;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.checkpoint.LineCheckpoint;
import io.gitlab.icestom.icestom.track.checkpoint.PlaneCheckpoint;
import net.minestom.server.coordinate.Pos;

import java.util.*;

import static io.gitlab.icestom.icestom.util.Expect.expect;

public interface TrackData {
    String getId();
    Pos getSpawnLocation();
    Map<Checkpoint, Integer> getCheckpoints();

    class TrackDeserializationException extends Exception {
        public TrackDeserializationException(String message) {
            super(message);
        }
    }

    default Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("id", getId());
        map.put("spawn_location", getSpawnLocation());

        List<Map<String, Object>> checkpoints = new ArrayList<>();
        map.put("checkpoints", checkpoints);

        getCheckpoints()
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .forEach(entry -> {
                    Checkpoint checkpoint = entry.getKey();
                    Map<String, Object> checkpointMap = new LinkedHashMap<>();

                    checkpointMap.put("index", entry.getValue());
                    checkpointMap.put("type", checkpoint.getClass().getSimpleName());

                    checkpointMap.putAll(checkpoint.serialize());

                    checkpoints.add(checkpointMap);
                });

        return map;
    }

    static TrackData deserialize(Toml toml) throws TrackDeserializationException {

        String id = expect(toml.getString("id"), new TrackDeserializationException("Missing id"));
        Pos spawn_location = expect(toml.getTable("spawn_location").to(Pos.class), new TrackDeserializationException("Missing id"));

        List<Toml> checkpoint_data = expect(toml.getTables("checkpoints"), new TrackDeserializationException("Missing checkpoints"));
        Map<Checkpoint, Integer> checkpoints = new HashMap<>();

        for (Toml map : checkpoint_data) {
            String type = expect(map.getString("type"), new TrackDeserializationException("Checkpoint missing type field"));
            int index = (int) Math.floor(expect(map.getLong("index"), new TrackDeserializationException("Checkpoint missing type field")));

            checkpoints.put(switch (type) {
                case "PlaneCheckpoint" -> PlaneCheckpoint.deserialize(map);
                case "LineCheckpoint" -> LineCheckpoint.deserialize(map);
                default -> throw new TrackDeserializationException("Unexpected value: " + type);
            }, index);
        }

        return new TrackData() {
            @Override
            public String getId() { return id; }

            @Override
            public Pos getSpawnLocation() { return spawn_location; }

            @Override
            public Map<Checkpoint, Integer> getCheckpoints() { return checkpoints; }
        };
    }
}