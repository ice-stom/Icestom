package io.gitlab.icestom.icestom.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.stage.StageOption;
import io.gitlab.icestom.icestom.event.stage.StageSchema;
import net.kyori.adventure.key.Key;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record EventDocument(String name, List<Stage> stages) {

    public record Stage(String type, String name, Map<String, Object> options) {}

    public static EventDocument fromJson(JsonObject json) {
        String name = Http.requireString(json, "name");

        if (name.isBlank()) throw new Http.BadRequestException("Event name can't be blank");

        JsonElement stagesElement = json.get("stages");

        if (stagesElement == null || !stagesElement.isJsonArray()) {
            throw new Http.BadRequestException("Missing 'stages' array");
        }

        List<Stage> stages = new ArrayList<>();

        for (JsonElement element : stagesElement.getAsJsonArray()) {
            if (!element.isJsonObject()) throw new Http.BadRequestException("Each stage must be an object");

            stages.add(stageFromJson(element.getAsJsonObject()));
        }

        if (stages.isEmpty()) throw new Http.BadRequestException("An event needs at least one stage");

        return new EventDocument(name, stages);
    }

    private static Stage stageFromJson(JsonObject json) {
        String type = Http.requireString(json, "type");
        String stageName = Http.requireString(json, "name");

        if (stageName.isBlank()) throw new Http.BadRequestException("Stage name can't be blank");

        Key key;
        try {
            key = Key.key(type);
        } catch (Exception e) {
            throw new Http.BadRequestException("'" + type + "' isn't a valid stage key");
        }

        StageSchema schema = IceStom.getInstance().getStageRegistry().getSchema(key);

        if (schema == null) {
            throw new Http.BadRequestException("Stage type '" + type + "' can't be built from the panel");
        }

        JsonObject rawOptions = json.has("options") && json.get("options").isJsonObject()
                ? json.getAsJsonObject("options")
                : new JsonObject();

        Map<String, Object> options = new LinkedHashMap<>();

        for (StageOption option : schema.options()) {
            JsonElement value = rawOptions.get(option.name());

            if (value == null || value.isJsonNull()) {
                if (option.required()) {
                    throw new Http.BadRequestException(
                            "Stage '" + stageName + "' is missing required option '" + option.name() + "'");
                }

                if (option.defaultValue() != null) options.put(option.name(), option.defaultValue());

                continue;
            }

            options.put(option.name(), coerce(stageName, option, value));
        }

        return new Stage(key.asString(), stageName, options);
    }

    private static Object coerce(String stageName, StageOption option, JsonElement value) {
        if (!value.isJsonPrimitive()) {
            throw new Http.BadRequestException(
                    "Option '" + option.name() + "' on stage '" + stageName + "' must be a plain value");
        }

        return switch (option.type()) {
            case STRING, TRACK -> {
                String string = value.getAsString();

                if (string.isBlank()) {
                    throw new Http.BadRequestException(
                            "Option '" + option.name() + "' on stage '" + stageName + "' can't be blank");
                }

                yield string;
            }
            case BOOLEAN -> {
                if (!value.getAsJsonPrimitive().isBoolean()) {
                    throw new Http.BadRequestException(
                            "Option '" + option.name() + "' on stage '" + stageName + "' must be true or false");
                }

                yield value.getAsBoolean();
            }
            case NUMBER -> {
                if (!value.getAsJsonPrimitive().isNumber()) {
                    throw new Http.BadRequestException(
                            "Option '" + option.name() + "' on stage '" + stageName + "' must be a number");
                }

                double number = value.getAsDouble();

                if (option.min() != null && number < option.min()) {
                    throw new Http.BadRequestException(
                            "Option '" + option.name() + "' on stage '" + stageName + "' must be at least " + option.min());
                }

                if (option.max() != null && number > option.max()) {
                    throw new Http.BadRequestException(
                            "Option '" + option.name() + "' on stage '" + stageName + "' must be at most " + option.max());
                }

                yield number;
            }
        };
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);

        var array = new com.google.gson.JsonArray();

        for (Stage stage : stages) {
            JsonObject stageJson = new JsonObject();
            stageJson.addProperty("type", stage.type());
            stageJson.addProperty("name", stage.name());

            JsonObject options = new JsonObject();

            stage.options().forEach((key, value) -> {
                if (value instanceof Boolean bool) options.addProperty(key, bool);
                else if (value instanceof Number number) options.addProperty(key, number);
                else options.addProperty(key, String.valueOf(value));
            });

            stageJson.add("options", options);
            array.add(stageJson);
        }

        json.add("stages", array);

        return json;
    }
}
