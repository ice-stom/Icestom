package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.instance.TrackInstance;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;

public record PlayerMovement(long tick, @Nullable TrackInstance.TickLocation before, TrackInstance.TickLocation current) {}
