# Native crash symbols for Google Play

Release builds request `ndk.debugSymbolLevel = "FULL"`. Android Gradle Plugin
embeds available symbols in the AAB under
`BUNDLE-METADATA/com.android.tools.build.debugsymbols/`. Uploading that AAB
also uploads those symbols; no separate Play Console upload is needed.

CI checks the built AAB and reports symbol coverage per ABI/library. If AGP
produces `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip`,
CI also saves it as a separate artifact for manual upload when needed.

Some prebuilt dependencies are already stripped. In particular,
`androidx.graphics:graphics-path:1.0.1` distributes its native `.so` files
without `.symtab` or DWARF debug information. Gradle cannot recover the
removed symbols, so Play Console can still report missing native symbols.
Such a library requires matching symbols from its publisher, or a tested
replacement built from the corresponding source with debug information.
Do not upload empty or unrelated symbol files to hide this warning.

This limitation does not change the app's functionality or signing.

Reference: https://developer.android.com/build/include-native-symbols
