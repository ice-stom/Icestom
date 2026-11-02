package io.gitlab.icestom.stomtrack.serde;

import io.gitlab.icestom.stomtrack.TrackCheckpoint;

public class TaggedCheckpointMapDecoder {
    public static class Serializer extends AbstractTaggedMapSerializer<TrackCheckpoint> {}

    public static class Deserializer extends AbstractTaggedMapDeserializer<TrackCheckpoint> {
        public Deserializer() {
            super(TrackCheckpoint.class);
        }
    }
}