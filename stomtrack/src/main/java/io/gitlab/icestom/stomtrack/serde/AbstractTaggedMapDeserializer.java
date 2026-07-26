package io.gitlab.icestom.stomtrack.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.*;

public abstract class AbstractTaggedMapDeserializer<T> extends JsonDeserializer<Map<T, Set<String>>> {

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    protected static final String TAG_SEPARATOR = ",";

    private final Class<T> type;

    protected AbstractTaggedMapDeserializer(Class<T> type) {
        this.type = type;
    }

    @Override
    public Map<T, Set<String>> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode root = p.getCodec().readTree(p);
        Map<T, Set<String>> result = new LinkedHashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String typeName = field.getKey();
            JsonNode fieldValue = field.getValue();

            if (fieldValue.isArray()) {
                for (JsonNode item : fieldValue) {
                    addEntry(typeName, item, result);
                }
            } else {
                addEntry(typeName, fieldValue, result);
            }
        }

        return result;
    }

    private void addEntry(String typeName, JsonNode item, Map<T, Set<String>> result) throws IOException {
        ObjectNode itemCopy = ((ObjectNode) item).deepCopy();

        Set<String> tags = parseTags(itemCopy.get("tags"));
        itemCopy.remove("tags");

        ObjectNode wrapper = itemCopy.objectNode();
        wrapper.set(typeName, itemCopy);

        T value = MAPPER.treeToValue(wrapper, type);
        result.put(value, tags);
    }

    protected static Set<String> parseTags(JsonNode tagsNode) {
        Set<String> tags = new LinkedHashSet<>();
        if (tagsNode == null || tagsNode.isNull()) {
            return tags;
        }
        String raw = tagsNode.asText("");
        if (raw.isEmpty()) {
            return tags;
        }
        for (String tag : raw.split(TAG_SEPARATOR)) {
            String trimmed = tag.trim();
            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }
        return tags;
    }
}