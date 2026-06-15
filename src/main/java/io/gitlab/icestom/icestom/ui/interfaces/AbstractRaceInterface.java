
package io.gitlab.icestom.icestom.ui.interfaces;

import io.gitlab.icestom.icestom.race.RaceInstance;
import io.gitlab.icestom.icestom.race.event.RaceEvent;
import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.event.TimeTrialEvent;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventHandler;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerEvent;
import org.jspecify.annotations.NonNull;

import java.lang.ref.WeakReference;

public abstract class AbstractRaceInterface extends Interface<RaceInstance, AbstractRaceInterface> implements EventHandler<Event> {

    private final WeakReference<RaceInstance> holder;
    private final EventNode<Event> eventNode;

    public AbstractRaceInterface(RaceInstance holder) {
        WeakReference<RaceInstance> weakReference = new WeakReference<>(holder);

        this.holder = weakReference;

        eventNode = EventNode.type("race_interface", EventFilter.ALL, (event, _) -> {
           if (event instanceof RaceEvent raceEvent) {
               if (raceEvent.getInstance() != weakReference.get()) return false;
           }

            if (event instanceof PlayerEvent playerEvent) {
                if (playerEvent.getPlayer().getInstance() != holder) return false;
            }

            return true;
        });

        super();
    }

    @Override
    public RaceInstance getHolder() { return holder.get(); }

    @Override
    public @NonNull EventNode<Event> eventNode() {
        return eventNode;
    }
}
