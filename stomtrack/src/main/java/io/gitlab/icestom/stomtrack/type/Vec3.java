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

@JsonSerialize(using = Vec3.Serializer.class)
@JsonDeserialize(using = Vec3.Deserializer.class)
public record Vec3(double x, double y, double z) {

    @Override
    public @NotNull String toString() {
        return x + "," + y + "," + z;
    }

    static Vec3 parse(String text) {
        String[] parts = text.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Expected \"x,y,z\", got: " + text);
        }
        return new Vec3(
                Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim()),
                Double.parseDouble(parts[2].trim())
        );
    }

    static final class Serializer extends StdSerializer<Vec3> {
        Serializer() { super(Vec3.class); }

        @Override
        public void serialize(Vec3 value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }

    static final class Deserializer extends StdDeserializer<Vec3> {
        Deserializer() { super(Vec3.class); }

        @Override
        public Vec3 deserialize(JsonParser p, DeserializationContext context) throws IOException {
            return Vec3.parse(p.getValueAsString());
        }
    }
}