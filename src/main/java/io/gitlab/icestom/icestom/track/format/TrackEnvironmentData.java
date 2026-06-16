package io.gitlab.icestom.icestom.track.format;

import io.gitlab.icestom.icestom.IceStom;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.attribute.BackgroundMusic;
import net.minestom.server.world.attribute.EnvironmentAttribute;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;

public record TrackEnvironmentData(
    int ambient_light,
    int min_y,
    int height,
    boolean nether_light,
    String skybox
) {
    static DimensionType.Skybox getSkybox(String string) {
        return switch (string) {
            case "OVERWORLD" -> DimensionType.Skybox.OVERWORLD;
            case "END" -> DimensionType.Skybox.END;
            default -> DimensionType.Skybox.NONE;
        };
    }

    private DimensionType getDimensionType() {

        DimensionType.Builder builder = DimensionType.builder();

        DimensionType.Skybox skybox1 = getSkybox(skybox);

        builder.minY(min_y);
        builder.height(height);
        builder.logicalHeight(height);
        builder.skybox(skybox1);
        builder.ambientLight(ambient_light);
//        builder.cardinalLight(nether_light ? DimensionType.CardinalLight.NETHER : DimensionType.CardinalLight.DEFAULT);

        builder.setAttribute(
                EnvironmentAttribute.BACKGROUND_MUSIC,
                BackgroundMusic.EMPTY
        );

        return builder.build();
    }

    public RegistryKey<DimensionType> getKey() {

        DynamicRegistry<DimensionType> registry = MinecraftServer.getDimensionTypeRegistry();

        Key key = Key.key(IceStom.NAMESPACE, "d" + hashCode());

        @Nullable RegistryKey<DimensionType> pre_existing = registry.getKey(key);

        if (pre_existing != null) return pre_existing;

        return MinecraftServer.getDimensionTypeRegistry()
                .register(key, getDimensionType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(ambient_light, min_y, height, nether_light, skybox);
    }
}
