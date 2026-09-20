package io.gitlab.icestom.icestom.entity;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.database.preference.PreferenceKey;
import io.gitlab.icestom.icestom.database.preference.PreferenceRegistry;
import io.gitlab.icestom.icestom.event.EventParticipant;
import io.gitlab.icestom.icestom.ui.theme.Themes;
import io.gitlab.icestom.icestom.ui.translation.TranslationManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.BoatMeta;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IceStomPlayer extends Player implements EventParticipant {

    private static final TranslationManager translationManager = IceStom.getInstance().getTranslationManager();

    private final Map<PreferenceKey<?>, Object> preferences = new HashMap<>();
    private @Nullable Integer openBoatUtilsVersion = null;

    public IceStomPlayer(@NotNull PlayerConnection playerConnection, GameProfile profile) {
        super(playerConnection, profile);
    }

    public @NotNull Component translate(@NotNull Component component) {
        return translationManager.render(component, getLocale(), Themes.DEFAULT_THEME);
    }

    @SuppressWarnings("unchecked")
    public <T> @NotNull T preference(PreferenceKey<T> key) {
        return (T) preferences.computeIfAbsent(key, PreferenceKey::defaultValue);
    }

    public <T> void preference(PreferenceKey<T> key, @NotNull T value) {
        Objects.requireNonNull(value);
        preferences.put(key, value);
    };

    public boolean hasPermission(String permission) { return false; }

    public void setOpenBoatUtilsVersion(@Nullable Integer openBoatUtilsVersion) { this.openBoatUtilsVersion = openBoatUtilsVersion; }

    public @Nullable Integer getOpenBoatUtilsVersion() {
        return openBoatUtilsVersion;
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        super.sendMessage(translate(message));
    }

    @Override
    public void kick(@NonNull Component message) {
        super.kick(translate(message));
    }

    @Override
    public Player getCurrentPlayer() {
        return this;
    }

    @Override
    public List<Player> getParticipants() {
        return List.of(this);
    }
}
