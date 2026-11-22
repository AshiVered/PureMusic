#!/usr/bin/env python3
"""Fails when a PureMusic hook disappeared from an upstream file.

Every line PureMusic adds to Metrolist code carries a `PUREMUSIC-HOOK: <name>` marker. After an upstream
merge, run this before building: if Metrolist rewrote one of these spots, the restriction it carried is
gone even though git reported no conflict.
"""
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

HOOKS = {
    "settings-store": "shared/src/commonMain/kotlin/com/metrolist/shared/settings/Settings.kt",
    "backup-exclude": "shared/src/commonMain/kotlin/com/metrolist/shared/settings/Settings.kt",
    "item-filter": "shared/src/commonMain/kotlin/com/metrolist/shared/settings/BlockedContent.kt",
    "song-filter": "shared/src/commonMain/kotlin/com/metrolist/shared/settings/BlockedContent.kt",
    "install": "androidApp/src/main/kotlin/com/metrolist/android/App.kt",
    "image-interceptor": "androidApp/src/main/kotlin/com/metrolist/android/utils/AppImageLoader.kt",
    "media-artwork": "androidApp/src/main/kotlin/com/metrolist/android/playback/MediaArtworkProvider.kt",
    "playback-gate": "androidApp/src/main/kotlin/com/metrolist/android/playback/MusicService.kt",
    "queue-notice": "androidApp/src/main/kotlin/com/metrolist/android/playback/AndroidPlaybackPolicies.kt",
    "settings-home": "shared/src/commonMain/kotlin/com/metrolist/shared/ui/settings/SharedSettingsScreen.kt",
    "settings-pane": "shared/src/commonMain/kotlin/com/metrolist/shared/ui/settings/SharedSettingsScreen.kt",
    "control-activity": "androidApp/src/main/AndroidManifest.xml",
    "branding": "androidApp/build.gradle.kts",
    "shared-branding": "shared/build.gradle.kts",
    "onboarding-brand": "shared/src/commonMain/kotlin/com/metrolist/shared/ui/onboarding/OnboardingContent.kt",
    "onboarding-step": "androidApp/src/main/kotlin/com/metrolist/android/onboarding/OnboardingScreen.kt",
    "no-crash-reports": "shared/src/commonMain/kotlin/com/metrolist/shared/utils/CrashReporting.kt",
    "home": "androidApp/src/main/kotlin/com/metrolist/android/ui/screens/HomeScreen.kt",
    "channel-page": "shared/src/commonMain/kotlin/com/metrolist/shared/api/YouTubeArtistParser.kt",
    "browse-owner": "shared/src/commonMain/kotlin/com/metrolist/shared/api/YouTubeBrowseOperations.kt",
    "podcast-gate": "androidApp/src/main/kotlin/com/metrolist/android/ui/screens/PodcastScreen.kt",
    "artist-gate": "androidApp/src/main/kotlin/com/metrolist/android/ui/screens/ArtistScreenHost.kt",
    "artwork-placeholder": "shared/src/commonMain/kotlin/com/metrolist/shared/ui/components/YTItemCards.kt",
    "playlist-cover": "shared/src/commonMain/kotlin/com/metrolist/shared/ui/components/PlaylistCoverImage.kt",
    "image-hidden": "shared/src/androidMain/kotlin/com/metrolist/shared/ui/components/images/AsyncImage.kt",
    "image-error": "shared/src/androidMain/kotlin/com/metrolist/shared/ui/components/images/AsyncImage.kt",
    "onboarding-numbering": "androidApp/src/main/kotlin/com/metrolist/android/onboarding/OnboardingScreen.kt",
    "about": "androidApp/src/main/kotlin/com/metrolist/android/ui/screens/settings/AboutScreen.kt",
}

# Upstream symbols the hooks rely on. If one is renamed, the hook may still be present but dead.
UPSTREAM_ANCHORS = {
    "androidApp/src/main/kotlin/com/metrolist/android/playback/AndroidQueueCoordinator.kt": "filteredQueueSelection(",
    "shared/src/commonMain/kotlin/com/metrolist/shared/api/YouTubeSearchOperations.kt": "BlockedContent.filterItems(",
    "shared/src/commonMain/kotlin/com/metrolist/shared/api/YouTubeRepository.kt": "BlockedContent.filter",
}


def main() -> int:
    failures = []
    for name, rel in HOOKS.items():
        path = ROOT / rel
        if not path.is_file():
            failures.append(f"{name}: {rel} no longer exists")
        elif f"PUREMUSIC-HOOK: {name}" not in path.read_text(encoding="utf-8"):
            failures.append(f"{name}: marker missing in {rel}")
    for rel, needle in UPSTREAM_ANCHORS.items():
        path = ROOT / rel
        if not path.is_file() or needle not in path.read_text(encoding="utf-8"):
            failures.append(f"upstream anchor `{needle}` missing in {rel}")
    for line in failures:
        print(f"PUREMUSIC HOOK CHECK FAILED: {line}")
    if not failures:
        print(f"All {len(HOOKS)} PureMusic hooks present.")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
