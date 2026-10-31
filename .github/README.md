![banner](/.github/assets/IcestomLogoText.png)

# IceStom
A [Minestom](https://minestom.net/) based Ice Boat Racing server software.

> [!warning]
> IceStom is currently in alpha, it is probably not yet suitable for deployment.

# Table of contents
- [Install](#Install)
- [Advantages](#Advantages)
- [Disadvantages](#Disadvantages)
- [Credits](#Credits)

# Install
You can [download a jar from the releases page](https://github.com/ice-stom/Icestom/releases).
and then execute the jar as you would with a regular Minecraft server.

The server isn't particually useful without any tracks, join the IceStom discord where a couple are stashed for now. You can also use the icestom mod to create your own tracks.

Otherwise, you can build IceStom from source by cloning this repository and building with gradle
```bash
./gradlew build # Linux
./gradlew.bat build # Windows
```

> [!warning]
> IceStom depends on the OpenBoatUtils protocol which isn't currently published
> anywhere, you'll need to clone [OpenBoatUtils](https://github.com/openboatutils/openboatutils)
> and publish the `:Protocol` to your local maven repo.
> scuffed? yes.

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
- ~~Zero anticheat, not even the vanilla anticheat.~~ see [Eschaton](https://github.com/ice-stom/eschaton)
- Alpha in every sense

# Credits
- [The contributors](https://github.com/ice-stom/Icestom/graphs/contributors)
- [The Minestom contributors](https://github.com/Minestom/Minestom)
