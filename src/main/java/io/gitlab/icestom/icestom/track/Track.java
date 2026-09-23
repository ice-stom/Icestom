package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.gitlab.icestom.icestom.track.colliders.InsideCollider;
import io.gitlab.icestom.stomtrack.EnvironmentFile;
import io.gitlab.icestom.stomtrack.TrackFile;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Track {

    private static final Logger log = LoggerFactory.getLogger(Track.class);

    private final String id;
    private final Component name;
    private final boolean looped;
    private final Pos spawnLocation;
    private final Map<CrossCollider, Integer> checkpoints;
    private final List<Pos> gridLocations;
    private final Map<String, String> tags;
    private final List<OBUSettingsPacket> openBoatUtilsPackets;
    private final Map<InsideCollider, Set<String>> regions;
    private final Map<CrossCollider, Set<String>> triggers;
    private final Map<String, Pos> locations;

    private final Map<Integer, List<CrossCollider>> checkpoint_lookup = new HashMap<>();

    private final InstanceContainer mapContainer;

    private final int wrapIndex;

    public Track(TrackFile trackFile, TrackLoad load) {
        this.id = trackFile.getId();
        this.name = trackFile.getName();
        this.looped = trackFile.isLooped();
        this.tags = trackFile.getTags();
        this.spawnLocation = PositionConverter.fromLocation(trackFile.getSpawnLocation());
        this.checkpoints = CheckpointConverter.fromCheckpointDef(trackFile.getCheckpoints());
        this.gridLocations = trackFile.getGrid().stream().map(PositionConverter::fromLocation).toList();
        this.openBoatUtilsPackets = trackFile.getOpenBoatUtils();
        this.regions = RegionConverter.fromRegionDef(trackFile.getRegions());
        this.triggers = TriggerConverter.fromTriggerDef(trackFile.getTriggers());
        this.locations = LocationConverter.fromLocationDef(trackFile.getLocations());

        this.mapContainer = load.instanceContainer();

        for (Map.Entry<CrossCollider, Integer> entry : checkpoints.entrySet()) {
            CrossCollider checkpoint = entry.getKey();
            int index = entry.getValue();

            List<CrossCollider> checkpoints = checkpoint_lookup.computeIfAbsent(index, _ -> new ArrayList<>());

            checkpoints.add(checkpoint);
        }

        wrapIndex = checkpoints.values().stream().max(Integer::compareTo).orElse(-1) + 1;
    }

    public int wrapCheckpointIndex(int checkpoint) {
        if (!looped) return checkpoint % (wrapIndex + 1);

        return checkpoint % wrapIndex;
    }

    public boolean isLastCheckpoint(int checkpoint) {
        if (!looped) return checkpoint == (wrapIndex - 1);

        return checkpoint % wrapIndex == 0;
    }

    public @Subst(IceStom.NAMESPACE) @NonNull String getId() { return id; }

    public @NonNull Component getName() { return name; }

    public boolean isLooped() { return looped; }

    public @NonNull Pos getSpawnLocation() { return spawnLocation; }

    public @NonNull Map<CrossCollider, Integer> getCheckpoints() { return checkpoints; }

    public @NonNull List<Pos> getGridLocations() {
        return gridLocations;
    }

    public @NonNull List<OBUSettingsPacket> getOpenBoatUtilsPackets() {
        return openBoatUtilsPackets;
    }

    public @NonNull List<CrossCollider> getCheckpoints(int index) {
        return checkpoint_lookup.getOrDefault(index, List.of());
    }

    public Map<String, String> getTags() { return tags; }

    public Map<InsideCollider, Set<String>> getRegions() { return regions; }

    public Map<CrossCollider, Set<String>> getTriggers() { return triggers; }

    public Map<String, Pos> getLocations() { return locations; }

    public InstanceContainer getMapContainer() { return mapContainer; }

    public static RegistryKey<DimensionType> getDimensionKey(EnvironmentFile environmentFile) {

        DynamicRegistry<DimensionType> registry = MinecraftServer.getDimensionTypeRegistry();

        Key key = Key.key(IceStom.NAMESPACE, "d" + Objects.hash(
                environmentFile.getAmbientLight(),
                environmentFile.getNetherLight(),
                environmentFile.getMinY(),
                environmentFile.getHeight(),
                environmentFile.getSkybox(),
                environmentFile.getVersion()
        ));

        @Nullable RegistryKey<DimensionType> pre_existing = registry.getKey(key);

        if (pre_existing != null) return pre_existing;

        DimensionType.Builder builder = DimensionType.builder();

        builder.minY(environmentFile.getMinY());
        builder.height(environmentFile.getHeight());
        builder.logicalHeight(environmentFile.getHeight());
        builder.skybox(switch (environmentFile.getSkybox()) {
            case OVERWORLD -> DimensionType.Skybox.OVERWORLD;
            case END -> DimensionType.Skybox.END;
            case NONE -> DimensionType.Skybox.NONE;
        });
        builder.ambientLight(environmentFile.getAmbientLight());
        builder.cardinalLight(environmentFile.getNetherLight() ? DimensionType.CardinalLight.NETHER : DimensionType.CardinalLight.DEFAULT);

        try {
            return MinecraftServer.getDimensionTypeRegistry()
                    .register(key, builder.build());
        } catch (UnsupportedOperationException e) {
            log.warn("Couldn't find a suitable dimension type candidate for environment. (maybe preload failed?)");
            return DimensionType.OVERWORLD;
        }
    }
}
