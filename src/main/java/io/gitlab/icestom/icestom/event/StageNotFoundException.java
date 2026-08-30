package io.gitlab.icestom.icestom.event;

public class StageNotFoundException extends RuntimeException {
    public StageNotFoundException(String message) {
        super(message);
    }
}
