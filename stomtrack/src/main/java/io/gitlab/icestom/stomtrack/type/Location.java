package io.gitlab.icestom.stomtrack.type;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@JsonSerialize(using = Location.Serializer.class)
@JsonDeserialize(using = Location.Deserializer.class)
public record Location(double x, double y, double z, float yaw, float pitch) {

    @Override
    public @NotNull String toString() {
        return x + "," + y + "," + z + "," + yaw + "," + pitch;
    }

    static Location parse(String text) {
        String[] parts = text.split(",");

        if (parts.length != 5) {
            throw new IllegalArgumentException("Expected \"x,y,z,yaw,pitch\", got: " + text);
        }

        return new Location(
                Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim()),
                Double.parseDouble(parts[2].trim()),
                Float.parseFloat(parts[3].trim()),
                Float.parseFloat(parts[4].trim())
        );
    }

    static final class Serializer extends StdSerializer<Location> {
        Serializer() { super(Location.class); }

        @Override
        public void serialize(Location value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }

    static final class Deserializer extends StdDeserializer<Location> {
        Deserializer() { super(Location.class); }

        @Override
        public Location deserialize(JsonParser p, DeserializationContext context) throws IOException {
            return Location.parse(p.getValueAsString());
        }
    }
}