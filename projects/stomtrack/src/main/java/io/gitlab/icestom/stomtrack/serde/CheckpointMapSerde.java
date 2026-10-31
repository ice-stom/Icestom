package io.gitlab.icestom.stomtrack.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import io.gitlab.icestom.stomtrack.TrackCheckpoint;

import java.io.IOException;
import java.util.*;

public class CheckpointMapSerde {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class Serializer extends JsonSerializer<Map<TrackCheckpoint, Integer>> {
        @Override
        public void serialize(Map<TrackCheckpoint, Integer> value,
                              JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            gen.writeStartObject();

            List<Map.Entry<TrackCheckpoint, Integer>> entries = new ArrayList<>(value.entrySet());
            entries.sort(Map.Entry.comparingByValue());

            for (Map.Entry<TrackCheckpoint, Integer> entry : entries) {
                ObjectNode wrapper = MAPPER.valueToTree(entry.getKey());
                String typeName = wrapper.fieldNames().next();
                ObjectNode inner = (ObjectNode) wrapper.get(typeName);

                gen.writeFieldName(typeName);
                gen.writeStartObject();

                if (gen instanceof ToXmlGenerator xgen) {
                    xgen.setNextIsAttribute(true);
                }
                gen.writeNumberField("index", entry.getValue());
                if (gen instanceof ToXmlGenerator xgen) {
                    xgen.setNextIsAttribute(false);
                }

                for (Iterator<Map.Entry<String, JsonNode>> it = inner.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> f = it.next();
                    gen.writeFieldName(f.getKey());
                    gen.writeTree(f.getValue());
                }

                gen.writeEndObject();
            }

            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<Map<TrackCheckpoint, Integer>> {
        @Override
        public Map<TrackCheckpoint, Integer> deserialize(JsonParser p,
                                                         DeserializationContext ctxt) throws IOException {
            JsonNode root = p.getCodec().readTree(p);
            Map<TrackCheckpoint, Integer> result = new LinkedHashMap<>();

            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String typeName = field.getKey();
                JsonNode fieldValue = field.getValue();

                if (fieldValue.isArray()) {
                    for (JsonNode item : fieldValue) {
                        addCheckpoint(typeName, item, result);
                    }
                } else {
                    addCheckpoint(typeName, fieldValue, result);
                }
            }
            return result;
        }

        private void addCheckpoint(String typeName, JsonNode item,
                                   Map<TrackCheckpoint, Integer> result) throws IOException {
            ObjectNode itemCopy = item.deepCopy();
            int index = itemCopy.get("index").asInt();
            itemCopy.remove("index");

            ObjectNode wrapper = itemCopy.objectNode();
            wrapper.set(typeName, itemCopy);

            TrackCheckpoint checkpoint = MAPPER.treeToValue(wrapper, TrackCheckpoint.class);
            result.put(checkpoint, index);
        }
    }
}