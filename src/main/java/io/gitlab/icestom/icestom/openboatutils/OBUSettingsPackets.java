package io.gitlab.icestom.icestom.openboatutils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public sealed interface OBUSettingsPackets extends OBUPacket
        permits OBUSettingsPackets.ResetPacket,
        OBUSettingsPackets.StepHeightPacket,
        OBUSettingsPackets.DefaultSlipperinessPacket,
        OBUSettingsPackets.BlocksSlipperinessPacket,
        OBUSettingsPackets.BoatFallDamagePacket,
        OBUSettingsPackets.BoatWaterElevationPacket,
        OBUSettingsPackets.AirControlPacket,
        OBUSettingsPackets.BoatJumpForcePacket,
        OBUSettingsPackets.ModePacket,
        OBUSettingsPackets.GravityPacket,
        OBUSettingsPackets.YawAccelPacket,
        OBUSettingsPackets.ForwardAccelPacket,
        OBUSettingsPackets.BackwardAccelPacket,
        OBUSettingsPackets.TurnAccelPacket,
        OBUSettingsPackets.AllowAccelStackingPacket,
        OBUSettingsPackets.ResendVersionPacket,
        OBUSettingsPackets.UnderwaterControlPacket,
        OBUSettingsPackets.SurfaceWaterControlPacket,
        OBUSettingsPackets.ExclusiveModePacket,
        OBUSettingsPackets.CoyoteTimePacket,
        OBUSettingsPackets.WaterJumpingPacket,
        OBUSettingsPackets.SwimForcePacket,
        OBUSettingsPackets.RemoveBlocksSlipperinessPacket,
        OBUSettingsPackets.ClearSlipperinessPacket,
        OBUSettingsPackets.ModeSeriesPacket,
        OBUSettingsPackets.ExclusiveModeSeriesPacket,
        OBUSettingsPackets.PerBlockPacket,
        OBUSettingsPackets.CollisionModePacket,
        OBUSettingsPackets.StepWhileFallingPacket,
        OBUSettingsPackets.InterpolationCompatPacket,
        OBUSettingsPackets.CollisionResolutionPacket,
        OBUSettingsPackets.AddCollisionEntityTypeFilterPacket,
        OBUSettingsPackets.ClearCollisionEntityTypeFilterPacket,
        OBUSettingsPackets.TransactionPacket,
        OBUSettingsPackets.SetWalltapMultiplier,
        OBUSettingsPackets.SetJumps,
        OBUSettingsPackets.SetScale,
        OBUSettingsPackets.SetStepUpSlipperiness,
        OBUSettingsPackets.SetResetOnWorldLoad {

    @Override
    default short getPacketId() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    default short getVersion() {
        throw new RuntimeException("Not Implemented");
    }

    static String getChannel() {
        return "openboatutils:settings";
    }

    static OBUSettingsPackets fromMap(Map<String, Object> map) {
        String type = (String) map.get("type");
        if (type == null) throw new IllegalArgumentException("Map is missing 'type' field");

        try {
            Class<?> clazz = Class.forName(
                    OBUSettingsPackets.class.getName() + "$" + type
            );

            OBUSettingsPackets packet = (OBUSettingsPackets) clazz.getDeclaredConstructor().newInstance();

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

    final class ResetPacket implements OBUSettingsPackets {
        public short getVersion() { return 16; }
        public short getPacketId() { return 0; }
    }

    final class StepHeightPacket implements OBUSettingsPackets {
        public float height;

        public StepHeightPacket() {}
        public StepHeightPacket(float height) {
            this.height = height;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 1; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeFloat(height);
        }
    }

    final class DefaultSlipperinessPacket implements OBUSettingsPackets {
        public float slipperiness;

        public DefaultSlipperinessPacket() {}
        public DefaultSlipperinessPacket(float slipperiness) {
            this.slipperiness = slipperiness;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 2; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeFloat(slipperiness);
        }
    }

    final class BlocksSlipperinessPacket implements OBUSettingsPackets {
        public float slipperiness;
        public List<String> block_ids;

        public BlocksSlipperinessPacket() {}
        public BlocksSlipperinessPacket(float slipperiness, List<String> block_ids) {
            this.slipperiness = slipperiness;
            this.block_ids = block_ids;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 3; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeFloat(slipperiness)
                    .writeString(String.join(",", block_ids));
        }
    }

    final class BoatFallDamagePacket implements OBUSettingsPackets {
        public boolean enabled;

        public BoatFallDamagePacket() {}
        public BoatFallDamagePacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 4; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeBoolean(enabled);
        }
    }

    final class BoatWaterElevationPacket implements OBUSettingsPackets {
        public boolean enabled;

        public BoatWaterElevationPacket() {}
        public BoatWaterElevationPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 5; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeBoolean(enabled);
        }
    }

    final class AirControlPacket implements OBUSettingsPackets {
        public boolean enabled;

        public AirControlPacket() {}
        public AirControlPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 6; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeBoolean(enabled);
        }
    }

    final class BoatJumpForcePacket implements OBUSettingsPackets {
        public float force;

        public BoatJumpForcePacket() {}
        public BoatJumpForcePacket(float force) {
            this.force = force;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 7; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeFloat(force);
        }
    }

    final class ModePacket implements OBUSettingsPackets {
        public short mode_id;

        public ModePacket() {}
        public ModePacket(short mode_id) {
            this.mode_id = mode_id;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 8; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeShort(mode_id);
        }
    }

    final class GravityPacket implements OBUSettingsPackets {
        public double gravity;

        public GravityPacket() {}
        public GravityPacket(double gravity) {
            this.gravity = gravity;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 9; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeDouble(gravity);
        }
    }

    final class YawAccelPacket implements OBUSettingsPackets {
        public float accel;

        public YawAccelPacket() {}
        public YawAccelPacket(float accel) {
            this.accel = accel;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 10; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeFloat(accel);
        }
    }

    final class ForwardAccelPacket implements OBUSettingsPackets {
        public float accel;

        public ForwardAccelPacket() {}
        public ForwardAccelPacket(float accel) {
            this.accel = accel;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 11; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeFloat(accel);
        }
    }

    final class BackwardAccelPacket implements OBUSettingsPackets {
        public float accel;

        public BackwardAccelPacket() {}
        public BackwardAccelPacket(float accel) {
            this.accel = accel;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 12; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeFloat(accel);
        }
    }

    final class TurnAccelPacket implements OBUSettingsPackets {
        public float accel;

        public TurnAccelPacket() {}
        public TurnAccelPacket(float accel) {
            this.accel = accel;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 13; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeFloat(accel);
        }
    }

    final class AllowAccelStackingPacket implements OBUSettingsPackets {
        public boolean enabled;

        public AllowAccelStackingPacket() {}
        public AllowAccelStackingPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 14; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeBoolean(enabled);
        }
    }

    final class ResendVersionPacket implements OBUSettingsPackets {
        public short getVersion() { return 16; }
        public short getPacketId() { return 15; }
    }

    final class UnderwaterControlPacket implements OBUSettingsPackets {
        public boolean enabled;

        public UnderwaterControlPacket() {}
        public UnderwaterControlPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 16; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeBoolean(enabled);
        }
    }

    final class SurfaceWaterControlPacket implements OBUSettingsPackets {
        public boolean enabled;

        public SurfaceWaterControlPacket() {}
        public SurfaceWaterControlPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 17; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeBoolean(enabled);
        }
    }

    final class ExclusiveModePacket implements OBUSettingsPackets {
        public short mode_id;

        public ExclusiveModePacket() {}
        public ExclusiveModePacket(short mode_id) {
            this.mode_id = mode_id;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 18; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeShort(mode_id);
        }
    }

    final class CoyoteTimePacket implements OBUSettingsPackets {
        public int ticks;

        public CoyoteTimePacket() {}
        public CoyoteTimePacket(int ticks) {
            this.ticks = ticks;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 19; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeInt(ticks);
        }
    }

    final class WaterJumpingPacket implements OBUSettingsPackets {
        public boolean enabled;

        public WaterJumpingPacket() {}
        public WaterJumpingPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 20; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeBoolean(enabled);
        }
    }

    final class SwimForcePacket implements OBUSettingsPackets {
        public float force;

        public SwimForcePacket() {}
        public SwimForcePacket(float force) {
            this.force = force;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 21; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeFloat(force);
        }
    }

    final class RemoveBlocksSlipperinessPacket implements OBUSettingsPackets {
        public List<String> block_ids;

        public RemoveBlocksSlipperinessPacket() {}
        public RemoveBlocksSlipperinessPacket(List<String> block_ids) {
            this.block_ids = block_ids;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 22; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeString(String.join(",", block_ids));
        }
    }

    final class ClearSlipperinessPacket implements OBUSettingsPackets {
        public short getVersion() { return 16; }
        public short getPacketId() { return 23; }
    }

    final class ModeSeriesPacket implements OBUSettingsPackets {
        public List<Short> mode_ids;

        public ModeSeriesPacket() {}
        public ModeSeriesPacket(List<Short> mode_ids) {
            this.mode_ids = mode_ids;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 24; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            OBUSettingsPackets.super.write(buf);
            buf.writeShort((short) mode_ids.size());
            for (short id : mode_ids) buf.writeShort(id);
            return buf;
        }
    }

    final class ExclusiveModeSeriesPacket implements OBUSettingsPackets {
        public List<Short> mode_ids;

        public ExclusiveModeSeriesPacket() {}
        public ExclusiveModeSeriesPacket(List<Short> mode_ids) {
            this.mode_ids = mode_ids;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 25; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            OBUSettingsPackets.super.write(buf);
            buf.writeShort((short) mode_ids.size());
            for (short id : mode_ids) buf.writeShort(id);
            return buf;
        }
    }

    final class PerBlockPacket implements OBUSettingsPackets {
        public PerBlockSetting setting;
        public float value;
        public List<String> block_ids;

        public PerBlockPacket() {}
        public PerBlockPacket(PerBlockSetting setting, float value, List<String> block_ids) {
            this.setting = setting;
            this.value = value;
            this.block_ids = block_ids;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 26; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeShort((short) setting.ordinal())
                    .writeFloat(value)
                    .writeString(String.join(",", block_ids));
        }
    }

    final class CollisionModePacket implements OBUSettingsPackets {
        public CollisionMode mode;

        public CollisionModePacket() {}
        public CollisionModePacket(CollisionMode mode) {
            this.mode = mode;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 27; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeShort((short) mode.ordinal());
        }
    }

    final class StepWhileFallingPacket implements OBUSettingsPackets {
        public boolean enabled;

        public StepWhileFallingPacket() {}
        public StepWhileFallingPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 28; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeBoolean(enabled);
        }
    }

    final class InterpolationCompatPacket implements OBUSettingsPackets {
        public boolean enabled;

        public InterpolationCompatPacket() {}
        public InterpolationCompatPacket(boolean enabled) {
            this.enabled = enabled;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 29; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeBoolean(enabled);
        }
    }

    final class CollisionResolutionPacket implements OBUSettingsPackets {
        public byte resolution;

        public CollisionResolutionPacket() {}
        public CollisionResolutionPacket(byte resolution) {
            this.resolution = resolution;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 30; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeByte(resolution);
        }
    }

    final class AddCollisionEntityTypeFilterPacket implements OBUSettingsPackets {
        public List<String> entity_ids;

        public AddCollisionEntityTypeFilterPacket() {}
        public AddCollisionEntityTypeFilterPacket(List<String> entity_ids) {
            this.entity_ids = entity_ids;
        }

        public short getVersion() { return 16; }
        public short getPacketId() { return 31; }
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeString(String.join(",", entity_ids));
        }
    }

    final class ClearCollisionEntityTypeFilterPacket implements OBUSettingsPackets {
        public short getVersion() { return 16; }
        public short getPacketId() { return 32; }
    }

    final class TransactionPacket implements OBUSettingsPackets {
        public GroupedPacketPayload groupedPacketPayload;

        public TransactionPacket() {}
        public TransactionPacket(GroupedPacketPayload groupedPacketPayload) { this.groupedPacketPayload = groupedPacketPayload; }

        public short getVersion() { return 19; }
        public short getPacketId() { return 33; }

        @Override
        public Map<String, Object> toMap() {
            throw new RuntimeException("You can't serialize a transaction packet");
        }

        @Override
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            OBUSettingsPackets.super.write(buf);
            groupedPacketPayload.write(buf);

            return buf;
        }
    }

    final class SetWalltapMultiplier implements OBUSettingsPackets {
        public float multiplier;

        public SetWalltapMultiplier() {}
        public SetWalltapMultiplier(float multiplier) { this.multiplier = multiplier; }

        public short getVersion() { return 19; }
        public short getPacketId() { return 34; }

        @Override
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeFloat(multiplier);
        }
    }

    final class SetJumps implements OBUSettingsPackets {
        public int jumps;

        public SetJumps() {}
        public SetJumps(int jumps) { this.jumps = jumps; }

        public short getVersion() { return 19; }
        public short getPacketId() { return 35; }

        @Override
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeInt(jumps);
        }
    }

    final class SetScale implements OBUSettingsPackets {
        public float scale;

        public SetScale() {}
        public SetScale(float scale) { this.scale = scale; }

        public short getVersion() { return 19; }
        public short getPacketId() { return 36; }

        @Override
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeFloat(scale);
        }
    }

    final class SetStepUpSlipperiness implements OBUSettingsPackets {
        public float slipperiness;

        public SetStepUpSlipperiness() {}
        public SetStepUpSlipperiness(float slipperiness) { this.slipperiness = slipperiness; }

        public short getVersion() { return 19; }
        public short getPacketId() { return 37; }

        @Override
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeFloat(slipperiness);
        }
    }

    final class SetResetOnWorldLoad implements OBUSettingsPackets {
        public boolean enabled;

        public SetResetOnWorldLoad() {}
        public SetResetOnWorldLoad(boolean enabled) { this.enabled = enabled; }

        public short getVersion() { return 19; }
        public short getPacketId() { return 38; }

        @Override
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUSettingsPackets.super.write(buf)
                    .writeBoolean(enabled);
        }
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