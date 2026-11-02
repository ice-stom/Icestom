package io.gitlab.icestom.stomtrack.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.IOException;

public class ComponentSerde {

    private static final GsonComponentSerializer SERIALIZER =
            GsonComponentSerializer.gson();

    public static class ComponentSerializer extends JsonSerializer<Component> {
        @Override
        public void serialize(Component value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {

            gen.writeString(SERIALIZER.serialize(value));
        }
    }

    public static class ComponentDeserializer extends JsonDeserializer<Component> {
        @Override
        public Component deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {

            return SERIALIZER.deserialize(p.getValueAsString());
        }
    }

    public static SimpleModule module() {
        SimpleModule module = new SimpleModule();

        module.addSerializer(Component.class, new ComponentSerializer());
        module.addDeserializer(Component.class, new ComponentDeserializer());

        return module;
    }
}
