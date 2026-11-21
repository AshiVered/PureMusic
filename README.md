# PureMusic

PureMusic is a fork of [Metrolist](https://github.com/MetrolistGroup/Metrolist), an open-source YouTube Music client, with password-protected content restrictions. Android only.

Since 0.3 PureMusic is based on Metrolist v14 (the Kotlin Multiplatform rewrite). The PureMusic 0.x code based on Metrolist v13 is kept on the [`legacy`](https://github.com/AshiVered/PureMusic/tree/legacy) branch.

## Features

Everything lives in **Settings > Control & Management**, protected by a management password (also offered as a step in the first-run wizard):

- **Approved artists only** – search YouTube Music for artists and approve them. Only their music is shown or played: home, search, albums, artist pages, radio, queue, library, history and Android Auto. Artist pages and links of other artists are blocked.
- **Home screen** – albums by the approved artists, with shuffle.
- **Hide images** – album covers, artist pictures and notification artwork (hidden by default).
- **Hide explicit content, video songs and non-musical videos.**
- **Disable** music videos, animated artwork (Canvas), Listen Together, Cast and Device Connect (all disabled by default).
- No crash reports or telemetry are sent.
- Restrictions are excluded from backup and restore, so restoring a backup cannot weaken them.

Upgrading from PureMusic 0.x keeps the password, switches and approved artists. If the approved artists cannot be migrated, everything stays blocked until the settings are reviewed.

## How the fork is built

PureMusic keeps its changes small so Metrolist updates can be merged automatically:

- PureMusic code lives in its own packages (`shared/.../puremusic`, `androidApp/.../puremusic`), its own strings (`puremusic_strings.xml`) and its own build scripts (`*/puremusic.gradle`).
- Metrolist files only get short hook lines marked `PUREMUSIC-HOOK: <name>`. `python3 scripts/puremusic/check_hooks.py` fails if a hook disappears after a merge.
- The app name is replaced at build time, so Metrolist's translation catalogs stay untouched.

## Building

Requires JDK 21 (Gradle provisions Temurin 25), the Android SDK, NDK 30.0.16248370 and CMake 4.1.2.

```sh
./gradlew :androidApp:assembleDebug
```

## License

GPL-3.0, like Metrolist. See [LICENSE](LICENSE).
