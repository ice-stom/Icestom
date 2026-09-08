package io.gitlab.icestom.icestom.ui.interfaces;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialEvent;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventHandler;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerEvent;
import org.jspecify.annotations.NonNull;

import java.lang.ref.WeakReference;

public abstract class AbstractGeneralInterface extends Interface<IceStom, AbstractGeneralInterface> implements EventHandler<Event> {

    private final WeakReference<IceStom> holder;
    private final EventNode<Event> eventNode;

    public AbstractGeneralInterface(IceStom holder) {
        WeakReference<IceStom> weakReference = new WeakReference<>(holder);

        this.holder = weakReference;

        eventNode = EventNode.type("general_interface", EventFilter.ALL);

        super();
    }

    @Override
    public IceStom getHolder() { return holder.get(); }

    @Override
    public @NonNull EventNode<Event> eventNode() {
        return eventNode;
    }
}
