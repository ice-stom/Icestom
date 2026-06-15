package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.GridBoatHolder;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.play.ClientTeleportConfirmPacket;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class BoatInstance extends IceStomInstance {

    private final Map<Player, Boat> boats = new HashMap<>();

    public BoatInstance(Key key) {
        super(UUID.randomUUID(), DimensionType.OVERWORLD, key);
    }

    public @Nullable Boat removeBoat(Player player) {
        @Nullable Boat boat = boats.remove(player);

        if (boat != null) return removeBoat(player, boat);

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

        Boat boat = new Boat();
        boats.put(player, boat);

        if (player.getInstance() == this) {
            EventListener<@NotNull PlayerPacketEvent> listener = EventListener.builder(PlayerPacketEvent.class)
                    .filter(e -> e.getPlayer() == player && e.getPacket() instanceof ClientTeleportConfirmPacket)
                    .handler(_ -> {
                        boat.setInstance(this, pos);
                        boat.addPassenger(player);
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
