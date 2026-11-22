import io.gitlab.icestom.tracklua.TrackLua;
import io.gitlab.icestom.tracklua.userdata.LuaEventProxy;
import net.hollowcube.luau.compiler.LuauCompileException;

void main() throws IOException, LuauCompileException {
    Path srcPath = Path.of("temp");

    TestTrackLike trackLike = new TestTrackLike(srcPath, "67circuit");

    TrackLua trackLua = new TrackLua(trackLike);

    UUID player = UUID.fromString("80d61d41-2c4f-49dc-be29-0d1055a83a47");

    trackLua.queueEvent(new LuaEventProxy.CheckpointEventProxy(
        player, 0
    ));

    while (!trackLua.bump()) {}
}