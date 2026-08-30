package io.gitlab.icestom.icestom.timetrial;

public record Split(
        long ms,
        int checkpoint_no
) {
    public Split(long ms, long tick_delta, int checkpoint_no) {
        this(
                ms + tick_delta,
                checkpoint_no
        );
    }

    public Split offset(long offset) {
        return new Split(
                ms - offset,
                checkpoint_no
        );
    }
}2