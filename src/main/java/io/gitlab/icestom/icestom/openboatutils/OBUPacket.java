package io.gitlab.icestom.icestom.openboatutils;

import net.minestom.server.network.packet.server.common.PluginMessagePacket;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public interface OBUPacket extends PacketByteBufWriter {

    short getPacketId();
    short getVersion();

    default PacketByteBuf write(PacketByteBuf buf) throws IOException {
        return buf.writeShort(getPacketId());
    }

    default PluginMessagePacket toPacket(String channel) throws IOException {
        PacketByteBuf buf = new PacketByteBuf();

        return new PluginMessagePacket(channel, write(buf).toBytes());
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
}
