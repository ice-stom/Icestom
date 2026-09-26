package io.gitlab.icestom.icestom.track.library;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.stomtrack.EnvironmentFile;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.InstanceContainer;
import org.intellij.lang.annotations.Subst;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static io.gitlab.icestom.icestom.track.Track.getDimensionKey;

public class VirtualTrackInstance extends InstanceContainer {

    private final List<CompoundBinaryTag> displayEntities = new ArrayList<>();
    private final List<Consumer<CompoundBinaryTag>> listeners = new ArrayList<>();

    public VirtualTrackInstance(String environmentName, EnvironmentFile environmentFile) {
        @Subst("track/") String dim_key = "track/" + environmentName;

        super(
                UUID.randomUUID(),
                getDimensionKey(environmentFile),
                Key.key(IceStom.NAMESPACE, dim_key)
        );
    }

    public void addDisplayEntity(CompoundBinaryTag tag) {
        displayEntities.add(tag);
        listeners.forEach(c -> c.accept(tag));
    }

    public void onNewDisplayEntity(Consumer<CompoundBinaryTag> tagConsumer) {
        displayEntities.forEach(tagConsumer);
        listeners.add(tagConsumer);
    }
}
