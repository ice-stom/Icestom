package io.gitlab.icestom.stomtrack.serde;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.openboatutils.protocol.channels.OBUSettingsPacket;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class OBUSettingsPacketSerde {

    @JsonIgnoreProperties({"protocolVersion", "version", "packetId", "channel"})
    private interface IgnoreProtocolVersionMixin {}

    private static final ObjectMapper PLAIN_MAPPER = new ObjectMapper()
            .addMixIn(OBUSettingsPacket.class, IgnoreProtocolVersionMixin.class)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    @SuppressWarnings("unchecked")
    private static final Map<String, Class<? extends OBUSettingsPacket>> TYPES =
            Arrays.stream(OBUSettingsPacket.class.getPermittedSubclasses())
                    .collect(Collectors.toMap(
                            Class::getSimpleName,
                            c -> (Class<? extends OBUSettingsPacket>) c
                    ));

    public static class OBUSettingsPacketSerializer extends JsonSerializer<OBUSettingsPacket> {
        @Override
        public void serialize(OBUSettingsPacket value,
                              JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            String type = value.getClass().getSimpleName();
            gen.writeStartObject();
            gen.writeFieldName(type);

            JsonNode node = PLAIN_MAPPER.valueToTree(value);
            gen.writeTree(node);

            gen.writeEndObject();
        }
    }

    public static class OBUSettingPacketDeserializer extends JsonDeserializer<OBUSettingsPacket> {
        @Override
        public OBUSettingsPacket deserialize(JsonParser p,
                                             DeserializationContext ctxt) throws IOException {
            JsonNode root = p.getCodec().readTree(p);
            String type = root.fieldNames().next();
            JsonNode content = root.get(type);
            Class<? extends OBUSettingsPacket> clazz = TYPES.get(type);
            if (clazz == null) {
                throw new IOException("Unknown OBUSettingsPacket type: " + type);
            }
            return PLAIN_MAPPER.treeToValue(content, clazz);
        }
    }

    public static SimpleModule module() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(OBUSettingsPacket.class, new OBUSettingsPacketSerializer());
        module.addDeserializer(OBUSettingsPacket.class, new OBUSettingPacketDeserializer());
        return module;
    }
}