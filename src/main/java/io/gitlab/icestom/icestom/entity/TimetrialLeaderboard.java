
package io.gitlab.icestom.icestom.entity;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.config.IceStomConfig;
import io.gitlab.icestom.icestom.timetrial.lap.TimeTrialResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.util.TextFormatter;
import io.gitlab.icestom.icestom.util.UsernameCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.object.ObjectContents;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;

import java.util.List;
import java.util.UUID;

public class TimetrialLeaderboard extends Entity {

    private final Track track;

    public TimetrialLeaderboard(Track track) {
        super(EntityType.TEXT_DISPLAY);

        this.track = track;

        TextDisplayMeta meta = (TextDisplayMeta) getEntityMeta();

        meta.setText(Component.text("Loading..."));
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.VERTICAL);
        meta.setUseDefaultBackground(true);
        meta.setBrightness(15, 15);
        meta.setTranslation(new Vec(0, 1, 0));

        updateLeaderboard();
    }

    public void updateLeaderboard() {
        TextDisplayMeta meta = (TextDisplayMeta) getEntityMeta();

        List<TimeTrialResult> topAttempts = IceStom.getInstance().getTimetrialDatabase().getBestAttempts(track.getId(), IceStomConfig.getConfig().icestom.leaderboard_rows);

        TextComponent.Builder leaderboard = Component.text();

        leaderboard.append(track.getName());

        for (TimeTrialResult attempt : topAttempts) {
            final String name = UsernameCache.getUsernameWithTempFallback(attempt.player(), "Loading..");

            leaderboard.appendNewline();
            leaderboard.append(Component.object(ObjectContents.playerHead(attempt.player())));
            leaderboard.appendSpace();
            leaderboard.append(Component.text(name));
            leaderboard.appendSpace();
            leaderboard.append(TextFormatter.getTime(attempt.getTime()).color(NamedTextColor.YELLOW));
        }

        meta.setText(leaderboard.build());
    }
}
