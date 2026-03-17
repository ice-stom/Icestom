package io.gitlab.icestom.icestom.openboatutils;

import net.minestom.server.network.packet.server.common.PluginMessagePacket;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public sealed interface OpenBoatUtilsPacket
        permits OpenBoatUtilsPacket.ResetPacket,
        OpenBoatUtilsPacket.StepHeightPacket,
        OpenBoatUtilsPacket.DefaultSlipperinessPacket,
        OpenBoatUtilsPacket.BlocksSlipperinessPacket,
        OpenBoatUtilsPacket.BoatFallDamagePacket,
        OpenBoatUtilsPacket.BoatWaterElevationPacket,
        OpenBoatUtilsPacket.AirControlPacket,
        OpenBoatUtilsPacket.BoatJumpForcePacket,
        OpenBoatUtilsPacket.ModePacket,
        OpenBoatUtilsPacket.GravityPacket,
        OpenBoatUtilsPacket.YawAccelPacket,
        OpenBoatUtilsPacket.ForwardAccelPacket,
        OpenBoatUtilsPacket.BackwardAccelPacket,
        OpenBoatUtilsPacket.TurnAccelPacket,
        OpenBoatUtilsPacket.AllowAccelStackingPacket,
        OpenBoatUtilsPacket.ResendVersionPacket,
        OpenBoatUtilsPacket.UnderwaterControlPacket,
        OpenBoatUtilsPacket.SurfaceWaterControlPacket,
        OpenBoatUtilsPacket.ExclusiveModePacket,
        OpenBoatUtilsPacket.CoyoteTimePacket,
        OpenBoatUtilsPacket.WaterJumpingPacket,
        OpenBoatUtilsPacket.SwimForcePacket,
        OpenBoatUtilsPacket.RemoveBlocksSlipperinessPacket,
        OpenBoatUtilsPacket.ClearSlipperinessPacket,
        OpenBoatUtilsPacket.ModeSeriesPacket,
        OpenBoatUtilsPacket.ExclusiveModeSeriesPacket,
        OpenBoatUtilsPacket.PerBlockPacket,
        OpenBoatUtilsPacket.CollisionModePacket,
        OpenBoatUtilsPacket.StepWhileFallingPacket,
        OpenBoatUtilsPacket.InterpolationCompatPacket,
        OpenBoatUtilsPacket.CollisionResolutionPacket,
        OpenBoatUtilsPacket.AddCollisionEntityTypeFilterPacket,
        OpenBoatUtilsPacket.ClearCollisionEntityTypeFilterPacket {

    short getPacketId();

    default PacketByteBuf write() throws IOException {
        return new PacketByteBuf()
                .writeShort(getPacketId());
    }

    default PluginMessagePacket toPacket() throws IOException {
        return new PluginMessagePacket(getChannel(), write().toBytes());
    }

    default Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", getClass().getSimpleName());
        for (Field field : getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                map.put(field.getName(), field.get(this));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + field.getName(), e);
            }
        }
        return map;
    }

    static String getChannel() {
        return "openboatutils:settings";
    }

    static OpenBoatUtilsPacket fromMap(Map<String, Object> map) {
        String type = (String) map.get("type");
        if (type == null) throw new IllegalArgumentException("Map is missing 'type' field");

        try {
            Class<?> clazz = Class.forName(
                    OpenBoatUtilsPacket.class.getName() + "$" + type
            );

            OpenBoatUtilsPacket packet = (OpenBoatUtilsPacket) clazz.getDeclaredConstructor().newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = map.get(field.getName());
                if (value == null) continue;

                Class<?> fieldType = field.getType();
                if (fieldType == float.class && value instanceof Double d) {
                    value = d.floatValue();
                } else if (fieldType == short.class && value instanceof Long l) {
                    value = l.shortValue();
                } else if (fieldType == int.class && value instanceof Long l) {
                    value = l.intValue();
                } else if (fieldType == byte.class && value instanceof Long l) {
                    value = l.byteValue();
                } else if (fieldType == double.class && value instanceof Long l) {
                    value = l.doubleValue();
                }

                field.set(packet, value);
            }

            return packet;

        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown packet type: " + type, e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate packet: " + type, e);
        }
    }

    final class ResetPacket implements OpenBoatUtilsPacket {
        public short getPacketId() { return 0; }
    }

    final class StepHeightPacket implements OpenBoatUtilsPacket {
        public float step_height;

        public StepHeightPacket() {}
        public StepHeightPacket(float step_height) {
            this.step_height = step_height;
        }

        public short getPacketId() { return 1; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeFloat(step_height);
        }
    }

    final class DefaultSlipperinessPacket implements OpenBoatUtilsPacket {
        public float slipperiness;

        public DefaultSlipperinessPacket() {}
        public DefaultSlipperinessPacket(float slipperiness) {
            this.slipperiness = slipperiness;
        }

        public short getPacketId() { return 2; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeFloat(slipperiness);
        }
    }

    final class BlocksSlipperinessPacket implements OpenBoatUtilsPacket {
        public float slipperiness;
        public List<String> block_ids;

        public BlocksSlipperinessPacket() {}
        public BlocksSlipperinessPacket(float slipperiness, List<String> block_ids) {
            this.slipperiness = slipperiness;
            this.block_ids = block_ids;
        }

        public short getPacketId() { return 3; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeFloat(slipperiness)
                    .writeString(String.join(",", block_ids));
        }
    }

    final class BoatFallDamagePacket implements OpenBoatUtilsPacket {
        public boolean fall_damage;

        public BoatFallDamagePacket() {}
        public BoatFallDamagePacket(boolean fall_damage) {
            this.fall_damage = fall_damage;
        }

        public short getPacketId() { return 4; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeBoolean(fall_damage);
        }
    }

    final class BoatWaterElevationPacket implements OpenBoatUtilsPacket {
        public boolean water_elevation;

        public BoatWaterElevationPacket() {}
        public BoatWaterElevationPacket(boolean water_elevation) {
            this.water_elevation = water_elevation;
        }

        public short getPacketId() { return 5; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeBoolean(water_elevation);
        }
    }

    final class AirControlPacket implements OpenBoatUtilsPacket {
        public boolean air_control;

        public AirControlPacket() {}
        public AirControlPacket(boolean air_control) {
            this.air_control = air_control;
        }

        public short getPacketId() { return 6; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeBoolean(air_control);
        }
    }

    final class BoatJumpForcePacket implements OpenBoatUtilsPacket {
        public float jump_force;

        public BoatJumpForcePacket() {}
        public BoatJumpForcePacket(float jump_force) {
            this.jump_force = jump_force;
        }

        public short getPacketId() { return 7; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeFloat(jump_force);
        }
    }

    final class ModePacket implements OpenBoatUtilsPacket {
        public short mode_id;

        public ModePacket() {}
        public ModePacket(short mode_id) {
            this.mode_id = mode_id;
        }

        public short getPacketId() { return 8; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeShort(mode_id);
        }
    }

    final class GravityPacket implements OpenBoatUtilsPacket {
        public double gravity;

        public GravityPacket() {}
        public GravityPacket(double gravity) {
            this.gravity = gravity;
        }

        public short getPacketId() { return 9; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeDouble(gravity);
        }
    }

    final class YawAccelPacket implements OpenBoatUtilsPacket {
        public float acceleration;

        public YawAccelPacket() {}
        public YawAccelPacket(float acceleration) {
            this.acceleration = acceleration;
        }

        public short getPacketId() { return 10; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeFloat(acceleration);
        }
    }

    final class ForwardAccelPacket implements OpenBoatUtilsPacket {
        public float acceleration;

        public ForwardAccelPacket() {}
        public ForwardAccelPacket(float acceleration) {
            this.acceleration = acceleration;
        }

        public short getPacketId() { return 11; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeFloat(acceleration);
        }
    }

    final class BackwardAccelPacket implements OpenBoatUtilsPacket {
        public float acceleration;

        public BackwardAccelPacket() {}
        public BackwardAccelPacket(float acceleration) {
            this.acceleration = acceleration;
        }

        public short getPacketId() { return 12; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeFloat(acceleration);
        }
    }

    final class TurnAccelPacket implements OpenBoatUtilsPacket {
        public float acceleration;

        public TurnAccelPacket() {}
        public TurnAccelPacket(float acceleration) {
            this.acceleration = acceleration;
        }

        public short getPacketId() { return 13; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeFloat(acceleration);
        }
    }

    final class AllowAccelStackingPacket implements OpenBoatUtilsPacket {
        public boolean enabled;

        public AllowAccelStackingPacket() {}
        public AllowAccelStackingPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getPacketId() { return 14; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeBoolean(enabled);
        }
    }

    final class ResendVersionPacket implements OpenBoatUtilsPacket {
        public short getPacketId() { return 15; }
    }

    final class UnderwaterControlPacket implements OpenBoatUtilsPacket {
        public boolean enabled;

        public UnderwaterControlPacket() {}
        public UnderwaterControlPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getPacketId() { return 16; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeBoolean(enabled);
        }
    }

    final class SurfaceWaterControlPacket implements OpenBoatUtilsPacket {
        public boolean enabled;

        public SurfaceWaterControlPacket() {}
        public SurfaceWaterControlPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getPacketId() { return 17; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeBoolean(enabled);
        }
    }

    final class ExclusiveModePacket implements OpenBoatUtilsPacket {
        public short mode_id;

        public ExclusiveModePacket() {}
        public ExclusiveModePacket(short mode_id) {
            this.mode_id = mode_id;
        }

        public short getPacketId() { return 18; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeShort(mode_id);
        }
    }

    final class CoyoteTimePacket implements OpenBoatUtilsPacket {
        public int ticks;

        public CoyoteTimePacket() {}
        public CoyoteTimePacket(int ticks) {
            this.ticks = ticks;
        }

        public short getPacketId() { return 19; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeInt(ticks);
        }
    }

    final class WaterJumpingPacket implements OpenBoatUtilsPacket {
        public boolean enabled;

        public WaterJumpingPacket() {}
        public WaterJumpingPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getPacketId() { return 20; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeBoolean(enabled);
        }
    }

    final class SwimForcePacket implements OpenBoatUtilsPacket {
        public float force;

        public SwimForcePacket() {}
        public SwimForcePacket(float force) {
            this.force = force;
        }

        public short getPacketId() { return 21; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeFloat(force);
        }
    }

    final class RemoveBlocksSlipperinessPacket implements OpenBoatUtilsPacket {
        public List<String> block_ids;

        public RemoveBlocksSlipperinessPacket() {}
        public RemoveBlocksSlipperinessPacket(List<String> block_ids) {
            this.block_ids = block_ids;
        }

        public short getPacketId() { return 22; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeString(String.join(",", block_ids));
        }
    }

    final class ClearSlipperinessPacket implements OpenBoatUtilsPacket {
        public short getPacketId() { return 23; }
    }

    final class ModeSeriesPacket implements OpenBoatUtilsPacket {
        public List<Short> mode_ids;

        public ModeSeriesPacket() {}
        public ModeSeriesPacket(List<Short> mode_ids) {
            this.mode_ids = mode_ids;
        }

        public short getPacketId() { return 24; }
        public PacketByteBuf write() throws IOException {
            PacketByteBuf buf = OpenBoatUtilsPacket.super.write();
            buf.writeShort((short) mode_ids.size());
            for (short id : mode_ids) buf.writeShort(id);
            return buf;
        }
    }

    final class ExclusiveModeSeriesPacket implements OpenBoatUtilsPacket {
        public List<Short> mode_ids;

        public ExclusiveModeSeriesPacket() {}
        public ExclusiveModeSeriesPacket(List<Short> mode_ids) {
            this.mode_ids = mode_ids;
        }

        public short getPacketId() { return 25; }
        public PacketByteBuf write() throws IOException {
            PacketByteBuf buf = OpenBoatUtilsPacket.super.write();
            buf.writeShort((short) mode_ids.size());
            for (short id : mode_ids) buf.writeShort(id);
            return buf;
        }
    }

    final class PerBlockPacket implements OpenBoatUtilsPacket {
        public PerBlockSetting setting;
        public float value;
        public List<String> block_ids;

        public PerBlockPacket() {}
        public PerBlockPacket(PerBlockSetting setting, float value, List<String> block_ids) {
            this.setting = setting;
            this.value = value;
            this.block_ids = block_ids;
        }

        public short getPacketId() { return 26; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeShort((short) setting.ordinal())
                    .writeFloat(value)
                    .writeString(String.join(",", block_ids));
        }
    }

    final class CollisionModePacket implements OpenBoatUtilsPacket {
        public CollisionMode mode;

        public CollisionModePacket() {}
        public CollisionModePacket(CollisionMode mode) {
            this.mode = mode;
        }

        public short getPacketId() { return 27; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeShort((short) mode.ordinal());
        }
    }

    final class StepWhileFallingPacket implements OpenBoatUtilsPacket {
        public boolean enabled;

        public StepWhileFallingPacket() {}
        public StepWhileFallingPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getPacketId() { return 28; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeBoolean(enabled);
        }
    }

    final class InterpolationCompatPacket implements OpenBoatUtilsPacket {
        public boolean enabled;

        public InterpolationCompatPacket() {}
        public InterpolationCompatPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getPacketId() { return 29; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeBoolean(enabled);
        }
    }

    final class CollisionResolutionPacket implements OpenBoatUtilsPacket {
        public byte resolution;

        public CollisionResolutionPacket() {}
        public CollisionResolutionPacket(byte resolution) {
            this.resolution = resolution;
        }

        public short getPacketId() { return 30; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeByte(resolution);
        }
    }

    final class AddCollisionEntityTypeFilterPacket implements OpenBoatUtilsPacket {
        public List<String> entity_ids;

        public AddCollisionEntityTypeFilterPacket() {}
        public AddCollisionEntityTypeFilterPacket(List<String> entity_ids) {
            this.entity_ids = entity_ids;
        }

        public short getPacketId() { return 31; }
        public PacketByteBuf write() throws IOException {
            return OpenBoatUtilsPacket.super.write()
                    .writeString(String.join(",", entity_ids));
        }
    }

    final class ClearCollisionEntityTypeFilterPacket implements OpenBoatUtilsPacket {
        public short getPacketId() { return 32; }
    }

    enum PerBlockSetting {
        JUMP_FORCE,
        FORWARD_ACCEL,
        BACKWARD_ACCEL,
        YAW_ACCEL,
        TURN_FORWARD_ACCEL
    }

    enum CollisionMode {
        VANILLA,
        NO_BOATS_AND_PLAYERS,
        NO_ENTITIES,
        FILTERED,
        NO_BOATS_AND_PLAYERS_AND_FILTERED
    }
}