# Omni Note+

<img src="docs/branding/omni-noteplus.svg" alt="Omni Note+ icon" width="112" />

**A native Android notebook for Markdown, quick capture and an organized local library.**

Omni Note+ combines a Compose editor, image/audio/file attachments, active notes,
archive and trash, and durable writing statistics. Its identity follows the Omni
family: a purple palette and a clear rounded O/note/+ mark.

| App information | Value |
| --- | --- |
| Display name | Omni Note+ |
| Android application ID / namespace | `com.omni.noteplus` |
| Version | `1.0.0` |
| Android version code | `1` |
| Minimum Android version | Android 7.0 / API 24 |
| Target SDK | Android 16 / API 36 |
| Compile SDK | API 36, minor API level 1 |
| Platform | Android; Kotlin + Jetpack Compose |
| Local database | Room, schema version 4 |

## What the app does

### Writing and reading

- Create a note through the editor or the library's quick-capture bar.
- Edit Markdown with selection-aware formatting, undo/redo and a compact toolbar.
- Switch between editing and a rendered preview.
- Render headings, emphasis, links, nested lists, checklists, quotes, fenced code,
  tables and strikethrough. Preview checklist changes map back to their source line.
- Keep drafts and debounce autosave; save before leaving the editor. Failed writes
  keep the draft available for retry.
- Export a note as plain text or Markdown through Android's share sheet.

### Media and capture

- Choose images, audio and documents through the Android system file picker,
  without broad media/storage permissions; readable imports are copied into private
  app storage with sanitized, unique filenames.
- Display image previews, audio controls and file cards inside Markdown.
- Recognize supported image/audio extensions in existing generic file links.
- Receive shared text, images, audio, video and files from other Android apps.
- Record voice notes and use the device's speech-recognition service for dictation.
- Apply local text cleanup to dictated text. Optional Gemini refinement is available
  only in builds configured with an API key.

### Library and organization

- Keep Active, Archive and Trash as distinct sections, with grid and list views.
- Search title/content, filter by tags or pinned status, and sort by newest/oldest,
  title A–Z/Z–A or note color.
- Pin notes, assign tags and choose note background colors.
- Use a note PIN to restrict opening a note through the app interface.
- Swipe cards in physical directions consistently in LTR and RTL: right archives
  Active notes, unarchives Archived notes or restores Trashed notes; left moves
  notes to Trash or requests permanent-deletion confirmation.
- Undo supported library actions after the database write succeeds. Permanent
  deletion requires confirmation and cannot be undone.
- Keep search, sorting and capture within reach while the title/navigation chrome
  collapses during downward scrolling.

### Productivity

Track writing across the whole library, independently of the current section,
search or filter. The productivity sheet shows recorded notes/words, current
library counts, writing days, streaks and activity for all time, 7 days or 30 days.

History remains meaningful after archive, restoration and permanent deletion.
The ledger stores dates, counts and opaque identities rather than deleted note
text, attachment content or PINs. Initial tracking includes existing notes without
inventing their original creation dates.

## Storage, privacy and limitations

Normal note editing and library management work offline. Notes and imported
attachments live in the app's private storage. The app has no account-based cloud
sync or analytics/advertising integration.

Optional online operations have different behavior:

- Gemini refinement sends the requested text to Google's API when a configured
  build uses that feature.
- Speech recognition depends on the selected Android speech service and may need
  a network connection.
- Remote images/links in Markdown can contact their destination providers.

The note PIN is an **interface lock**, not encrypted storage. The current Room
schema stores note text and the note PIN locally without app-level encryption.
Protect the device and its backups accordingly. Android system backup and device
transfer follow the manifest/rules and the device's backup settings.

Text/Markdown export does not automatically package private attachment files.
Export or share needed media separately. See [Privacy policy](privacy_policy.html).

## Install and update

For a development build, open this repository's **Actions** tab, select a successful
Android CI/CD Pipeline run, then download the Debug APK artifact. Extract its ZIP
and install the APK on an Android device; Android may ask permission to install
apps from the selected browser/file manager.

| Artifact | Purpose |
| --- | --- |
| Debug APK | Installable development build, signed with a debug certificate |
| `release-unsigned` APK | Release compilation check; needs signing before installation |
| `release-signed` APK | Release build using the configured signing key |
| `AAB-release-unsigned` | Optimized app bundle; sign with your upload key before Play Console upload |
| `AAB-release-signed` | App bundle signed with the configured key, for Play Console upload |
| Quality / unit-test reports | Lint output and test results |
| Trivy report | Filesystem/dependency scan result |

Debug builds from different runners may use different debug certificates. If an
update reports a signature mismatch, retain/export your data before removing the
old installation. Production updates must use the same signing key and increasing
version codes.

### Moving from the former app ID

The previous ID was `com.aistudio.omninote.premium`. Android treats
`com.omni.noteplus` as a separate application, so it can coexist with the old app
and does not automatically inherit its private notes or media. Keep the old app
until you have preserved the content you need.

Version `1.0.0` / code `1` starts the new app identity. The **Room schema stays at
version 4**: app release versions and database schema versions are separate.
Existing database migrations remain in place.

## Build locally

Use Android Studio with the Android SDK and a compatible JDK. The Gradle wrapper
is included; no system-wide Gradle installation or `.env` file is required.

| Tool | Current configuration |
| --- | --- |
| Gradle wrapper | 9.3.1 |
| Android Gradle Plugin | 9.1.1 |
| JDK for app builds / lint | 17 |
| JDK for unit tests | 21, required by the Android 16 Robolectric test |
| Java source/target compatibility | 11 |

Configure the SDK path through Android Studio or an untracked `local.properties`
file. Install the compile platform matching API 36.1. Dependencies are declared
in `gradle/libs.versions.toml` and `app/build.gradle.kts`.

```sh
# Linux / macOS
chmod +x gradlew
./gradlew :app:assembleDebug
./gradlew :app:lintDebug

# Run with JDK 21 selected
./gradlew :app:testDebugUnitTest

# Release compilation; unsigned unless signing variables are configured
./gradlew :app:assembleRelease :app:bundleRelease
```

On Windows, use `gradlew.bat` with the same tasks. Instrumented tests are available
through `:app:connectedDebugAndroidTest` when a device/emulator is connected; the
regular CI pipeline runs JVM/Robolectric/Compose tests rather than device tests.

### Optional Gemini configuration

No API key is needed for normal builds or local note features. `BuildConfig.GEMINI_API_KEY`
defaults to an empty string. For a private development build, set `GEMINI_API_KEY`
as an environment variable or in your **user-level** `~/.gradle/gradle.properties`:

```properties
GEMINI_API_KEY=your_private_development_key
```

The Gradle property takes precedence over the environment variable. Never commit
real keys. A key compiled into an APK can be extracted; this build-time setting
is not a secure production credential store.

### Production signing

Local Release builds read these environment variables:

| Variable | Meaning |
| --- | --- |
| `KEYSTORE_PATH` | Path to the private signing keystore |
| `STORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Signing alias; defaults to `upload` locally |
| `KEY_PASSWORD` | Private-key password |

For GitHub Actions, add all four repository **Actions secrets**:

| Secret | Meaning |
| --- | --- |
| `KEYSTORE_BASE64` | Base64-encoded production keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Existing signing-key alias |
| `KEY_PASSWORD` | Private-key password |

Use a durable private key and keep its backup outside Git. A temporary or debug
key is not used as a replacement for a production key. Keystores and build
artifacts are ignored by Git.

## Google Play publication

New app releases use the **AAB**, not the standalone APK. The bundle is built at
`app/build/outputs/bundle/release/app-release.aab`; the APK remains available under
`app/build/outputs/apk/release/`. CI uploads each in a separate ZIP artifact.
It validates the bundle with pinned bundletool 1.18.3 (SHA-256 checked) and verifies
its application ID, version code and version name against Gradle.

An unsigned AAB is a build output, not a Play-ready upload. Sign it with your durable
**upload key**, or configure the signing variables above and rebuild. For a new
Play app, Play App Signing can generate and retain the distribution signing key;
your upload key authenticates the AAB you send to Play. Both keys have different
roles. Keep the upload keystore and its passwords private and backed up.

Recommended initial Console settings:

| Field | Value |
| --- | --- |
| App name | Omni Note+ |
| Package | `com.omni.noteplus` (must match the bundle; permanent once registered) |
| Default listing language | English (United States), matching the current app UI; add Arabic listing text separately |
| Type | App |
| Pricing | Free, if that is your intended business model |
| Category | Productivity |

Before rollout, supply real screenshots, a store icon/feature graphic, a public
HTML privacy-policy URL and accurate App content/Data safety declarations. The
included `privacy_policy.html` is source material; a GitHub source-file page is
not a hosted policy page. Core editing is local, but optional dictation, remote
media and any configured Gemini build must be considered in the disclosures.
No advertising or analytics SDK is configured in this project.

Start with internal testing and check editor saving, attachments, recording,
sharing, RTL layout and R8-optimized behavior on real devices. Personal developer
accounts created after November 13, 2023 require a closed test with at least 12
continuously opted-in testers for 14 days before applying for production access.
Answer Console policy/export declarations only after reviewing and accepting the
terms. Publishing every update requires a new, higher Android version code.

Official guidance: [app setup](https://support.google.com/googleplay/android-developer/answer/9859152),
[Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756),
[testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465).

## CI/CD behavior

The workflow runs on pushes to `main`/`develop`, pull requests, version tags and
manual dispatch. Every job uses **Ubuntu 24.04** explicitly. Android SDK setup uses
`android-actions/setup-android@v4`, whose action runtime is Node.js 24.

1. Lint, unit tests and a Trivy scan run independently.
2. Debug APK and Release APK/AAB builds run after those checks pass.
3. Artifacts and reports are uploaded, and the final job records pipeline status.

Pull-request Release checks are deliberately unsigned and do not receive signing
credentials. On ordinary branch pushes, Release is signed when all signing secrets
are present; with no secrets it produces separately labelled unsigned APK and AAB artifacts.
Partial/invalid signing configuration fails rather than silently falling back.

A version tag such as `v1.0.0` requires valid production signing, verifies the
APK signature with `apksigner` and AAB signature with `jarsigner`, and only then
publishes both files in a GitHub Release. Unsigned artifacts are never published
as a tagged production release.

Manual dispatch options:

| Input | Default | Behavior |
| --- | --- | --- |
| `build_release` | `true` | Include Release APK and AAB compilation; disable to run Debug/checks only |
| `require_signing` | `false` | Fail if a requested Release lacks a valid production key |
| `enable_extended_checks` | `false` | Also compile Release Kotlin and run Release unit tests |

The pipeline does not create production signing secrets. They must be supplied
by the repository owner before a signed distribution can be produced.

## Performance and release size

Release builds enable R8 code optimization/obfuscation and optimized resource
shrinking. Unused code from dependencies, unused resources and unused icon glyphs
can be removed; Debug stays easy to inspect. CI checks the compiled APK identity
and version and uploads the R8 mapping/shrink reports for crash retracing.

The library sections share one Room notes stream instead of three separate note
queries. Filtering, sorting, tag extraction and card-content analysis run off the
UI thread, and filter results are reused by navigation and page content. Word
counts scan text without allocating a separate string for every word. The Room
database is reused across activity recreation. Local Gradle builds use a 2 GiB
heap and two workers; CI applies its own build memory settings.

Existing artwork, image quality, animations and navigation transitions are kept.
These changes reduce avoidable work; actual startup/scrolling performance still
needs measurement on a physical device. APK sizes should be compared using the
same build type: Debug and optimized Release are different outputs.

## Source layout

| Path | Responsibility |
| --- | --- |
| `app/src/main/java/com/omni/noteplus/data/` | Room entities, DAOs, migrations, repository and productivity ledger |
| `app/src/main/java/com/omni/noteplus/ui/screens/` | Note library and editor screens |
| `app/src/main/java/com/omni/noteplus/ui/components/` | Markdown rendering, attachments, swipes and shared UI |
| `app/src/main/java/com/omni/noteplus/ui/editor/` | Editor buffer, selection and retained drafts |
| `app/src/main/java/com/omni/noteplus/ui/viewmodels/` | Screen state and serialized note actions |
| `app/src/main/java/com/omni/noteplus/ui/theme/` | Theme, palette and typography |
| `app/src/main/res/` | Strings, launcher/toolbar vectors and Android resources |
| `app/src/test/` | JVM, Room, Robolectric and Compose regression tests |
| `app/src/androidTest/` | Tests requiring a real device/emulator |
| `docs/branding/omni-noteplus.svg` | Editable vector brand reference |
| `gradle/` | Wrapper and dependency version catalog |
| `.github/scripts/verify-apk.py` | Checks compiled APK identity/version against Gradle |
| `.github/scripts/verify-bundle.py` | Validates the AAB with bundletool and checks identity/version |
| `.github/workflows/ci.yml` | Quality checks, tests, builds and release publication |

Launcher resources include standard/round legacy icons for five densities,
adaptive icons and a dedicated Android 13 monochrome mark. All runtime icon assets
are committed, so builds need no Python icon generator or external graphics tool.

Root-level Kotlin experiments, an old APK, a patch, an unused sample screenshot,
AI Studio metadata and placeholder secret files have been removed. Build outputs
belong in CI artifacts or `app/build/`, not in the source tree.

## Contributing

Work in a branch and open a pull request. Keep package declarations aligned with
source directories, retain database migrations when changing the schema, and add
regression coverage for meaningful behavior changes. Run lint and the relevant
unit tests before proposing a merge. UI/media/device changes also benefit from
physical-device checks, especially with RTL phone settings.

Project owner: [obieda-hussien](https://github.com/obieda-hussien).
No project license file is currently declared in this repository.
