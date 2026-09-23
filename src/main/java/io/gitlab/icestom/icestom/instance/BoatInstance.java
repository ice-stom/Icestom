package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.database.preference.PreferenceKey;
import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.GridBoatHolder;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.SharedInstance;
import net.minestom.server.network.packet.client.play.ClientTeleportConfirmPacket;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class BoatInstance extends SharedInstance {

    private static final Logger log = LoggerFactory.getLogger(BoatInstance.class);
    private final Map<Player, Boat> boats = new HashMap<>();

    private static final PreferenceKey<Key> BOAT_TYPE = IceStom.getInstance().getPreferenceRegistry().register(new PreferenceKey<>(
            Key.key(IceStom.NAMESPACE, "boat_type"),
            Key.class,
            Key.key("oak_boat")
    ));

    public BoatInstance(UUID uuid, InstanceContainer instanceContainer) {
        super(uuid, instanceContainer);
    }

    public @Nullable Boat removeBoat(Player player) {
        @Nullable Boat boat = boats.remove(player);

        if (boat != null) {
            Pos position = player.getPosition();

            Boat boat1 = removeBoat(player, boat);

            player.teleport(position.withY(Math.ceil(position.y())).withDirection(position.direction()));
            player.setVelocity(Vec.ZERO);

            return boat1;
        }

        return null;
    }

    public Boat removeBoat(Player player, @NotNull Boat boat) {
        if (boat.getVehicle() instanceof GridBoatHolder gridBoatHolder) {
            gridBoatHolder.remove();
        }

        for (Entity passenger : boat.getPassengers()) {
            if (passenger instanceof Player) {
                boat.removePassenger(passenger);
            }
        }

        boat.getPassengers().forEach(Entity::remove);
        boat.remove();

        boats.remove(player, boat);

        return boat;
    }

    @SuppressWarnings("UnstableApiUsage")
    public Boat createBoat(Player player, Pos pos) {
        removeBoat(player);

        Key key = ((IceStomPlayer) player).preference(BOAT_TYPE);

        Boat boat;
        try {
            boat = IceStom.getInstance().getBoatProvider().apply(key);
        } catch (Exception exception) {
            log.error("Failed to create boat with {} (creating {})", IceStom.getInstance().getBoatProvider(), key);
            throw exception;
        }

        boats.put(player, boat);

        if (player.getInstance() == this) {
            EventListener<@NotNull PlayerPacketEvent> listener = EventListener.builder(PlayerPacketEvent.class)
                    .filter(e -> e.getPlayer() == player && e.getPacket() instanceof ClientTeleportConfirmPacket)
                    .handler(_ -> {
                        if (boat.isRemoved()) return;

                        boat.setInstance(this, pos);
                        boat.addPassenger(player);
                        boat.addViewer(player);
                    })
                    .expireCount(1)
                    .build();

            eventNode().addListener(listener);
        } else {
            boat.setInstance(this, pos);
            boat.addPassenger(player);
        }

        player.teleport(pos.withPitch(player.getPosition().pitch()));

        return boat;
    }
}
