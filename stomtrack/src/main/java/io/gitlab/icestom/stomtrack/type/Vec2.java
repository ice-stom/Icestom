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

@JsonSerialize(using = Vec2.Serializer.class)
@JsonDeserialize(using = Vec2.Deserializer.class)
public record Vec2(double x, double y) {

    @Override
    public @NotNull String toString() {
        return x + "," + y;
    }

    static Vec2 parse(String text) {
        String[] parts = text.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected \"x,y\", got: " + text);
        }
        return new Vec2(
                Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim())
        );
    }

    static final class Serializer extends StdSerializer<Vec2> {
        Serializer() { super(Vec2.class); }

        @Override
        public void serialize(Vec2 value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }

    static final class Deserializer extends StdDeserializer<Vec2> {
        Deserializer() { super(Vec2.class); }

        @Override
        public Vec2 deserialize(JsonParser p, DeserializationContext context) throws IOException {
            return Vec2.parse(p.getValueAsString());
        }
    }
}