package io.gitlab.icestom.icestom.track;

import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;

public record TickMovement(@Nullable Vec before, Vec current) {}
