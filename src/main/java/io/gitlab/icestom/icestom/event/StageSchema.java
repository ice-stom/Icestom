package io.gitlab.icestom.icestom.event;

import java.util.List;

public record StageSchema(String label, String description, List<StageOption> options) {

    public StageSchema {
        options = List.copyOf(options);
    }

    public static StageSchema of(String label, String description, StageOption... options) {
        return new StageSchema(label, description, List.of(options));
    }
}
