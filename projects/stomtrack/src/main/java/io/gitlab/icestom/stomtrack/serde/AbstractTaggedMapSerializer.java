package io.gitlab.icestom.stomtrack.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import java.io.IOException;
import java.util.*;

public abstract class AbstractTaggedMapSerializer<T> extends JsonSerializer<Map<T, Set<String>>> {

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    protected static final String TAG_SEPARATOR = ",";

    @Override
    public void serialize(Map<T, Set<String>> value,
                          JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        List<Map.Entry<T, Set<String>>> entries = new ArrayList<>(value.entrySet());
        entries.sort(Comparator.comparing(e -> joinTags(e.getValue())));

        for (Map.Entry<T, Set<String>> entry : entries) {
            ObjectNode wrapper = MAPPER.valueToTree(entry.getKey());
            String typeName = wrapper.fieldNames().next();
            ObjectNode inner = (ObjectNode) wrapper.get(typeName);

            gen.writeFieldName(typeName);
            gen.writeStartObject();

            if (gen instanceof ToXmlGenerator xgen) {
                xgen.setNextIsAttribute(true);
            }
            gen.writeStringField("tags", joinTags(entry.getValue()));
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

    protected static String joinTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) return "";
        List<String> sorted = new ArrayList<>(tags);
        Collections.sort(sorted);
        return String.join(TAG_SEPARATOR, sorted);
    }
}