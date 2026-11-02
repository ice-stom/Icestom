package io.gitlab.icestom.icestom.event.stage;

public class StageNotFoundException extends RuntimeException {
    public StageNotFoundException(String message) {
        super(message);
    }
}
