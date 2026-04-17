package io.gitlab.icestom.icestom.openboatutils;

import java.io.IOException;
import java.util.List;

public class TransactionPayload {
    public List<OpenBoatUtilsPacket> packets;

    public TransactionPayload(List<OpenBoatUtilsPacket> packets) { this.packets = packets; }

    public PacketByteBuf write(PacketByteBuf buf) throws IOException {
        buf.writeInt(packets.size());

        for (OpenBoatUtilsPacket packet : packets) {
            packet.write(buf);
        }

        return buf;
    }
}
