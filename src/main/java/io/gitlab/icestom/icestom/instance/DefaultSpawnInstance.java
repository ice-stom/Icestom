package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.IceStom;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class DefaultSpawnInstance extends IceStomInstance implements SpawnInstance {

    private static final Logger log = LoggerFactory.getLogger(DefaultSpawnInstance.class);

    public DefaultSpawnInstance() {
        super(UUID.randomUUID(), DimensionType.OVERWORLD, Key.key(IceStom.NAMESPACE, "spawn"));

        PolarLoader polarLoader = null;

        Path worldPath = Path.of("spawn.polar");

        if (worldPath.toFile().exists()) {
            try (InputStream stream = Files.newInputStream(worldPath)) {
                polarLoader = new PolarLoader(stream);
                log.info("Loading spawn.polar.");
            } catch (IOException exception) {
                log.error("Failed to load spawn world: Failed to load spawn.polar: {}", String.valueOf(exception));
                return;
            }
        }

        if (polarLoader == null) {
            try (@Nullable InputStream stream = getClass().getResourceAsStream("/spawn.polar")) {
                if (stream == null) {
                    log.error("Failed to load spawn world: no resource found");
                    return;
                }

                polarLoader = new PolarLoader(stream);
            } catch (IOException exception) {
                log.error("Failed to load internal spawn world: {}", String.valueOf(exception));
                return;
            }
        }

        setChunkLoader(polarLoader);
        setChunkSupplier(LightingChunk::new);
    }

    @Override
    public void init() {
        Entity entity = new Entity(EntityType.TEXT_DISPLAY);
        entity.setNoGravity(true);
        TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();

        meta.setBackgroundColor(0);
        meta.setText(Component.text("Spawn"));
        meta.setBrightness(15, 15);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setScale(Vec.ONE.mul(4));

        entity.setInstance(this, new Pos(0, 2, 0));
    }

    @Override
    public Pos spawnLocation(Player player) {
        return Pos.ZERO;
    }
}
