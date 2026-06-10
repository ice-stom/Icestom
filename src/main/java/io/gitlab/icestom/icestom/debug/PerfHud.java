package io.gitlab.icestom.icestom.debug;

import io.gitlab.icestom.icestom.util.MovingAverage;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventHandler;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.utils.time.TimeUnit;
import org.jspecify.annotations.NonNull;

public class PerfHud implements EventHandler<Event> {
    private final EventNode<Event> eventNode = EventNode.all("perf_hud");

    private final BossBar bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);

    private final MovingAverage mspt_5t = new MovingAverage(5);
    private final MovingAverage mspt_20t = new MovingAverage(20);
    private final MovingAverage mspt_100t = new MovingAverage(100);

    public PerfHud() {
        eventNode.addListener(PlayerSpawnEvent.class, playerSpawnEvent -> {
            addViewer(playerSpawnEvent.getPlayer());
        });
        eventNode.addListener(ServerTickMonitorEvent.class, this::onTick);
    }

    private void updateBossBar() {

        float mem = 1 - ((float) Runtime.getRuntime().freeMemory() / Runtime.getRuntime().totalMemory());

        double mspt = mspt_5t.getAverage();
        bossBar.name(Component.text(String.format("MSPT: %.2f - %.2f%%", mspt_20t.getAverage(), mem * 100)));

        var tickLength = (double) TimeUnit.SERVER_TICK.getDuration().toMillis();
        double avgTps = mspt / tickLength;
        var mspt100t = mspt_100t.getAverage();
        var avgTps100t = mspt100t / tickLength;
        avgTps100t *= 8;

        double progress = avgTps / avgTps100t;
        progress = Math.min(1, Math.max(0, progress));
        bossBar.progress((float) progress);

        if (mspt < 25) {
            bossBar.color(BossBar.Color.GREEN);
        } else if (mspt < 50) {
            bossBar.color(BossBar.Color.YELLOW);
        } else {
            bossBar.color(BossBar.Color.RED);
        }
    }

    public void addViewer(Audience audience) {
        bossBar.addViewer(audience);
    }

    public void onTick(ServerTickMonitorEvent tick) {
        var tickMonitor = tick.getTickMonitor();
        var diff = tickMonitor.getTickTime();
        mspt_5t.add(diff);
        mspt_20t.add(diff);
        mspt_100t.add(diff);
        updateBossBar();
    }

    @Override
    public @NonNull EventNode<Event> eventNode() {
        return eventNode;
    }
}
