package io.gitlab.icestom.icestom.instance;

import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;

public class TestInstance extends BoatInstance {
    public TestInstance() {
        super();

        setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.PACKED_ICE));
        setChunkSupplier(LightingChunk::new);
    }
}
