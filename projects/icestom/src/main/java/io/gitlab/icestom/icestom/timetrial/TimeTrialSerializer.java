package io.gitlab.icestom.icestom.timetrial;

import net.minestom.server.coordinate.Pos;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TimeTrialSerializer {

    private static final int BYTES_PER_SPLIT = 8;
    private static final int BYTES_PER_TICK = 32;

    private TimeTrialSerializer() {}

    public static String encodeSplits(List<Split> splits) {
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES + splits.size() * BYTES_PER_SPLIT);

        buf.putInt(splits.size());
        for (Split split : splits) {
            buf.putLong(split.ms());
        }

        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf.array());
    }

    public static List<Split> decodeSplits(String encoded) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(Base64.getUrlDecoder().decode(encoded));
            int count = buf.getInt();
            List<Split> splits = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                splits.add(new Split(buf.getLong(), i));
            }

            return splits;
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Malformed splits binary data", e);
        }
    }

    public static String encodeTicks(Map<Integer, Pos> ticks) {
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES + ticks.size() * BYTES_PER_TICK);

        buf.putInt(ticks.size());
        for (Map.Entry<Integer, Pos> entry : ticks.entrySet()) {
            Pos pos = entry.getValue();
            buf.putInt(entry.getKey());
            buf.putDouble(pos.x());
            buf.putDouble(pos.y());
            buf.putDouble(pos.z());
            buf.putFloat(pos.yaw());
        }

        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf.array());
    }

    public static Map<Integer, Pos> decodeTicks(String encoded) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(Base64.getUrlDecoder().decode(encoded));
            int count = buf.getInt();
            Map<Integer, Pos> ticks = new HashMap<>(count * 2);

            for (int i = 0; i < count; i++) {
                int tick = buf.getInt();
                double x = buf.getDouble();
                double y = buf.getDouble();
                double z = buf.getDouble();
                float yaw = buf.getFloat();
                ticks.put(tick, new Pos(x, y, z, yaw, 0f));
            }

            return ticks;
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Malformed ticks binary data", e);
        }
    }
}