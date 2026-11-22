import io.gitlab.icestom.stomtrack.TrackFile;
import io.gitlab.icestom.stomtrack.TrackLoader;
import io.gitlab.icestom.stomtrack.type.Location;
import io.gitlab.icestom.tracklua.LuaFileSystem;
import io.gitlab.icestom.tracklua.TrackLuaHandle;
import io.gitlab.icestom.tracklua.WorldLuaHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class TestTrackLike implements TrackLuaHandle {

    private static final Logger log = LoggerFactory.getLogger(TestTrackLike.class);

    private final TrackFile trackFile;
    private final LuaFileSystem fs;

    public TestTrackLike(Path base, String track) throws IOException {
        try (InputStream is = Files.newInputStream(base.resolve(track + ".track.xml"))) {
            trackFile = TrackLoader.loadTrack(is);
        }

        fs = new LuaFileSystem();

        Path luaDir = base.resolve("lua");

        try (Stream<Path> paths = Files.list(luaDir)) {
            paths.forEach(path -> {
                Path relative = luaDir.relativize(path);
                log.info("loading {} into luafs", relative);

                try {
                    fs.addFile(fs.root().resolve(relative.toString()).toString(), Files.newInputStream(path));
                } catch (IOException e) {
                    log.error("Failed to load file into filesytem", e);
                }
            });
        }
    }

    @Override
    public List<UUID> getPlayers() {
        return List.of(UUID.fromString("80d61d41-2c4f-49dc-be29-0d1055a83a47"));
    }

    @Override
    public WorldLuaHandle getWorld() {
        return null;
    }

    @Override
    public void subscribeTrigger(String tag) {

    }

    @Override
    public void subscribeRegion(String tag) {

    }

    @Override
    public void unsubscribeTrigger(String tag) {

    }

    @Override
    public void unsubscribeRegion(String tag) {

    }

    @Override
    public void teleportPlayer(UUID player, Location location) {
        log.info("Teleporting {} to {}", player, location);
    }

    @Override
    public TrackFile getTrackFile() {
        return trackFile;
    }

    @Override
    public LuaFileSystem getFileSystem() {
        return fs;
    }
}
