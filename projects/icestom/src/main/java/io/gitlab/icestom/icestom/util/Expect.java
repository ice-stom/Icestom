package io.gitlab.icestom.icestom.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Expect {
    public static <T, E extends Throwable> @NotNull T expect(@Nullable T v, E e) throws E {
        if (v == null) throw e;

        return v;
    }
}
