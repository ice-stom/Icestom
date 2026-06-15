package io.gitlab.icestom.icestom.openboatutils;

import java.io.IOException;
import java.util.List;

public class GroupedPacketPayload {
    public List<OBUSettingsPackets> packets;

    public GroupedPacketPayload(List<OBUSettingsPackets> packets) { this.packets = packets; }

    public PacketByteBuf write(PacketByteBuf buf) throws IOException {
        buf.writeInt(packets.size());

        for (OBUSettingsPackets packet : packets) {
            packet.write(buf);
        }

        return buf;
    }
}
