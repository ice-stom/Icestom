package io.gitlab.icestom.stomtrack.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gitlab.icestom.stomtrack.EnvironmentFile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.IOException;

public class SkyboxSerde {
    public static class SkyboxSerializer extends JsonSerializer<EnvironmentFile.Skybox> {
        @Override
        public void serialize(EnvironmentFile.Skybox value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {

            gen.writeString(value.toString());
        }
    }

    public static class SkyboxDeserializer extends JsonDeserializer<EnvironmentFile.Skybox> {
        @Override
        public EnvironmentFile.Skybox deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {

            return EnvironmentFile.Skybox.valueOf(p.getValueAsString());
        }
    }
}
