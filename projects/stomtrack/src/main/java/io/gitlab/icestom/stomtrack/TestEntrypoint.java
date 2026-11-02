package io.gitlab.icestom.stomtrack;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.gitlab.icestom.stomtrack.impl.MutableTrackFile;
import io.gitlab.icestom.stomtrack.serde.ComponentSerde;
import io.gitlab.icestom.stomtrack.serde.OBUSettingsPacketSerde;
import io.gitlab.icestom.stomtrack.type.Location;
import io.gitlab.icestom.stomtrack.type.Vec2;
import io.gitlab.icestom.stomtrack.type.Vec3;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class TestEntrypoint {
    static void main(String[] args) throws IOException {
        TrackFile file = new MutableTrackFile("test", Component.text("test"), false, new Location(0, 0, 0, 0, 0));

        file.getCheckpoints().put(new TrackCheckpoint.CuboidCheckpoint(new Vec3(0, 0, 0), new Vec3(1, 1, 1)), 1);
        file.getCheckpoints().put(new TrackCheckpoint.LineCheckpoint(new Vec2(0, 0), new Vec2(1, 1), 0, 4), 2);
        file.getCheckpoints().put(new TrackCheckpoint.PlaneCheckpoint(new Vec3(0, 0, 0), new Vec3(1, 1, 1), new Vec3(0, 1, 0), 4), 2);


        file.getGrid().add(new Location(0, 1, 2, 3, 4));
        file.getGrid().add(new Location(4, 3, 2, 1, 0));

        file.getOpenBoatUtils().add(new OBUSettingsPacket.AirControl(true));
        file.getOpenBoatUtils().add(new OBUSettingsPacket.RemoveBlockSlipperiness(List.of("minecraft:air")));

        file.getTags().put("icestom.test", "hello");
        file.getTags().put("icestom.empty", "");

        file.getRegions().put(new TrackRegion.PolyRegion(List.of(new Vec2(0, 0), new Vec2(10, 0), new Vec2(10, 10)), 0, 10), Set.of("test"));
        file.getRegions().put(new TrackRegion.PolyRegion(List.of(new Vec2(0, 0), new Vec2(10, 0), new Vec2(10, 10)), 0, 10), Set.of("test", "xyz"));
        file.getRegions().put(new TrackRegion.PolyRegion(List.of(new Vec2(0, 0), new Vec2(0, 10), new Vec2(10, 10)), 0, 10), Set.of("test"));

        file.getTriggers().put(new TrackTrigger.LineTrigger(new Vec2(0, 0), new Vec2(1, 1), 0, 2f), Set.of("abc", "xyz"));

        file.getLocations().put("test", new Location(0, 0, 0, 90, 0));

        XmlMapper mapper = XmlMapper.builder()
                .addModule(ComponentSerde.module())
                .addModule(OBUSettingsPacketSerde.module())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();

        String ser = mapper.writeValueAsString(file);

        System.out.println(ser);

        String b = mapper.writeValueAsString(mapper.reader().readValue(ser, MutableTrackFile.class));

        System.out.println(b);
        System.out.println(b.equals(ser));

    }
}
