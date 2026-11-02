package io.gitlab.icestom.icestom.intergrations;

import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.protocol.ISChannel;
import io.gitlab.icestom.protocol.packets.configuration.ClientboundConfiguration;
import io.gitlab.icestom.protocol.packets.configuration.ServerboundConfiguration;
import io.gitlab.icestom.protocol.packets.configuration.clientbound.ServerHello;
import io.gitlab.icestom.protocol.packets.configuration.serverbound.ClientHello;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventHandler;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.network.packet.client.common.ClientPluginMessagePacket;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

public class IceStomModManager implements EventHandler<Event> {

    private static final Logger log = LoggerFactory.getLogger(IceStomModManager.class);
    private final EventNode<Event> eventNode = EventNode.all("icestom_mod_manager");

    public IceStomModManager() {
        eventNode().addListener(PlayerPluginMessageEvent.class, event ->
                this.onPluginMessage(event.getPlayer(), event.getIdentifier(), event.getMessage()));

        eventNode().addListener(
                AsyncPlayerConfigurationEvent.class,
                event -> {
                    Player player = event.getPlayer();

                    player.sendPacket(new PluginMessagePacket(
                            ISChannel.CONFIGURATION.getIdentifier(),
                            ClientboundConfiguration.newBuilder()
                                    .setHello(ServerHello.newBuilder()
                                            .setSpec(1)
                                            .build())
                                    .build()
                                    .toByteArray()));
                }
        );

        MinecraftServer.getPacketListenerManager().setConfigurationListener(
                ClientPluginMessagePacket.class,
                (packet, connection) -> {
                    onPluginMessage(connection, packet.channel(), packet.data());
                }
        );
    }

    private void onPluginMessage(Player player, String identifier, byte[] data) {
        Optional<ISChannel> optionalChannel = Arrays.stream(ISChannel.values())
                .filter(channel1 -> channel1.getIdentifier().equals(identifier))
                .findFirst();

        if (optionalChannel.isEmpty()) return;

        switch (optionalChannel.get()) {
            case CONFIGURATION -> {
                ServerboundConfiguration serverboundConfiguration;

                try {
                    serverboundConfiguration = ServerboundConfiguration.parseFrom(data);
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    log.warn("Failed to decode C2S Configuration packet", e);
                    return;
                }

                if (serverboundConfiguration.hasHello()) {
                    ClientHello clientHello = serverboundConfiguration.getHello();

                    String namespace = clientHello.getNamespace();
                    String version = clientHello.getVersion();
                    boolean unstable = clientHello.getUnstable();

                    log.info("Client Hello: NS:{} Version {} {}", namespace, version, unstable ? "UNSTABLE" : "STABLE");

                    for (ClientHello.Feature feature : clientHello.getFeaturesList()) {
                        log.info("Feature {}, {}", feature.getKey(), feature.getVersion());

                        ((IceStomPlayer) player).addModFeature(feature.getKey(), feature.getVersion());
                    }
                }
            }
        }
    }

    @Override
    public @NonNull EventNode<Event> eventNode() {
        return eventNode;
    }
}
