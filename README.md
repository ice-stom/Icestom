# IceStom
A [Minestom](https://minestom.net/) based Ice Boat Racing server software.

> [!warning]
> IceStom is currently extremely alpha, it is probably not yet suitable for deployment. 

# Table of contents
- [Install](#Install)
- [Event panel](#Event-panel)
- [Advantages](#Advantages)
- [Disadvantages](#Disadvantages)
- [Credits](#Credits)

# Install
~~Download a jar from the releases page~~

## Prerequisites
- **JDK 25.** Minestom is compiled for Java 25 and IceStom's entrypoint relies on Java 25 accepting
  a non-public `main`. Gradle provisions 25 automatically for compiling, but you need a real JDK 25
  to *run* the server.
- **The OpenBoatUtils protocol library.** `io.github.openboatutils:Protocol` is not on any public
  repository; it is resolved from your local Maven cache. Publish it once:
  ```bash
  git clone -b dev https://github.com/OpenBoatUtils/OpenBoatUtils
  cd OpenBoatUtils && ./gradlew :Protocol:publishToMavenLocal
  ```
- **The panel site**, if you want the web panel. Clone
  [IcestomSite](https://github.com/ice-stom/IcestomSite) next to this repository; the build copies it
  into the jar. Without it everything still builds and runs, the panel URL just 404s.

## Build
```bash
./gradlew build # Linux
./gradlew.bat build # Windows
```

You can then execute the jar as you would with a regular Minecraft server:

```bash
java --enable-native-access=ALL-UNNAMED -jar IceStom-0.3.0.jar
```

The server writes its config, logs, events, tracks and database into the working directory, so run
it from a directory of its own. `config.toml` is created on first start.

## Running from IntelliJ
A shared run configuration lives in `.run/IceStom Server.run.xml`. It uses `run/` as the working
directory. Set the project SDK to JDK 25 first, otherwise it will not start.

The server isn't particually useful without any tracks,
contact @microwavedram for some track files, we are
currently working on some tooling to make track creation simple.


# Event panel
IceStom can host a web panel for building and running events, the way LuckPerms hosts an editor for
permissions. A player runs `/panel` and gets a one time link; the page it opens can assemble an
event out of the server's registered stages, start it with a chosen set of players, and drive it
live — starting the countdown, waving the chequered flag, moving everyone to the podium.

The panel is served by the server itself, on its own port, so there is no separate site to host and
no TLS certificate to obtain.

It is **off by default**, because anyone holding a link can start and cancel events. To turn it on,
in `config.toml`:

```toml
[web]
enabled = true
port = 8080
# What players should actually open. Without this the link only works on the machine itself.
public_url = http://play.example.com:8080
# Who may run /panel. Empty means links can only be minted from the console.
operators = [microwavedram]
```

Open that port, restart, and `/panel` will hand out working links. Links expire after
`session_minutes` (an hour by default) and are single use in practice: treat one like a password
for the duration.

The panel itself lives in the [IcestomSite](https://github.com/ice-stom/IcestomSite) repository and
is bundled into the jar at build time. See its README for how it is developed and how the API works.

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
