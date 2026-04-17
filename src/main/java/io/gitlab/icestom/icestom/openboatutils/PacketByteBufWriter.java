package io.gitlab.icestom.icestom.openboatutils;

import java.io.IOException;

public interface PacketByteBufWriter {
    PacketByteBuf write(PacketByteBuf buf) throws IOException;
}
