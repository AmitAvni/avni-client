# Avni Client

A lightweight, open-source **Minecraft: Java Edition** utility client — a Fabric mod paired with a
desktop launcher. Built as a personal, non-commercial hobby project.

Avni has two parts:

1. **The mod** — a [Fabric](https://fabricmc.net/) mod for Minecraft **1.21.11** that adds
   quality-of-life features: a configurable HUD, zoom, fullbright, keystrokes, waypoints, and a
   custom main-menu theme.
2. **The launcher** — a native [JavaFX](https://openjfx.io/) desktop app that downloads and installs
   Minecraft + Fabric, manages accounts and versions, and launches the game with the mod bundled in.

> The launcher signs players in with **their own** Microsoft accounts (standard Microsoft/Xbox
> OAuth device-code flow) purely to obtain their profile for launching the game they own. It does
> not modify, resell, or proxy any Microsoft or Mojang services.

## Features

### In-game mod
- **Movable, resizable HUD** with a drag-and-drop editor (snap to edges and to other elements)
  - FPS, coordinates + chunk + biome panel, facing direction, CPS, memory, day counter,
    real-time clock, session timer, a compass strip with waypoint markers, and a keystrokes overlay
- **Waypoints** — create, color, and navigate to saved locations (distance + direction + compass)
- **Zoom** (hold), **fullbright** toggle
- **Custom title screen** matching the client's theme
- A clean in-game settings panel with real toggle switches

### Launcher
- Installs Minecraft, Fabric, and the matching Fabric API automatically
- **Version picker** for the full Fabric-supported range (incl. the year-based 26.x releases),
  downloading the correct Java runtime per version
- **Accounts** — offline accounts, plus Microsoft sign-in
- **Settings** — RAM allocation, game directory, and in-game feature toggles
- A polished, themed UI

## Building

Requires **JDK 21**.

```bash
# Build the mod jar
./gradlew build

# Run the launcher
./gradlew :launcher:run

# Run the mod in a dev client
./gradlew runClient
```

## Tech

- Minecraft 1.21.11 · Fabric Loader · Fabric API
- Java 21 · JavaFX 21 · Gradle

## License

[MIT](LICENSE)
