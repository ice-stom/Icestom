package io.gitlab.icestom.icestom.timetrial;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;

public class ManagedTimetrialInstance extends TimeTrialingInstance {

    public ManagedTimetrialInstance(TrackLibrary.Ticket loadTicket, Track track) {
        super(IceStom.getInstance().getTrackLibrary().newReference(loadTicket), track);
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        TimeTrialManager manager = IceStom.getInstance().getTimeTrialManager();

        if (getPlayers().isEmpty() && manager.isUnreferenced(getTrack().getId())) {
            manager.unprovisionInstance(this);
        }
    }
}