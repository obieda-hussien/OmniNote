# Omni Note+

<img src="docs/branding/omni-noteplus.svg" alt="Omni Note+ icon" width="108" />

An offline-first Android notes app with Markdown editing, media attachments,
quick capture, archive/trash, search and productivity history.

The Android application ID and namespace are `com.omni.noteplus`. Main code,
unit tests and instrumented tests live under `java/com/omni/noteplus` in their
respective source sets. The launcher name and library title are **Omni Note+**.
File sharing uses the application-derived `com.omni.noteplus.fileprovider` authority.

## Build

Open this project in Android Studio, or use JDK 17 with the Android SDK:

```sh
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

The unit test suite uses JDK 21 for Robolectric Android 16:

```sh
./gradlew :app:testDebugUnitTest
```

GitHub Actions builds Debug and Release APKs and runs lint and unit tests.
Release signing requires the repository's configured keystore secrets. The
existing `OmniNote-Release.apk` at the repository root predates this rebrand;
use the new CI artifacts for the new application ID and icon.

## Existing installations

Changing the application ID makes Android treat this as a separate app. It can
be installed alongside the old one and does not automatically inherit its
private notes or attachments. Keep the old installation and export any
notes/attachments you want to preserve before uninstalling it. Database and
preference filenames remain stable within the new app; this package change
does not add a destructive database migration.

## Icon design

The mark combines a rounded **O**, two note lines and a **+**, on a restrained
purple gradient. The same vector geometry supplies adaptive icons, Android 13
themed icons, all five legacy launcher densities and the in-app emblem.

To regenerate the assets, install Pillow and Inkscape, then run:

```sh
python tools/generate_launcher_icons.py
```
