package io.gitlab.icestom.icestom.ui;

public class TickCountdown {

    private int ticks = 0;
    private int duration = 0;

    private boolean running = false;

    public void start(int duration) {
        this.duration = duration;
        this.ticks = duration;
        running = true;
    }

    public boolean tick() {
        if (ticks <= 0) {
            return false;
        };

        if (--ticks == 0) {
            running = false;
            return true;
        }

        return false;
    }

    public boolean isRunning() {
        return running;
    }

    public int getRemainingTicks() {
        return ticks;
    }

    public int getDurationTicks() {
        return duration;
    }
}
