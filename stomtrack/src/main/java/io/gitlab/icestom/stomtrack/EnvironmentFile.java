package io.gitlab.icestom.stomtrack;

public interface EnvironmentFile {

    int VERSION = 1;

    default int getVersion() {
        return VERSION;
    }

    int getAmbientLight();
    int getMinY();
    int getHeight();
    boolean getNetherLight();
    Skybox getSkybox();

    enum Skybox {
        NONE,
        OVERWORLD,
        END
    }
}
