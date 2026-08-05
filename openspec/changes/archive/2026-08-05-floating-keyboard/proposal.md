## Why

On tablets and large screens the keyboard always occupies a large bottom area of the screen, which is intrusive when typing in a small text field. Users want an optional "floating keyboard" mode where the keyboard is displayed as a small, movable window so it covers much less of the screen.

## What Changes

- Add a "Floating keyboard" preference (`floating_keyboard`, default off) to the settings screen.
- When enabled, the IME window is resized to a small fixed-ratio window (~70% of screen width, capped) instead of the full-width keyboard.
- Add a drag handle at the top of the keyboard that lets the user move the floating window around the screen; the last position is persisted.
- The keyboard keys are scaled to stay compact inside the smaller window.
- When disabled, the keyboard returns to its normal full-width, bottom-docked layout.
- The floating window reports zero insets so the app behind is not resized or pushed around.

## Capabilities

### New Capabilities
- `floating-keyboard`: Support for showing the keyboard as a small, movable floating window controlled by a settings toggle.

### Modified Capabilities
<!-- None -->

## Impact

- Settings screen: `res/xml/settings.xml` and `res/values/strings.xml` (new preference + strings).
- IME service: `srcs/juloo.keyboard2/Keyboard2.java` (window layout params, `onConfigureWindow`, `onComputeInsets`, drag handling, position persistence).
- Config: `srcs/juloo.keyboard2/Config.java` (new `floating_keyboard` flag and size constants).
- Keyboard sizing: `srcs/juloo.keyboard2/Keyboard2View.java` (`onMeasure` uses a reduced width in floating mode).
- Layout/UI: `res/layout/keyboard.xml` (drag handle) and a new small custom view for the handle.
- No dependency or API changes.
