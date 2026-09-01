package io.gitlab.icestom.icestom.event;

import java.util.List;

public interface Stateful<E extends Enum<E>> {
    E getState();

    List<StateChange<E>> getStageChanges();

    record StateChange<E>(String name, E before, E after, Runnable run) {}
}
