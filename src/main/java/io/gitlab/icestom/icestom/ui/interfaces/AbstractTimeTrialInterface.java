package io.gitlab.icestom.icestom.ui.interfaces;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialEvent;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventHandler;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerEvent;
import org.jspecify.annotations.NonNull;

import java.lang.ref.WeakReference;
import java.util.Set;

public abstract class AbstractTimeTrialInterface extends Interface<TimeTrialingInstance, AbstractTimeTrialInterface> implements EventHandler<Event> {

    private final WeakReference<TimeTrialingInstance> holder;
    private final EventNode<Event> eventNode;

    public AbstractTimeTrialInterface(TimeTrialingInstance holder) {
        WeakReference<TimeTrialingInstance> weakReference = new WeakReference<>(holder);

        this.holder = weakReference;

        eventNode = EventNode.type("time_trialing_interface", EventFilter.ALL, (event, _) -> {
           if (event instanceof TimeTrialEvent timeTrialEvent) {
               if (timeTrialEvent.getInstance() != weakReference.get()) return false;
           }

            if (event instanceof PlayerEvent playerEvent) {
                if (playerEvent.getPlayer().getInstance() != holder) return false;
            }

            return true;
        });

        super();
    }

    @Override
    public TimeTrialingInstance getHolder() { return holder.get(); }

    @Override
    public @NonNull EventNode<Event> eventNode() {
        return eventNode;
    }
}
