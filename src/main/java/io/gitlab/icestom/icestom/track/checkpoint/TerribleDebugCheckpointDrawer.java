package io.gitlab.icestom.icestom.track.checkpoint;

import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;

public class TerribleDebugCheckpointDrawer {
    private static final int EDGE_STEPS = 24;

    private static final int VERT_STEPS = 8;

    private static final int COLUMN_COUNT = 5;

    private static final float DUST_SIZE = 1.2f;

    private static final int COLOR_R = 255;
    private static final int COLOR_G = 140;
    private static final int COLOR_B = 0;

    private TerribleDebugCheckpointDrawer() {}

    public static void drawPlaneCheckpoint(InstanceContainer instance, PlaneCheckpoint checkpoint) {
        Vec a = checkpoint.getA();
        Vec b = checkpoint.getB();
        Vec upNorm = checkpoint.getUp();

        double halfHeight = checkpoint.height * 0.5;
        Vec offset = upNorm.mul(halfHeight);

        Vec aBottom = a.sub(offset);
        Vec bBottom = b.sub(offset);
        Vec aTop = a.add(offset);
        Vec bTop = b.add(offset);

        // Top and bottom edges
        drawEdge(instance, aBottom, bBottom, EDGE_STEPS);
        drawEdge(instance, aTop, bTop, EDGE_STEPS);

        // Vertical columns
        for (int i = 0; i < COLUMN_COUNT; i++) {
            double t = (double) i / (COLUMN_COUNT - 1);
            Vec bottom = lerp(aBottom, bBottom, t);
            Vec top = lerp(aTop, bTop, t);
            drawEdge(instance, bottom, top, VERT_STEPS);
        }
    }

    private static void drawEdge(Instance instance, Vec from, Vec to, int steps) {
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec point = lerp(from, to, t);
            spawnDust(instance, point);
        }
    }

    private static void spawnDust(Instance instance, Vec pos) {
        ParticlePacket packet = new ParticlePacket(
                Particle.DUST.withColor(new Color(COLOR_R, COLOR_G, COLOR_B)).withScale(DUST_SIZE),
                pos.x(), pos.y(), pos.z(),
                0f, 0f, 0f,
                0f,
                1
        );
        instance.sendGroupedPacket(packet);
    }

    private static Vec lerp(Vec from, Vec to, double t) {
        return from.add(to.sub(from).mul(t));
    }
}