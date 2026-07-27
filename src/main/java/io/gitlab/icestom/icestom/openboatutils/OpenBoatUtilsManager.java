package io.gitlab.icestom.icestom.openboatutils;

import io.github.openboatutils.protocol.OBUChannel;
import io.github.openboatutils.protocol.OBUPacket;
import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.github.openboatutils.protocol.impl.DataOutputStreamWriter;
import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.config.IceStomConfig;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventHandler;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.network.packet.client.common.ClientPluginMessagePacket;
import net.minestom.server.network.packet.client.common.ClientPongPacket;
import net.minestom.server.network.packet.server.common.PingPacket;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class OpenBoatUtilsManager implements EventHandler<Event> {

    // https://github.com/o7Moon/OpenBoatUtils/wiki/Version-IDs
    private static final short MIN_OPENBOATUTILS_VERSION = 19;

    private final PluginMessagePacket join_setting_packet;
    private static final Logger log = LoggerFactory.getLogger(OpenBoatUtilsManager.class);

    private final EventNode<Event> eventNode = EventNode.all("openboatutils");

    public OpenBoatUtilsManager() {
        try {
            OBUPacket packet = new OBUSettingsPacket.Compound(new OBUSettingsPacket.CompoundPayload(List.of(
                    new OBUSettingsPacket.InterpolationCompatibility(IceStomConfig.getConfig().openboatutils.interpolation_compatibility),
                    new OBUSettingsPacket.ResetOnWorldLoad(false),
                    new OBUSettingsPacket.ResendVersion()
            )));

            join_setting_packet = writePacket(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        eventNode.addListener(PlayerPluginMessageEvent.class, this::playerPluginMessageEvent);
    }

    public void playerPluginMessageEvent(PlayerPluginMessageEvent event) {
        final Player player = event.getPlayer();

        if (!event.getIdentifier().equals(OBUChannel.SETTINGS.getChannel())) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getMessage()));
            short packetId = in.readShort();

            if (packetId != 0) return;

            if (((IceStomPlayer) player).getOpenBoatUtilsVersion() != null) return;

            int version = in.readInt();

            if (version < MIN_OPENBOATUTILS_VERSION) {
                player.sendMessage(Component.translatable("message.openboatutils.outdated_version_global"));
                return;
            }

            boolean unstable = in.readBoolean();

            if (unstable) {
                if (IceStomConfig.getConfig().openboatutils.block_unstable) {
                    player.kick(Component.translatable("message.openboatutils.block_unstable"));
                    return;
                }

                player.sendMessage(Component.translatable("message.openboatutils.warning_unstable"));
            }

            ((IceStomPlayer) player).setOpenBoatUtilsVersion(version);
            player.sendPacket(join_setting_packet);

            int random = ThreadLocalRandom.current().nextInt(0xFF);

            player.sendPacket(new PingPacket(random));

            // if we get the ping packet before the plugin message we know something is up
            EventListener<@NotNull PlayerPacketEvent> listener = EventListener.builder(PlayerPacketEvent.class)
                    .filter(e -> e.getPlayer() == player && (
                            (e.getPacket() instanceof ClientPluginMessagePacket pluginMessagePacket && pluginMessagePacket.channel().equals(OBUChannel.SETTINGS.getChannel())) ||
                                    e.getPacket() instanceof ClientPongPacket
                    ))
                    .handler(event2 -> {
                        if (event2.getPacket() instanceof ClientPluginMessagePacket pluginMessagePacket) {
                            DataInputStream in2 = new DataInputStream(new ByteArrayInputStream(pluginMessagePacket.data()));

                            try {
                                if (in2.readShort() == 0) {
                                    return;
                                };
                            } catch (IOException ignored) {}
                        };
                        if (!(event2.getPacket() instanceof ClientPongPacket(int id))) {
                            log.error("Transaction failed with {} instead of a pong", event2.getPacket());
                            return;
                        };

                        if (id != random) {
                            player.kick(Component.translatable("message.openboatutils.failed_transaction"));
                            return;
                        };
                    })
                    .expireCount(1)
                    .build();

            player.eventNode().addListener(listener);
        } catch (IOException e) {
            log.error("Failed OpenBoatUtils Handshake: {}", String.valueOf(e));
        }
    }

    public @NonNull EventNode<Event> eventNode() {
        return eventNode;
    }

    public static PluginMessagePacket writePacket(OBUPacket packet) throws IOException {
        DataOutputStreamWriter writer = new DataOutputStreamWriter();

        try {
            packet.write(writer);
        } catch (IOException e) {
            log.error("Failed to write OpenBoatUtils plugin message {}", e.toString());
            throw new IOException(e);
        }

        return new PluginMessagePacket(packet.getChannel().getChannel(), writer.toBytes());
    }
}
