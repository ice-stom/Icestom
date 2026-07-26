package io.gitlab.icestom.stomtrack.serde;

import io.gitlab.icestom.stomtrack.TrackCheckpoint;
import io.gitlab.icestom.stomtrack.TrackTrigger;

public class TaggedTriggerMapDecoder {
    public static class Serializer extends AbstractTaggedMapSerializer<TrackTrigger> {}

    public static class Deserializer extends AbstractTaggedMapDeserializer<TrackTrigger> {
        public Deserializer() {
            super(TrackTrigger.class);
        }
    }
}