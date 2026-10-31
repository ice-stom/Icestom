package io.gitlab.icestom.icestom.event.stage;

public class InvalidStageArgumentsException extends RuntimeException {
    public InvalidStageArgumentsException(String message) {
        super(message);
    }
}
