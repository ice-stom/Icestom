package io.gitlab.icestom.icestom.instance;

import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.gitlab.icestom.icestom.config.IceStomConfig;
import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.event.TrackColliderEvent;
import io.gitlab.icestom.icestom.track.event.TrackRegionEnterEvent;
import io.gitlab.icestom.icestom.track.event.TrackTriggerPassEvent;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static io.gitlab.icestom.icestom.intergrations.OpenBoatUtilsManager.writePacket;

public abstract class BoatedTrackInstance extends TrackInstance {

    private static final Logger log = LoggerFactory.getLogger(BoatedTrackInstance.class);

    private static final String TELEPORT_PREFIX = "icestom.teleport_";
    private static final String RESET_TAG = "icestom.reset";

    protected static final PluginMessagePacket no_collision_packet;
    protected static final PluginMessagePacket vanilla_collision_packet;

    static {
        try {
            no_collision_packet = writePacket(new OBUSettingsPacket.CollisionMode(OBUSettingsPacket.CollisionModes.NO_BOATS_AND_PLAYERS));
            vanilla_collision_packet = writePacket(new OBUSettingsPacket.CollisionMode(OBUSettingsPacket.CollisionModes.VANILLA));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected final ItemStack RESET_ITEM = ItemStack.of(Objects.requireNonNull(Material.fromKey(IceStomConfig.getConfig().icestom.reset_item)))
            .withCustomName(Component.text("Reset", NamedTextColor.RED).style(Style.style().decoration(TextDecoration.ITALIC, false)));

    protected final Map<UUID, TickLocation> lastPosition = new HashMap<>();
    private final Map<UUID, Long> teleportedAtTick = new HashMap<>();

    protected BoatedTrackInstance(TrackLibrary.Ticket ticket, Track track) {
        super(ticket, track);

        track.getRegions().values().stream()
                .flatMap(Set::stream)
                .filter(tag -> tag.startsWith(TELEPORT_PREFIX))
                .distinct()
                .forEach(tag -> {
                    if (!track.getLocations().containsKey(getTeleportTarget(tag))) {
                        log.warn("Teleport region '{}' on track {} has no location named '{}'", tag, track.getId(), getTeleportTarget(tag));
                    }

                    subscribeRegionId(tag);
                });

        eventNode().addListener(TrackRegionEnterEvent.class, event -> {
            if (!event.getTag().startsWith(TELEPORT_PREFIX)) return;

            Pos target = track.getLocations().get(getTeleportTarget(event.getTag()));
            if (target == null) return;

            final Player player = event.getPlayer();
            if (player.getVehicle() instanceof Boat) createBoat(player, target);
            else player.teleport(target);
        });

        subscribeRegionId(RESET_TAG);
        subscribeTriggerId(RESET_TAG);

        Consumer<TrackColliderEvent> handleReset = event -> {
            if (event.getTag().equals(RESET_TAG)) resetToLastCheckpoint(event.getPlayer());
        };

        eventNode().addListener(TrackRegionEnterEvent.class, handleReset::accept);
        eventNode().addListener(TrackTriggerPassEvent.class, handleReset::accept);
    }

    public boolean resetToLastCheckpoint(Player player) {
        return false;
    }

    protected Pos getCheckpointReset(int checkpoint) {
        return track.getLocations().getOrDefault("icestom.reset_" + checkpoint, track.getSpawnLocation());
    }

    private static String getTeleportTarget(String tag) {
        return tag.substring(TELEPORT_PREFIX.length());
    }

    @Override
    public void resetPlayer(Player player) {
        Pos spawn = track.getSpawnLocation();
        float pitch = player.getPosition().pitch();

        removeBoat(player);

        super.resetPlayer(player);

        createBoat(player, spawn);
        player.teleport(player.getPosition().withPitch(pitch));
    }

    @Override
    public CompletableFuture<Boat> createBoat(Player player, Pos pos) {
        teleportedAtTick.put(player.getUuid(), getPlayerTick(player.getUuid()));
        lastPosition.remove(player.getUuid());

        return super.createBoat(player, pos);
    }

    protected boolean isBeforeTeleport(TickLocation movement) {
        return movement.tick() <= teleportedAtTick.getOrDefault(movement.player(), -1L);
    }
}
