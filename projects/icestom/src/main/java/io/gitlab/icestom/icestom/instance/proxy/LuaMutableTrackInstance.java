package io.gitlab.icestom.icestom.instance.proxy;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import net.hollowcube.luau.LuaError;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.Block;
import org.intellij.lang.annotations.Subst;

public class LuaMutableTrackInstance extends LuaInstanceContainer {

    private final boolean writeable;

    public LuaMutableTrackInstance(TrackInstance instance) {
        super(instance.getInstanceContainer());

        writeable = IceStom.getInstance().getTrackLibrary().isExclusive(instance.getTicket().getTrackInstanceId());
    }

    @Override
    public boolean getWriteable() {
        return writeable;
    }

    @Override
    public void setBlock(int x, int y, int z, @Subst("minecraft:air") String material) throws UnwritableWorldException {
        if (!writeable) throw new UnwritableWorldException();

        Key key = Key.key(material);

        Block block = Block.fromKey(key);

        if (block == null) throw new LuaError("Unknown block " + key);

        instance.setBlock(x, y, z, block);
    }
}
