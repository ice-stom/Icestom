package io.gitlab.icestom.icestom.util;

import net.kyori.adventure.nbt.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.adventure.serializer.nbt.NbtComponentSerializer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface DisplayEntityConverter {

    class DisplayEntity extends Entity {
        public DisplayEntity(EntityType entityType) {
            super(entityType);

            hasPhysics = false;
            setNoGravity(true);
        }
    }

    static Block parseBlockState(@NotNull CompoundBinaryTag blockState) {
        String name = blockState.getString("Name");

        Block block = Block.fromKey(name);

        CompoundBinaryTag properties = blockState.getCompound("Properties");

        for (String propertyName : properties.keySet()) {
            String value = properties.getString(propertyName);

            block = block.withProperty(propertyName, value);
        }

        return block;
    }

    static void applyDisplayProperties(
            @NotNull AbstractDisplayMeta meta,
            @NotNull CompoundBinaryTag nbt
    ) {
        if (nbt.contains("transformation")) {
            CompoundBinaryTag transformation =
                    nbt.getCompound("transformation");

            // translation
            if (transformation.contains("translation")) {
                ListBinaryTag translation =
                        transformation.getList("translation");

                meta.setTranslation(new Vec(
                        translation.getFloat(0),
                        translation.getFloat(1),
                        translation.getFloat(2)
                ));
            }

            // scale
            if (transformation.contains("scale")) {
                ListBinaryTag scale =
                        transformation.getList("scale");

                meta.setScale(new Vec(
                        scale.getFloat(0),
                        scale.getFloat(1),
                        scale.getFloat(2)
                ));
            }

            // left/right rotation are quaternion values.
            if (transformation.contains("left_rotation")) {
                ListBinaryTag rotation =
                        transformation.getList("left_rotation");

                meta.setLeftRotation(new float[]{
                        rotation.getFloat(0),
                        rotation.getFloat(1),
                        rotation.getFloat(2),
                        rotation.getFloat(3)
                });
            }

            if (transformation.contains("right_rotation")) {
                ListBinaryTag rotation =
                        transformation.getList("right_rotation");

                meta.setRightRotation(new float[]{
                        rotation.getFloat(0),
                        rotation.getFloat(1),
                        rotation.getFloat(2),
                        rotation.getFloat(3)
                });
            }
        }

        if (nbt.contains("billboard_render_constraints")) {
            int billboard = nbt.getByte("billboard_render_constraints");

            meta.setBillboardRenderConstraints(
                    switch (billboard) {
                        case 0 -> AbstractDisplayMeta.BillboardConstraints.FIXED;
                        case 1 -> AbstractDisplayMeta.BillboardConstraints.VERTICAL;
                        case 2 -> AbstractDisplayMeta.BillboardConstraints.HORIZONTAL;
                        case 3 -> AbstractDisplayMeta.BillboardConstraints.CENTER;
                        default -> AbstractDisplayMeta.BillboardConstraints.FIXED;
                    }
            );
        }

        if (nbt.contains("view_range")) {
            meta.setViewRange(nbt.getFloat("view_range"));
        }

        if (nbt.contains("shadow_radius")) {
            meta.setShadowRadius(nbt.getFloat("shadow_radius"));
        }

        if (nbt.contains("shadow_strength")) {
            meta.setShadowStrength(nbt.getFloat("shadow_strength"));
        }

        if (nbt.contains("width")) {
            meta.setWidth(nbt.getFloat("width"));
        }

        if (nbt.contains("height")) {
            meta.setHeight(nbt.getFloat("height"));
        }

        if (nbt.contains("brightness")) {
            int brightness = nbt.getInt("brightness");

            int blockLight = brightness & 0xFFFF;
            int skyLight = (brightness >> 16) & 0xFFFF;

            meta.setBrightness(blockLight, skyLight);
        }
    }

    static @NotNull DisplayEntity loadBlockDisplay(@NotNull CompoundBinaryTag nbt) {
        DisplayEntity entity = new DisplayEntity(EntityType.BLOCK_DISPLAY);

        BlockDisplayMeta meta = (BlockDisplayMeta) entity.getEntityMeta();

        CompoundBinaryTag blockState = nbt.getCompound("block_state");

        String blockName = blockState.getString("Name");

        Block block = Block.fromState(
                blockName + parseBlockState(blockState.getCompound("Properties"))
        );

        meta.setBlockState(block);

        applyDisplayProperties(meta, nbt);

        return entity;
    }

    static @NotNull DisplayEntity loadItemDisplay(@NotNull CompoundBinaryTag nbt) {
        DisplayEntity entity = new DisplayEntity(EntityType.ITEM_DISPLAY);

        ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();

        CompoundBinaryTag item = nbt.getCompound("item");

        ItemStack stack = ItemStack.fromItemNBT(item);

        meta.setItemStack(stack);

        byte displayType = nbt.getByte("item_display");

        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.values()[displayType]);

        applyDisplayProperties((AbstractDisplayMeta) meta, nbt);

        return entity;
    }

    static @NotNull DisplayEntity loadTextDisplay(@NotNull CompoundBinaryTag nbt) {
        DisplayEntity entity = new DisplayEntity(EntityType.TEXT_DISPLAY);

        TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();

        if (nbt.contains("text")) {
            meta.setText(decodeTextComponent(nbt.get("text")));
        }

        if (nbt.contains("line_width")) {
            meta.setLineWidth(nbt.getInt("line_width"));
        }

        byte flags = nbt.contains("flags") ? nbt.getByte("flags") : 0;

        boolean hasShadow = (flags & 0x01) != 0;
        boolean seeThrough = (flags & 0x02) != 0;
        boolean useDefaultBackground = (flags & 0x04) != 0;
        boolean alignLeft = (flags & 0x08) != 0;
        boolean alignRight = (flags & 0x10) != 0;

        meta.setShadow(hasShadow);
        meta.setSeeThrough(seeThrough);

        if (useDefaultBackground) {
            meta.setUseDefaultBackground(true);
        } else if (nbt.contains("background")) {
            meta.setUseDefaultBackground(false);
            meta.setBackgroundColor(nbt.getInt("background"));
        }

        if (nbt.contains("text_opacity")) {
            meta.setTextOpacity(nbt.getByte("text_opacity"));
        }

        TextDisplayMeta.Alignment alignment = TextDisplayMeta.Alignment.CENTER;
        if (alignLeft) {
            alignment = TextDisplayMeta.Alignment.LEFT;
        } else if (alignRight) {
            alignment = TextDisplayMeta.Alignment.RIGHT;
        }
        meta.setAlignment(alignment);

        applyDisplayProperties(meta, nbt);

        return entity;
    }


    private static Component decodeTextComponent(BinaryTag tag) {
        if (tag == null) {
            return Component.empty();
        }

        if (tag.type() == BinaryTagTypes.COMPOUND || tag.type() == BinaryTagTypes.LIST) {
            return NbtComponentSerializer.nbt().deserialize(tag);
        }

        if (tag.type() == BinaryTagTypes.STRING) {
            String raw = ((StringBinaryTag) tag).value();
            try {
                return GsonComponentSerializer.gson().deserialize(raw);
            } catch (Exception e) {
                return Component.text(raw);
            }
        }

        return Component.empty();
    }
}
