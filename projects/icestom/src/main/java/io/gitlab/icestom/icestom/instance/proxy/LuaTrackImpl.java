package io.gitlab.icestom.icestom.instance.proxy;

import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.track.PositionConverter;
import io.gitlab.icestom.stomtrack.TrackFile;
import io.gitlab.icestom.stomtrack.type.Location;
import io.gitlab.icestom.tracklua.LuaFileSystem;
import io.gitlab.icestom.tracklua.TrackLuaHandle;
import io.gitlab.icestom.tracklua.WorldLuaHandle;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.UUID;

public class LuaTrackImpl implements TrackLuaHandle {

    private final TrackInstance trackInstance;

    public LuaTrackImpl(TrackInstance trackInstance) {
        this.trackInstance = trackInstance;
    }

    @Override
    public void teleportPlayer(UUID player, Location location) {
        Player instance = trackInstance.getPlayerByUuid(player);

        if (instance != null) instance.teleport(PositionConverter.fromLocation(location));
    }

    @Override
    public List<UUID> getPlayers() {
        return trackInstance.getPlayers().stream().map(Entity::getUuid).toList();
    }

    public WorldLuaHandle getWorld() {
        return new LuaMutableTrackInstance(trackInstance);
    }

    // TODO: replace these 4 with something that stops tracklua from messing with the upstream IS implementations

    @Override
    public void subscribeTrigger(String tag) {
        trackInstance.subscribeTriggerId(tag);
    }

    @Override
    public void subscribeRegion(String tag) {
        trackInstance.subscribeRegionId(tag);
    }

    @Override
    public void unsubscribeTrigger(String tag) {
        trackInstance.unsubscribeTriggerId(tag);
    }

    @Override
    public void unsubscribeRegion(String tag) {
        trackInstance.unsubscribeRegionId(tag);
    }

    @Override
    public TrackFile getTrackFile() {
        return trackInstance.getTrack().getSource();
    }

    @Override
    public LuaFileSystem getFileSystem() {
        return trackInstance.getTrack().getLuaFileSystem();
    }
}
