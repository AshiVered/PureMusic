# PureMusic

PureMusic is a fork of [Metrolist](https://github.com/MetrolistGroup/Metrolist), an open-source YouTube Music client, with password-protected content restrictions. Android only.

Since 0.3 PureMusic is based on Metrolist v14 (the Kotlin Multiplatform rewrite). The PureMusic 0.x code based on Metrolist v13 is kept on the [`legacy`](https://github.com/AshiVered/PureMusic/tree/legacy) branch.

## Features

Everything lives in **Settings > Control & Management**, protected by a management password (also offered as a step in the first-run wizard):

- **Approved content only** – search YouTube Music for artists, podcasts and channels and approve them. Only their content is shown or played: home, search, albums, artist and podcast pages, radio, queue, library, history and Android Auto. Pages and links of anything else are blocked.
- **Home screen** – the approved podcasts and albums by the approved artists, with shuffle.
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

## Automatic Metrolist updates

`.github/workflows/upstream-sync.yml` runs daily (and on demand from the Actions tab):

1. Fetches Metrolist's `main`; stops if there is nothing new.
2. Merges it (`scripts/puremusic/merge_upstream.sh`). Metrolist's workflows, FUNDING and README conflicts are resolved automatically; any other conflict stops the run and opens an issue.
3. Verifies every PureMusic hook is still in place (`scripts/puremusic/check_hooks.py`).
4. Bumps the version (0.3 → 0.3.1 …) and builds a signed release APK.
5. Pushes to `main` and creates a **draft** release with release notes generated from Metrolist's commits (`scripts/puremusic/changelog.py`). Test it, then publish.

Failures open (or comment on) a "Metrolist sync failed" issue; nothing is pushed or released.

Required repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (the signing key) and `SYNC_TOKEN` (a token with `repo` and `workflow` scopes).

For a PureMusic feature release, bump `pureMusicVersion` in `androidApp/puremusic.properties`, reset `pureMusicPatch` to 0 and increase `pureMusicBuild`.

## Building

Requires JDK 21 (Gradle provisions Temurin 25), the Android SDK, NDK 30.0.16248370 and CMake 4.1.2.

```sh
./gradlew :androidApp:assembleDebug
```

## License

GPL-3.0, like Metrolist. See [LICENSE](LICENSE).
