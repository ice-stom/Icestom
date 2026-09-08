package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.util.TextFormatter;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
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
        {
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

        {

            TextComponent.Builder builder = Component.text();

            for (int i = 0; i < 9; i++) {
                builder.appendSpace();
                builder.append(TextFormatter.getIcestomLogoRow(i));
                builder.appendSpace();
                builder.appendSpace();

                if (i == 3) {

                    builder.append(Component.text("I", TextColor.color(TextFormatter.ICESTOM_LOGO_COLOR[0])));
                    builder.append(Component.text("c", TextColor.color(TextFormatter.ICESTOM_LOGO_COLOR[1])));
                    builder.append(Component.text("e", TextColor.color(TextFormatter.ICESTOM_LOGO_COLOR[2])));
                    builder.append(Component.text("St", TextColor.color(TextFormatter.ICESTOM_LOGO_COLOR[3])));
                    builder.append(Component.text("om", TextColor.color(TextFormatter.ICESTOM_LOGO_COLOR[4])));

                    builder.appendSpace();

                    builder.append(Component.text("v" + IceStom.VERSION));
                } else if (i == 4) {
                    builder.append(Component.text("\"probably on a test server\""));
                } else if (i == 5) {
                    builder.append(Component.text("- microwavedram"));
                }

                builder.appendSpace();

                if (i != 8) builder.appendNewline();
            }

            Entity entity = new Entity(EntityType.TEXT_DISPLAY);
            entity.setNoGravity(true);
            TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();

            meta.setBackgroundColor(0);
            meta.setText(builder.build());
            meta.setBrightness(15, 15);
            meta.setScale(Vec.ONE.mul(500));
            meta.setAlignLeft(true);
            meta.setLineWidth(500);

            meta.setTranslation(new Vec(0, 0, -1000));

            entity.setInstance(this, new Pos(0, 0, 0).withYaw(180));

//            setBlockArea(Area.box(new Vec(0, 6, 12), new Vec(9, 3, 0)), Block.WHITE_CONCRETE);
        }
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        for (Player player : getPlayers()) {
            if (player.getPosition().y() < -64) {
                resetPlayer(player);
            }
        }
    }

    @Override
    public Pos spawnLocation(Player player) {
        return Pos.ZERO;
    }
}
