package io.gitlab.icestom.stomtrack;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.gitlab.icestom.stomtrack.impl.MutableEnvironmentFile;
import io.gitlab.icestom.stomtrack.impl.MutableTrackFile;
import io.gitlab.icestom.stomtrack.serde.ComponentSerde;
import io.gitlab.icestom.stomtrack.serde.OBUSettingsPacketSerde;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface TrackLoader {

    XmlMapper MAPPER = XmlMapper.builder()
            .addModule(ComponentSerde.module())
            .addModule(OBUSettingsPacketSerde.module())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    static MutableTrackFile loadTrack(InputStream is) throws IOException {
        return MAPPER.readValue(is, MutableTrackFile.class);
    }

    static void saveTrack(OutputStream os, MutableTrackFile trackFile) throws IOException {
        MAPPER.writeValue(os, trackFile);
    }

    static MutableEnvironmentFile loadEnvironmentFile(InputStream is) throws IOException {
        return MAPPER.readValue(is, MutableEnvironmentFile.class);
    }

    static void saveEnvironmentFile(OutputStream os, MutableEnvironmentFile trackFile) throws IOException {
        MAPPER.writeValue(os, trackFile);
    }
}
