# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Setup

Initialize git submodules before building (required for `vendor/cdict`):
```sh
git submodule update --init
```

Set `ANDROID_HOME` or create `local.properties` with `sdk.dir=<path>` if not using Android Studio/Nix.

## Build & Test Commands

```sh
./gradlew assembleDebug          # Build debug APK → build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # Build and install on connected device
./gradlew test                   # Run unit tests (JVM, no device needed)
./gradlew checkKeyboardLayouts   # Validate layout XML files; updates check_layout.output (commit this)
./gradlew genLayoutsList         # Regenerate res/values/layouts.xml from srcs/layouts/
./gradlew compileComposeSequences  # Regenerate srcs/juloo.keyboard2/ComposeKeyData.java from srcs/compose/
./gradlew genMethodXml           # Regenerate res/xml/method.xml
```

Running a single test class:
```sh
./gradlew test --tests "juloo.keyboard2.KeyValueTest"
```

## Architecture

This is an Android IME (Input Method Editor) — a virtual keyboard app.

**Source layout:**
- `srcs/juloo.keyboard2/` — Main Java application code
- `srcs/layouts/` — Keyboard layout definitions in XML (e.g. `latn_qwerty_us.xml`)
- `srcs/compose/` — JSON files defining Shift/Fn/Compose/accent key mappings; compiled to `ComposeKeyData.java`
- `res/` — Android resources (strings, preferences UI, icons)
- `test/juloo.keyboard2/` — JVM unit tests (no Android device needed)
- `vendor/` — Git submodules (cdict for dictionary/suggestions, NDK build)

**Key classes:**
- `Keyboard2.java` — The `InputMethodService`; entry point for the IME
- `Keyboard2View.java` — Custom `View` that renders the keyboard
- `KeyValue.java` — Central model representing a key (character, action, modifier); parsed by `KeyValueParser.java`
- `KeyboardData.java` — Loads and represents layout XML
- `Pointers.java` — Handles touch input and swipe gesture detection
- `KeyEventHandler.java` — Translates key presses/swipes into text/key events sent to the app
- `KeyModifier.java` — Applies active modifiers (Shift, Fn, Ctrl, etc.) to key values
- `ComposeKey.java` / `ComposeKeyData.java` — Compose sequence state machine (ComposeKeyData is generated)
- `Config.java` — User preferences

**Generated files (do not edit directly):**
- `res/values/layouts.xml` — from `gen_layouts.py` / `./gradlew genLayoutsList`
- `res/xml/method.xml` — from `gen_method_xml.py`
- `srcs/juloo.keyboard2/ComposeKeyData.java` — from `srcs/compose/compile.py`

## Adding/Modifying Layouts

1. Create/edit XML in `srcs/layouts/` named as `<script>_<layout>_<country>.xml`
2. Include the `name` attribute (see `latn_qwerty_us.xml` for reference)
3. Run `./gradlew genLayoutsList` to register it
4. Run `./gradlew checkKeyboardLayouts` and commit the updated `check_layout.output`

Use the `loc` prefix for keys that are hidden unless needed by the current locale.

## Modifying Compose/Shift/Fn Sequences

Edit JSON in `srcs/compose/` (`shift.json`, `fn.json`, `compose/extra.json`, `accent_*.json`), then run:
```sh
./gradlew compileComposeSequences
```
Key names are listed in `doc/Possible-key-values.md`.
