# PureMusic

PureMusic is a fork of [Metrolist](https://github.com/mostafaalagamy/Metrolist), an open-source YouTube Music client for Android.

In settings, after setting up a password, you can:

1. Picture blocking.
2. Song filtering based on a whitelist of approved artists.

## To-Do List

### Filtering & Home Screen

- [X] **1.** Improve the filtering system.
- [ ] **2.** When filtering is enabled, display only approved artists on the home screen. Remove all existing sections (Speed Dial, Hits, etc.).
- [ ] **3.** When filtering is disabled, display the latest songs on the home screen.
- [ ] **4.** When filtering is enabled, show only songs by approved artists in search results.

### Picture Blocking

- [ ] **5.** Extend picture blocking to artist search results accessed through Control & Management, whenever picture blocking is enabled.
- [ ] **6.** Hide pictures on the lock screen whenever picture blocking is enabled.

### Content Restrictions

- [ ] **7.** Move the Explicit Content toggle from the Content section to Control & Management.
- [ ] **8.** Completely disable video playback:
  - Enable the existing video-hiding toggles in the Content section.
  - Remove the UI options that allow users to disable these restrictions.

### Application Identity & Cleanup

- [ ] **9.** Clean up the project directory and retain only the FOSS version.
- [ ] **10.** Change the application name to PureMusic.
- [ ] **11.** Change the package name to `aiv.ashivered.puremusic`.
- [ ] **12.** Change app icon.

### Updates & Information

- [ ] **12.** Disable the built-in updater until we establish our own update channel.
- [ ] **13.** Replace the original changelog and About section information with PureMusic-specific information.