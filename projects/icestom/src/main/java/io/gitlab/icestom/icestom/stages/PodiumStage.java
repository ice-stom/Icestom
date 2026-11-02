package io.gitlab.icestom.icestom.stages;

import io.gitlab.icestom.icestom.entity.Snowball;
import io.gitlab.icestom.icestom.event.Stateful;
import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.event.stage.InvalidStageArgumentsException;
import io.gitlab.icestom.icestom.event.event.Result;
import io.gitlab.icestom.icestom.event.lua.ParticipantStore;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.TrackEventStage;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.timer.TaskSchedule;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class PodiumStage extends TrackInstance implements TrackEventStage, Stateful<PodiumStage.PodiumState> {

    private static final Component[] positions = new Component[] {
            Component.text("1st", TextColor.color(0xfcab0a)),
            Component.text("2nd", TextColor.color(0xadaeb2)),
            Component.text("3rd", TextColor.color(0x916b32))
    };

    private final CompletableFuture<List<Result<EventParticipant>>> future = new CompletableFuture<>();

    private final ParticipantStore participantStore = new ParticipantStore();

    private final String stageName;

    private PodiumState state = PodiumState.INIT;

    public PodiumStage(String stageName, TrackLibrary.Ticket ticket, Track track) {
        super(ticket, track);
        this.stageName = stageName;

        eventNode().addListener(PlayerUseItemEvent.class, event -> {
            final Player player = event.getPlayer();
            ItemStack itemStack = event.getItemStack();

            if (itemStack.material() == Material.SNOWBALL) {
                event.setCancelled(true);

                Snowball snowball = new Snowball(player, player.getPosition().direction().mul(40), pos -> {
                    sendGroupedPacket(new ParticlePacket(
                            Particle.ITEM_SNOWBALL,
                            true,
                            true,
                            pos,
                            Vec.ONE.mul(0.5),
                            1f,
                            10
                    ));
                });

                snowball.setInstance(this, player.getPosition().add(0, player.getEyeHeight(), 0));
            }
        });
    }

    @Override
    public String getStageName() {
        return stageName;
    }

    @Override
    public CompletableFuture<List<Result<EventParticipant>>> begin(List<Result<EventParticipant>> results) {
        MinecraftServer.getInstanceManager()
                .registerSharedInstance(this);

        for (Result<EventParticipant> result : results) {
            participantStore.addParticipant(result.getParticipant());
        }

        teleportAllParticipants(results);

        start();

        return future;
    }

    @Override
    public void cleanup() {
        MinecraftServer.getInstanceManager()
                .unregisterInstance(this);

        getTicket().burn();
    }

    @Override
    protected void handleMovements(List<TickLocation> movements) {}

    @Override
    protected boolean shouldTrackPlayer(Player player) {
        return false;
    }

    @Override
    public Pos spawnLocation(Player player) {
        EventParticipant participant = participantStore.getParticipantFromPlayer(player.getUuid());
        int position = participantStore.getIndexOfParticipant(participant);

        Pos podiumLocation = track.getLocations().get("icestom.podium_" + position);

        if (podiumLocation != null) {
            return podiumLocation;
        } else {
            return track.getLocations().getOrDefault("icestom.podium_all", track.getSpawnLocation());
        }
    }

    @Override
    public PodiumState getState() {
        return state;
    }

    @Override
    public List<StateChange<PodiumState>> getStageChanges() {
        return List.of(
                new StateChange<>("End", PodiumState.PODIUM, PodiumState.END, this::end)
        );
    }

    private void start() {
        if (state != PodiumState.INIT) return;
        state = PodiumState.PODIUM;

        for (Player player : getPlayers()) {
            player.getInventory().addItemStack(ItemStack.of(Material.SNOWBALL));
        }

        List<EventParticipant> podium = new ArrayList<>(participantStore.getParticipants().stream().limit(3).toList());
        Collections.reverse(podium);

        Iterator<EventParticipant> participantIterator = podium.iterator();

        AtomicInteger pos = new AtomicInteger(podium.size() - 1);
        MinecraftServer.getSchedulerManager().submitTask(() -> {
            if (!participantIterator.hasNext()) {
                state = PodiumState.CELEBRATION;
                return TaskSchedule.stop();
            };

            EventParticipant participant = participantIterator.next();

            TextComponent.Builder text = Component.text();

            text.append(positions[pos.getAndDecrement()]);
            text.appendSpace();
            text.append(participant.getName());

            for (UUID player : participant.getPlayers()) {
                Player player1 = getPlayerByUuid(player);

                if (player1 != null) sendGroupedPacket(new ParticlePacket(
                        Particle.FIREWORK,
                        true,
                        true,
                        player1.getPosition(),
                        Vec.ONE.mul(2),
                        0.1f,
                        100
                ));
            }

            showTitle(Title.title(
                    text.build(),
                    Component.empty(),
                    0,
                    50,
                    10
            ));

            return TaskSchedule.duration(Duration.ofMillis(3000));
        });
    }

    private void end() {
        if (state != PodiumState.PODIUM) return;
        state = PodiumState.END;

        List<Result<EventParticipant>> results = new ArrayList<>();

        for (EventParticipant participant : participantStore.getParticipants()) {
            results.add(new Result<>(participant));
        }

        future.complete(results);
    }

    public static CompletableFuture<PodiumStage> create(Map<String, Object> options) {
        if (!(options.get("name") instanceof String name)) {
            return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'name' isn't a string"));
        }

        if (!(options.get("track") instanceof String track_id)) {
            return CompletableFuture.failedFuture(new InvalidStageArgumentsException("'track' isn't a string"));
        }

        return TrackEventStage.withTrackTicket(track_id, (ticket, track) -> new PodiumStage(name, ticket, track));
    }

    public enum PodiumState {
        INIT,
        PODIUM,
        CELEBRATION,
        END
    }
}
