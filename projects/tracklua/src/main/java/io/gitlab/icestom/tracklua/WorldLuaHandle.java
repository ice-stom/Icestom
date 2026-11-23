package io.gitlab.icestom.tracklua;

public interface WorldLuaHandle {
    boolean getWriteable();

    String getBlock(int x, int y, int z);
    void setBlock(int x, int y, int z, String material) throws UnwritableWorldException;

    class UnwritableWorldException extends Exception {}
}
