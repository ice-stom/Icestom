package io.gitlab.icestom.icestom.ui.interfaces;

import io.gitlab.icestom.icestom.IceStom;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventHandler;
import net.minestom.server.event.EventNode;
import org.jspecify.annotations.NonNull;

import java.lang.ref.WeakReference;

public abstract class AbstractGeneralInterface extends Interface<IceStom, AbstractGeneralInterface> implements EventHandler<Event> {

    private final WeakReference<IceStom> holder;
    private final EventNode<Event> eventNode;

    public AbstractGeneralInterface(IceStom holder) {
        this.holder = new WeakReference<>(holder);

        eventNode = EventNode.all("vanilla_general");
    }

    @Override
    public IceStom getHolder() { return holder.get(); }

    @Override
    public @NonNull EventNode<Event> eventNode() {
        return eventNode;
    }
}
