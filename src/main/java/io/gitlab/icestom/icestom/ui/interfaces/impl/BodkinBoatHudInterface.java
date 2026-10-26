package io.gitlab.icestom.icestom.ui.interfaces.impl;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialStartEvent;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialTimedLapEndedEvent;
import io.gitlab.icestom.icestom.timetrial.lap.TimeTrialResult;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.ui.interfaces.AbstractGeneralInterface;
import io.gitlab.icestom.icestom.ui.interfaces.AbstractTimeTrialInterface;
import io.gitlab.icestom.icestom.ui.interfaces.Interface;
import io.gitlab.icestom.icestom.ui.interfaces.InterfaceProvider;
import io.gitlab.icestom.icestom.util.UsernameCache;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.common.ClientPluginMessagePacket;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BodkinBoatHudInterface implements InterfaceProvider {

    public static final String CHANNEL = "timingsystemscoreboardmod:protocol";
    private static final Logger log = LoggerFactory.getLogger(BodkinBoatHudInterface.class);

    private final Set<Player> enabledTimetrial = new HashSet<>();

    @Override
    public <H, I extends Interface<H, I>> I getInterface(H holder) {
        return (I) switch (holder) {
            case IceStom instance -> new BodkinBoatHudGeneralInterface(instance);
            case TimeTrialingInstance instance -> new BodkinBoatHudTimetrialInterface(instance);
            default -> throw new RuntimeException(
                    "Unsupported Interface type: " +
                            holder.getClass().getSimpleName()
            );
        };
    }

    private static byte[] encodeString(DataType type, String javaString) {
        int len = switch (type) {
            case STRING_16 -> 16;
            case STRING_32 -> 32;
            case STRING_64 -> 64;
            default -> throw new IllegalArgumentException(
                    "Attempt to encode " + type + " as a string."
            );
        };

        byte[] str = javaString.getBytes(StandardCharsets.UTF_8);

        if (str.length > len) {
            throw new IllegalArgumentException(
                    "Can't fit " + str.length + " bytes into " + len + " bytes."
            );
        }

        byte[] data = new byte[len];
        Arrays.fill(data, (byte) ' ');

        System.arraycopy(str, 0, data, 0, str.length);

        return data;
    }

    public class BodkinBoatHudGeneralInterface extends AbstractGeneralInterface {

        public BodkinBoatHudGeneralInterface(IceStom holder) {
            super(holder);

            MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketEvent.class, event -> {

                final Player player = event.getPlayer();

                if (!(event.getPacket() instanceof ClientPluginMessagePacket(String channel, byte[] data))) return;
                if (!channel.equals(CHANNEL)) return;

                DataInputStream is = new DataInputStream(new ByteArrayInputStream(data));

                try {
                    byte type = is.readByte();

                    log.info("Bodkin Plugin {} Message: {}", type, Arrays.toString(data));

                    switch (type) {
                        case 0 -> {
                            int version = is.readInt();

                            boolean enabledForRaces = is.readBoolean();
                            boolean disableVanillaForRaces = is.readBoolean();
                            boolean enablesForTimeTrials = is.readBoolean();
                            boolean disableVanillaForTimeTrials = is.readBoolean();

                            if (enablesForTimeTrials) enabledTimetrial.add(player);

                            log.info("Feature Packet: Version: {} Flags: {} {} {} {}",
                                    version,
                                    enabledForRaces,
                                    disableVanillaForRaces,
                                    enablesForTimeTrials,
                                    disableVanillaForTimeTrials);
                        }
                        case 1 -> {
                            long most = is.readLong();
                            long least = is.readLong();
                            UUID uuid = new UUID(most, least);

                            String name = UsernameCache.getUsernameCached(uuid);

                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            DataOutputStream w = new DataOutputStream(out);
                            w.writeByte(2);
                            w.writeLong(most);
                            w.writeLong(least);
                            w.write(encodeString(DataType.STRING_16, name));
                            w.writeInt(0xFFFFFFFF);
                            w.writeInt(0xFFFFFFFF);
                            player.sendPacket(new PluginMessagePacket(CHANNEL, out.toByteArray()));
                        }
                    }



                } catch (IOException e) {
                    log.warn("Failed to decode packet", e);
                }
            });
        }

        @Override
        public boolean supportsPlayer(Player player) {
            return ((IceStomPlayer) player).hasSeenPluginMessageChannel(CHANNEL);
        }
    }

    public  class BodkinBoatHudTimetrialInterface extends AbstractTimeTrialInterface {

        private static final int ROWS = 5;

        private static final PlainTextComponentSerializer componentSerializer = PlainTextComponentSerializer.plainText();

        public enum Column {
            POSITION(ColumnType.POSITION),
            CHECKPOINTS(ColumnType.CHECKPOINTS),
            TIME(ColumnType.TT_TIME),
            PLAYER(ColumnType.PLAYER_UUID);

            private final ColumnType type;

            Column(ColumnType type) {
                this.type = type;
            }

            public ColumnType getType() {
                return type;
            }
        }

        public BodkinBoatHudTimetrialInterface(TimeTrialingInstance holder) {
            super(holder);

            eventNode().addListener(TimeTrialStartEvent.class, event -> {
                log.info("tt start");
                final Player player = event.getPlayer();

                ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
                DataOutputStream writer = new DataOutputStream(byteArray);

                try {
                    writer.writeByte(0);
                    writer.writeInt(Column.values().length);

                    for (Column col : Column.values()) {
                        writer.writeInt(col.getType().ordinal());
                    }

                    writer.writeInt(OneOffType.MAIN_TITLE.ordinal());
                    writer.writeInt(OneOffType.SUB_TITLE.ordinal());
                    writer.writeInt(OneOffType.TOTAL_CHECKPOINTS.ordinal());
                    writer.writeInt(OneOffType.ATTEMPTS.ordinal());

                } catch (IOException e) {
                    log.error("Failed to write packet", e);
                    return;
                }

                player.sendPacket(new PluginMessagePacket(CHANNEL, byteArray.toByteArray()));

                log.info("sent structure");
                updateLeaderboard(player);
            });

            eventNode().addListener(TimeTrialTimedLapEndedEvent.class, event -> updateLeaderboard(event.getPlayer()));
        }

        private void updateLeaderboard(Player player) {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            DataOutputStream writer = new DataOutputStream(byteArray);

            Track track = getHolder().getTrack();

            @NotNull List<TimeTrialResult> rows = IceStom.getInstance().getTimetrialDatabase().getBestAttempts(track.getId(), ROWS);

            try {
                writer.writeByte(1);
                writer.writeInt(rows.size());

                int i = 0;

                for (TimeTrialResult row : rows) {
                    UUID uuid = row.player();

                    for (Column col : Column.values()) {
                        switch (col) {
                            case POSITION -> writer.writeInt(++i);
                            case CHECKPOINTS -> writer.writeInt(row.splits().size());
                            case TIME -> writer.writeLong(row.getTime());
                            case PLAYER -> {
                                writer.writeLong(uuid.getMostSignificantBits());
                                writer.writeLong(uuid.getLeastSignificantBits());
                            }
                        }
                    }
                }

                writer.write(encodeString(DataType.STRING_64, componentSerializer.serialize(track.getName())));
                writer.write(encodeString(DataType.STRING_64, "By: " + track.getTags().getOrDefault("icestom.author", "unknown")));
                writer.writeInt(track.getHighestCheckpoint() + 1);
                writer.writeInt(-1);

            } catch (IOException e) {
                log.error("Failed to write packet", e);
                return;
            }

            player.sendPacket(new PluginMessagePacket(CHANNEL, byteArray.toByteArray()));
        }

        @Override
        public boolean supportsPlayer(Player player) {
            return enabledTimetrial.contains(player);
        }
    }

    public enum DataType {
        INT,
        FLOAT,
        LONG,
        UUID,
        BYTE,
        BOOLEAN,
        STRING_16,
        STRING_32,
        STRING_64
    }

    public enum ColumnType {
        PLAYER_UUID(DataType.UUID),
        POSITION(DataType.INT),
        FINISHED(DataType.BOOLEAN),
        IN_PIT(DataType.BOOLEAN),
        DNF(DataType.BOOLEAN),
        PITS(DataType.INT),
        CHECKPOINTS(DataType.INT),
        LAPS(DataType.INT),
        TIME_DIFF_FROM_FIRST_MS(DataType.LONG),
        TIME_DIFF_FROM_PREVIOUS_MS(DataType.LONG),
        FASTEST_LAP(DataType.BOOLEAN),
        DRS_STATE(DataType.BYTE),
        TT_TIME(DataType.LONG),
        IMPROVEMENT(DataType.LONG),
        GHOSTED(DataType.BOOLEAN),
        OFFLINE(DataType.BOOLEAN),
        ATTACK_MODE(DataType.BYTE),
        READY(DataType.BOOLEAN),
        PADDLE_TYPE(DataType.BYTE),
        PADDLE_DURABILITY(DataType.FLOAT),
        EVENT_POINTS(DataType.INT),
        EVENT_POINTS_FINALIST(DataType.BOOLEAN),
        EVENT_POINTS_WINNER_ORDER(DataType.INT);

        private final DataType dataType;

        ColumnType(DataType dataType) {
            this.dataType = dataType;
        }

        public DataType getDataType() {
            return dataType;
        }
    }

    public enum OneOffType {
        MAIN_TITLE(DataType.STRING_64),
        SUB_TITLE(DataType.STRING_64),
        SCOREBOARD_TITLE(DataType.STRING_64),
        TOTAL_LAPS(DataType.INT),
        TOTAL_PITS(DataType.INT),
        IS_QUALIFICATION(DataType.BOOLEAN),
        BRONZE_MEDAL_TIME(DataType.LONG),
        BRONZE_MEDAL_OBTAINED_PERCENTAGE(DataType.FLOAT),
        SILVER_MEDAL_TIME(DataType.LONG),
        SILVER_MEDAL_OBTAINED_PERCENTAGE(DataType.FLOAT),
        GOLD_MEDAL_TIME(DataType.LONG),
        GOLD_MEDAL_OBTAINED_PERCENTAGE(DataType.FLOAT),
        DIAMOND_MEDAL_TIME(DataType.LONG),
        DIAMOND_MEDAL_OBTAINED_PERCENTAGE(DataType.FLOAT),
        NETHERITE_MEDAL_TIME(DataType.LONG),
        NETHERITE_MEDAL_OBTAINED_PERCENTAGE(DataType.FLOAT),
        ATTEMPTS(DataType.INT),
        COMPLETIONS(DataType.INT),
        TIME_SPENT_MS(DataType.LONG),
        SESSION_ATTEMPTS(DataType.INT),
        SESSION_COMPLETIONS(DataType.INT),
        SESSION_TIME_SPENT_MS(DataType.LONG),
        TOTAL_CHECKPOINTS(DataType.INT),
        POSITION_PERCENTAGE(DataType.FLOAT),
        EVENT_NAME(DataType.STRING_64),
        USE_DRS(DataType.BOOLEAN),
        TYRES_ENABLED(DataType.BOOLEAN),
        WEATHER(DataType.BYTE),
        RAIN(DataType.FLOAT),
        EVENT_POINTS_ENABLED(DataType.BOOLEAN),
        EVENT_POINTS_FINALIST_THRESHOLD(DataType.INT);

        private final DataType dataType;

        OneOffType(DataType dataType) {
            this.dataType = dataType;
        }

        public DataType getDataType() {
            return dataType;
        }
    }
}
