package io.gitlab.icestom.icestom.openboatutils;

import net.kyori.adventure.key.Key;

import java.io.IOException;
import java.util.UUID;

public sealed interface OBUContextPackets extends OBUPacket
        permits OBUContextPackets.ResetContext,
        OBUContextPackets.SwitchContext,
        OBUContextPackets.DropContext,
        OBUContextPackets.StoreContext,
        OBUContextPackets.EntityContext {

    static String getChannel() {
        return "openboatutils:context";
    }

    @Override
    default short getPacketId() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    default short getVersion() {
        throw new RuntimeException("Not Implemented");
    }

    final class ResetContext implements OBUContextPackets {
        public ResetContext() {}

        public short getVersion() { return 19; }
        public short getPacketId() { return 0; }
    }

    final class SwitchContext implements OBUContextPackets {
        public Key key;

        public SwitchContext() {}
        public SwitchContext(Key key) { this.key = key; }

        public short getVersion() { return 19; }
        public short getPacketId() { return 1; }

        @Override
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUContextPackets.super.write(buf)
                    .writeString(key.toString());
        }
    }

    final class DropContext implements OBUContextPackets {
        public Key key;

        public DropContext() {}
        public DropContext(Key key) { this.key = key; }

        public short getVersion() { return 19; }
        public short getPacketId() { return 2; }

        @Override
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            return OBUContextPackets.super.write(buf)
                    .writeString(key.toString());
        }
    }

    final class StoreContext implements OBUContextPackets {
        public Key key;
        public GroupedPacketPayload transaction;

        public StoreContext() {}
        public StoreContext(Key key, GroupedPacketPayload transaction) { this.key = key; this.transaction = transaction; }

        public short getVersion() { return 19; }
        public short getPacketId() { return 3; }

        @Override
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            OBUContextPackets.super.write(buf)
                    .writeString(key.toString());
            transaction.write(buf);
            return buf;
        }
    }

    final class EntityContext implements OBUContextPackets {
        public UUID uuid;
        public GroupedPacketPayload transaction;

        public EntityContext() {}
        public EntityContext(UUID uuid, GroupedPacketPayload transaction) { this.uuid = uuid; this.transaction = transaction; }

        public short getVersion() { return 19; }
        public short getPacketId() { return 4; }

        @Override
        public PacketByteBuf write(PacketByteBuf buf) throws IOException {
            OBUContextPackets.super.write(buf)
                    .writeString(uuid.toString());
            transaction.write(buf);
            return buf;
        }
    }
}