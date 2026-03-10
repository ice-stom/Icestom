package io.gitlab.icestom.icestom.ui.leaderboard;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record RaceLeaderboardRow(
        UUID player,
        long delta,

        @Nullable Long flapTime,

        int completedLaps,
        int completedPits,

        boolean pit,
        boolean dnf
) {}
