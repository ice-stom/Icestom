package io.gitlab.icestom.icestom.entity;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.ui.theme.Themes;
import io.gitlab.icestom.icestom.ui.translation.TranslationManager;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.util.Tristate;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IceStomPlayer extends Player {

    private static final TranslationManager translationManager = IceStom.getInstance().getTranslationManager();

    private static final LuckPerms luckPerms = IceStom.getInstance().getLuckPerms();
    private static final PlayerAdapter<Player> playerAdapter = luckPerms.getPlayerAdapter(Player.class);

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

    public Tristate getPermission(String permission) {
        return playerAdapter.getUser(this)
                .getCachedData()
                .getPermissionData()
                .checkPermission(permission);
    }

    public boolean hasPermission(String permission) {
        return getPermission(permission).asBoolean();
    }

    public void setOpenBoatUtilsVersion(Integer openBoatUtilsVersion) { this.openBoatUtilsVersion = openBoatUtilsVersion; }

    public @Nullable Integer getOpenBoatUtilsVersion() {
        return openBoatUtilsVersion;
    }
}
