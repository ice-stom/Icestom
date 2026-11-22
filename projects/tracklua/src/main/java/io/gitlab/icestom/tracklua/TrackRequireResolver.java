package io.gitlab.icestom.tracklua;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.luau.require.RequireResolver;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

public final class TrackRequireResolver implements RequireResolver {
    private static final List<String> EXTENSIONS = List.of(".luau", ".lua");
    private static final long MAX_MODULE_BYTES = 1_000_000;

    private final LuaFileSystem fs;
    private final LuauCompiler compiler;
    private final String cacheKey;

    private Path cursor;

    public TrackRequireResolver(LuaFileSystem fs, LuauCompiler compiler, String cacheKey) {
        this.fs = fs;
        this.compiler = compiler;
        this.cacheKey = cacheKey;
        this.cursor = fs.root();
    }

    private boolean insideRoot(Path p) {
        return p != null && p.startsWith(fs.root());
    }

    @Override
    public @NotNull Result reset(@NotNull LuaState state, @NotNull String requirerChunkName) {
        if (!requirerChunkName.startsWith("@")) return Result.NOT_FOUND;

        try {
            Path p = fs.root().resolve(requirerChunkName.substring(1)).normalize();
            if (!insideRoot(p)) return Result.NOT_FOUND;

            String name = p.getFileName() == null ? "" : p.getFileName().toString();

            for (String ext : EXTENSIONS) {
                if (name.equals("init" + ext)) {
                    cursor = p.getParent();
                    return Result.PRESENT;
                }
            }

            for (String ext : EXTENSIONS) {
                if (name.endsWith(ext)) {
                    cursor = p.resolveSibling(name.substring(0, name.length() - ext.length()));
                    return Result.PRESENT;
                }
            }

            cursor = p;
            return Result.PRESENT;
        } catch (InvalidPathException e) {
            return Result.NOT_FOUND;
        }
    }

    @Override
    public @NotNull Result toParent(@NotNull LuaState state) {
        Path parent = cursor.getParent();
        if (!insideRoot(parent)) return Result.NOT_FOUND;
        cursor = parent;
        return Result.PRESENT;
    }

    @Override
    public @NotNull Result toChild(@NotNull LuaState state, @NotNull String name) {
        if (name.isEmpty() || name.contains("/") || name.contains("\\")) return Result.NOT_FOUND;
        try {
            cursor = cursor.resolve(name);
            return Result.PRESENT;
        } catch (InvalidPathException e) {
            return Result.NOT_FOUND;
        }
    }

    @Override
    public @NotNull Result jumpToAlias(@NotNull LuaState state, @NotNull String aliasPath) {
        return Result.NOT_FOUND;
    }

    @Override
    public @NotNull Result getConfigStatus(@NotNull LuaState state) {
        return Result.NOT_FOUND;
    }

    @Override
    public String resolveAlias(@NotNull LuaState state, @NotNull String alias) {
        return null;
    }

    private Path findFile() {
        if (cursor.getFileName() == null) return null;

        Path found = null;
        for (String ext : EXTENSIONS) {
            Path f = cursor.resolveSibling(cursor.getFileName() + ext);
            if (Files.isRegularFile(f)) {
                if (found != null) return null;
                found = f;
            }
            Path init = cursor.resolve("init" + ext);
            if (Files.isRegularFile(init)) {
                if (found != null) return null;
                found = init;
            }
        }
        return found;
    }

    @Override
    public Module getModule(@NotNull LuaState state) {
        try {
            Path file = findFile();
            if (file == null) return null;

            String path = file.toString();

            return new Module(cacheKey + ":" + path, "@" + path, path);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    @Override
    public int load(@NotNull LuaState state, @NotNull String path, @NotNull String chunkName, @NotNull String loadName) {
        String file = loadName.startsWith("@") ? loadName.substring(1) : loadName;
        try {
            Path p = fs.root().resolve(file).normalize();
            if (!insideRoot(p)) throw state.error("module outside track root: " + file);
            if (Files.size(p) > MAX_MODULE_BYTES) throw state.error("module too large: " + file);

            byte[] source = Files.readAllBytes(p);
            state.load(chunkName.startsWith("@") ? chunkName : "@" + file, compiler.compile(source));
            state.call(0, 1);
            return 1;
        } catch (LuauCompileException e) {
            throw state.error("failed to compile " + file + ": " + e.getMessage());
        } catch (IOException e) {
            throw state.error("failed to read " + file + ": " + e);
        } catch (InvalidPathException e) {
            throw state.error("invalid module path: " + file);
        }
    }
}