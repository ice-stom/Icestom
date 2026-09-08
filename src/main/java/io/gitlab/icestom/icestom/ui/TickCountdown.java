package io.gitlab.icestom.icestom.ui;

public class TickCountdown {

    private int durationTicks;
    private int remainingTicks;

    public void start(int ticks) {
        if (ticks <= 0) {
            durationTicks = 0;
            remainingTicks = 0;
            return;
        }

        durationTicks = ticks;
        remainingTicks = ticks;
    }

    public boolean tick() {
        if (remainingTicks <= 0) return false;

        remainingTicks--;

        return remainingTicks == 0;
    }

    public void cancel() {
        remainingTicks = 0;
    }

    public boolean isRunning() {
        return remainingTicks > 0;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public int getDurationTicks() {
        return durationTicks;
    }
}
