package io.gitlab.icestom.icestom.track.format;

import com.moandjiezana.toml.Toml;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.checkpoint.CuboidCheckpoint;
import io.gitlab.icestom.icestom.track.checkpoint.LineCheckpoint;
import io.gitlab.icestom.icestom.track.checkpoint.PlaneCheckpoint;
import io.gitlab.icestom.icestom.track.format.serialization.PosAdapter;
import io.gitlab.icestom.icestom.openboatutils.OBUSettingsPackets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static io.gitlab.icestom.icestom.util.Expect.expect;

public interface TrackData {
    int VERSION = 1;

    @NotNull String getId();
    @NotNull Component getName();
    boolean isLooped();
    @NotNull Pos getSpawnLocation();
    @NotNull Map<Checkpoint, Integer> getCheckpoints();
    @NotNull List<Pos> getGridLocations();
    @NotNull List<OBUSettingsPackets> getOpenBoatUtilsPackets();

    class TrackDeserializationException extends Exception {
        public TrackDeserializationException(String message) {
            super(message);
        }
    }

    default @NotNull String getWorldId() {
        return getId();
    }

    default @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("version", VERSION);

        map.put("id", getId());
        map.put("spawn_location", getSpawnLocation());
        map.put("name", JSONComponentSerializer.json().serialize(getName()));
        map.put("looped", isLooped());

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

        Map<String, List<Double>> grid_locations = new HashMap<>();
        map.put("grid", grid_locations);

        int i = 0;
        for (Pos gridLocation : getGridLocations()) {
            grid_locations.put(String.valueOf(i), PosAdapter.serialize(gridLocation));
            i++;
        }

        List<Map<String, Object>> openboatutils_packets = new ArrayList<>();
        map.put("openboatutils", openboatutils_packets);

        getOpenBoatUtilsPackets()
                .forEach(packet -> {
                    openboatutils_packets.add(packet.toMap());
                });

        return map;
    }

    static @NotNull TrackData deserialize(Toml toml) throws TrackDeserializationException {

        String id = expect(toml.getString("id"), new TrackDeserializationException("Missing id"));
        Component name = JSONComponentSerializer.json().deserialize(expect(toml.getString("name"), new TrackDeserializationException("Missing name")));
        boolean looped = expect(toml.getBoolean("looped"), new TrackDeserializationException("Missing looped"));
        Pos spawn_location = expect(toml.getTable("spawn_location").to(Pos.class), new TrackDeserializationException("Missing id"));

        List<Toml> checkpoint_data = expect(toml.getTables("checkpoints"), new TrackDeserializationException("Missing checkpoints"));
        Map<Checkpoint, Integer> checkpoints = new HashMap<>();

        for (Toml map : checkpoint_data) {
            String type = expect(map.getString("type"), new TrackDeserializationException("Checkpoint missing type field"));
            int index = (int) Math.floor(expect(map.getLong("index"), new TrackDeserializationException("Checkpoint missing type field")));

            checkpoints.put(switch (type) {
                case "PlaneCheckpoint" -> PlaneCheckpoint.deserialize(map);
                case "LineCheckpoint" -> LineCheckpoint.deserialize(map);
                case "CuboidCheckpoint" -> CuboidCheckpoint.deserialize(map);
                default -> throw new TrackDeserializationException("Unexpected value: " + type);
            }, index);
        }

        Toml grid_data = expect(toml.getTable("grid"), new TrackDeserializationException("Missing grid locations"));
        List<Pos> grid_locations = new ArrayList<>();

        for (int i = 0; i < grid_data.entrySet().size(); i++) {
            @Nullable Pos pos = PosAdapter.deserializePos(grid_data, String.valueOf(i));

            if (pos == null) throw new TrackDeserializationException("Missing grid location index " + i);

            grid_locations.add(pos);
        }

        List<Toml> openboatutils_data = expect(toml.getTables("openboatutils"), new TrackDeserializationException("Missing openboatutils entry"));
        List<OBUSettingsPackets> openboatutils_packets = new ArrayList<>();

        for (Toml packet : openboatutils_data) {
            openboatutils_packets.add(OBUSettingsPackets.fromMap(packet.toMap()));
        }

        return new TrackData() {
            @Override
            public @NotNull String getId() { return id; }

            @Override
            public @NotNull Component getName() { return name; }

            @Override
            public boolean isLooped() { return looped; }

            @Override
            public @NotNull Pos getSpawnLocation() { return spawn_location; }

            @Override
            public @NotNull Map<Checkpoint, Integer> getCheckpoints() { return checkpoints; }

            @Override
            public @NotNull List<Pos> getGridLocations() {
                return grid_locations;
            }

            @Override
            public @NotNull List<OBUSettingsPackets> getOpenBoatUtilsPackets() { return openboatutils_packets; }
        };
    }
}