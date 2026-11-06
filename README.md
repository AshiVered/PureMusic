# PureMusic

PureMusic is a fork of [Metrolist](https://github.com/mostafaalagamy/Metrolist), an open-source YouTube Music client for Android.

In settings, after setting up a password, you can:

1. Picture blocking.
2. Song filtering based on a whitelist of approved artists.

## To-Do List

### Filtering & Home Screen

- [X] **1.** Improve the filtering system.
- [X] **2.** When filtering is enabled, display albums of approved artists on the home screen (matching the layout when disabled, with only approved content). Remove all existing sections (Speed Dial, Hits, etc.).
- [X] **3.** When filtering is disabled, display the latest songs on the home screen.
- [X] **4.** When filtering is enabled, show albums and songs by approved artists in search results.

### Picture Blocking

- [X] **5.** Extend picture blocking to artist search results accessed through Control & Management, whenever picture blocking is enabled.
- [X] **6.** Hide pictures on the lock screen whenever picture blocking is enabled.
- [X] **7.** Block artist cover in artist page when picture blocking is enabled.

### Content Restrictions

- [X] **8.** Move the Explicit Content toggle from the Content section to Control & Management.
- [X] **9.** Move the video toggles from the Content section to Control & Management.

### Application Identity & Cleanup

- [ ] **10.** Clean up the project directory and retain only the FOSS version.
- [X] **11.** Change the application name to PureMusic.
- [ ] **12.** Change the package name to `aiv.ashivered.puremusic`.
- [X] **13.** Change app icon - use image "logo.png" to create adaptive icons.

### Security

- [X] **14.** Why isn't the password set the first time it's set, and only after several times is it actually set? The first few times it asks again and again to set it.

### Updates & Information

- [ ] **15.** Disable the built-in updater until we establish our own update channel.
- [ ] **16.** Replace the original changelog and About section information with PureMusic-specific information.