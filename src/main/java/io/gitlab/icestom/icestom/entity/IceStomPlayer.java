package io.gitlab.icestom.icestom.entity;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.EventParticipant;
import io.gitlab.icestom.icestom.ui.theme.Themes;
import io.gitlab.icestom.icestom.ui.translation.TranslationManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IceStomPlayer extends Player implements EventParticipant {

    private static final TranslationManager translationManager = IceStom.getInstance().getTranslationManager();

    private Integer openBoatUtilsVersion = null;

    public IceStomPlayer(@NotNull PlayerConnection playerConnection, GameProfile profile) {
        super(playerConnection, profile);
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        super.sendMessage(translationManager.render(message, getLocale(), Themes.DEFAULT_THEME));
    }

    @Override
    public void kick(Component message) {
        super.kick(translationManager.render(message, getLocale(), Themes.DEFAULT_THEME));
    }

    public boolean hasPermission(String permission) {
        return false;
    }

    public void setOpenBoatUtilsVersion(Integer openBoatUtilsVersion) { this.openBoatUtilsVersion = openBoatUtilsVersion; }

    public @Nullable Integer getOpenBoatUtilsVersion() {
        return openBoatUtilsVersion;
    }

    @Override
    public Player getPlayer() {
        return this;
    }

    @Override
    public List<Player> getParticipants() {
        return List.of(this);
    }
}
