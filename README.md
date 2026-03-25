# Unexpected Keyboard — pashol fork

Fork of [Julow/Unexpected-Keyboard](https://github.com/Julow/Unexpected-Keyboard) at v1.32.1.

## Changes from upstream

Cherry-picked from upstream master:

**Spell checking / suggestions**
- Candidates view (suggestion strip above the keyboard)
- Word tracking as you type, autocomplete on space bar, undo on delete
- Dictionary-based spell checking with downloadable dictionaries
- Option to disable suggestions

**Bug fixes**
- Fix autocapitalisation flicker
- Fix missing swipe vibration and visual feedback
- Fix alternate keyboard layouts rendering behind navbar
- Fix very small label size on 11+ column layouts
- Fix insets being excluded from computed width
- Fix parsing of escaped characters in macros
- Fix clipboard manager crashes
- Fix crash with Monet themes on Android 9
- Fix label color when keys are pressed
- Fix space and delete not repeating
- Fix crash in capitalized word suggestion
- Fix no keyboard after switching from clipboard manager
- Avoid loops in modmaps

**Build / SDK**
- Gradle upgrade (Kotlin DSL)
- Target SDK 36
- 16 KB page size dictionary compatibility (Pixel 9+)

## Building

See [Contributing](CONTRIBUTING.md).

Initialize submodules first (required for dictionary support):
```bash
git submodule update --init
./gradlew assembleDebug
```
