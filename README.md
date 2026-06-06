# IceStom
A [Minestom](https://minestom.net/) based Ice Boat Racing server software.

> [!warning]
> IceStom is currently extremely alpha, it is probably not yet suitable for deployment. 

# Table of contents
- [Install](#Install)
- [Advantages](#Advantages)
- [Disadvantages](#Disadvantages)
- [Credits](#Credits)

# Install
~~Download a jar from the releases page~~

Build IceStom from source by cloning this repository and building with gradle
```bash
./gradlew build # Linux
./gradlew.bat build # Windows
```

You can then execute the jar as you would with a regular Minecraft server.

The server isn't particually useful without any tracks,
contact @microwavedram for some track files, we are
currently working on some tooling to make track creation simple.


# Advantages and Disadvantages
Icestom is very different from other Minecraft servers.

## Advantages
- Extremely Lightweight
- Small memory footprint
- Highly extensible
- Immutable (no world files, tracks are loaded from individual files and loaded into Minestom instances / worlds)
- No Mojank code, including the native non existence of boatlag
- Built from the lessons learnt through TimingSystem

## Disadvantages
- Lack of any vannila features (pretty much every single command)
- Entirely incompatible with Bukkit
- Doesn't work with older clients (running behind a proxy with Via is pretty much essential)
- Zero anticheat, not even the vanilla anticheat

# Credits
- The contributors
- [The Minestom contributors](https://github.com/Minestom/Minestom)
