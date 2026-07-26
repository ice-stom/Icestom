package io.gitlab.icestom.stomtrack.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.gitlab.icestom.stomtrack.EnvironmentFile;
import io.gitlab.icestom.stomtrack.serde.SkyboxSerde;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("Environment")
public class MutableEnvironmentFile implements EnvironmentFile {

    @JacksonXmlProperty(isAttribute = true)
    private int version = EnvironmentFile.VERSION;
    private int ambientLight = 0;
    private int minY = -64;
    private int height = 384;
    private boolean netherLight = false;
    @JsonSerialize(using = SkyboxSerde.SkyboxSerializer.class)
    @JsonDeserialize(using = SkyboxSerde.SkyboxDeserializer.class)
    private Skybox skybox = Skybox.OVERWORLD;

    public void setAmbientLight(int ambientLight) { this.ambientLight = ambientLight; }
    public void setMinY(int minY) { this.minY = minY; }
    public void setHeight(int height) { this.height = height; }
    public void setNetherLight(boolean netherLight) { this.netherLight = netherLight; }
    public void setSkybox(Skybox skybox) { this.skybox = skybox; }

    @Override public int getAmbientLight() { return ambientLight; }
    @Override public int getMinY() { return minY; }
    @Override public int getHeight() { return height; }
    @Override public boolean getNetherLight() { return netherLight; }
    @Override public Skybox getSkybox() { return skybox; }
    @Override public int getVersion() { return version; }
}
