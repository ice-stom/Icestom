package io.gitlab.icestom.tracklua;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

public class LuaFileSystem {
    private static final Logger log = LoggerFactory.getLogger(LuaFileSystem.class);

    private final FileSystem fs = Jimfs.newFileSystem(
            Configuration.unix()
                    .toBuilder()
                    .setWorkingDirectory("/")
                    .build()
    );

    public LuaFileSystem() {}

    public Path root() {
        return fs.getPath("/");
    }

    public void addFile(Path src, InputStream is) {
        Path dest = root().resolve(src.toString()).normalize();

        try {
            Files.createDirectories(dest.getParent());

            try (OutputStream outputStream = Files.newOutputStream(dest)) {
                is.transferTo(outputStream);
            }
        } catch (IOException e) {
            log.error("Failed to add {}", src, e);
        }
    }
}
