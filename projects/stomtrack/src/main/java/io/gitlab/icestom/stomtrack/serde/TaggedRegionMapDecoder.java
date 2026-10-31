package io.gitlab.icestom.stomtrack.serde;

import io.gitlab.icestom.stomtrack.TrackRegion;

public class TaggedRegionMapDecoder {
    public static class Serializer extends AbstractTaggedMapSerializer<TrackRegion> {}

    public static class Deserializer extends AbstractTaggedMapDeserializer<TrackRegion> {
        public Deserializer() {
            super(TrackRegion.class);
        }
    }
}