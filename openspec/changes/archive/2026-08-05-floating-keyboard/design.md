## Context

Unexpected Keyboard is an `InputMethodService` (`Keyboard2`). Its UI is hosted in the framework-created soft-input window (`SoftInputWindow`, accessible via `getWindow().getWindow()`) of type `TYPE_INPUT_METHOD`. By default the framework sizes this window to `MATCH_PARENT` width with `Gravity.BOTTOM`, and it re-configures the window on mode changes. The app already manipulates these window params directly in `updateSoftInputWindowLayoutParams()` (full-height window, bottom-anchored `inputArea`).

Motivation and scope: see `proposal.md - Why`. Requirements: see `specs/floating-keyboard/spec.md`.

## Goals / Non-Goals

**Goals:**
- A settings toggle that switches the IME between normal full-width docking and a small floating window.
- Fixed-ratio floating size (~70% of screen width, capped at 700dp) so no resize controls are needed.
- Draggable window via a handle, with position persisted.
- Keep the app behind the keyboard un-resized while floating.

**Non-Goals:**
- User-adjustable window size (fixed ratio only).
- An on-keyboard quick "dock/undock" button (settings-only toggle; decided with the user).
- Resizable-on-drag window behavior.
- Adding a drag handle to the emoji/clipboard panes (they inherit the floating window but are not draggable).

## Decisions

### D1. Float by resizing the IME window, not a separate overlay
The keyboard stays in the existing IME window; we change its `WindowManager.LayoutParams` (`width` = the computed floating width, `height` = `WRAP_CONTENT`, `gravity` = `TOP|START`, plus persisted `x`/`y`). A fixed pixel width is used instead of `WRAP_CONTENT` because the candidates bar and the emoji/clipboard panes are `match_parent` and would expand a `WRAP_CONTENT`-width window to the full screen width.

- Alternative considered: a separate `TYPE_APPLICATION_OVERLAY` window hosting a second keyboard view. Rejected: requires `SYSTEM_ALERT_WINDOW` permission, breaks the `InputConnection`/editor binding model, and duplicates the whole view stack.

### D2. Re-apply floating params in `onConfigureWindow`
The framework resets the window to `MATCH_PARENT` whenever the window mode changes (`updateInputViewShown`, `updateFullscreenMode`), via `InputMethodService.onConfigureWindow`. We override it: call `super`, then `applyFloatingWindow()`. `setInputView()` already funnels into `updateSoftInputWindowLayoutParams()`, which branches to `applyFloatingWindow()` when the flag is set. This covers the keyboard, emoji and clipboard panes (all set via `setInputView`).

### D3. Size via `Keyboard2View.onMeasure` width
`applyFloatingWindow()` sets the window width to `floatingWindowWidth()` = `min(screenWidth * FLOATING_KEYBOARD_WIDTH_RATIO, density * FLOATING_KEYBOARD_MAX_WIDTH_DP)` and `Keyboard2View.onMeasure` (which currently forces `dm.widthPixels`) reports the same floating width so keys render and hit-test within the narrower window. Height stays content-based (`wrap_content`). `keyboard_rows_height_pixels` is scaled by `FLOATING_KEYBOARD_HEIGHT_SCALE` (0.75) in `Config.refresh()` when floating so keys stay compact in the smaller window.

### D4. Position and dragging
`gravity = TOP|START` makes `lp.x`/`lp.y` the window's top-left corner, so drag math is straightforward. A handle `View` (`FloatingHandleView`, draws three centered lines, ~24dp tall) is added at the top of `res/layout/keyboard.xml`, visible only in floating mode. Its `OnTouchListener` updates `lp.x`/`lp.y` on `ACTION_MOVE` (via `WindowManager.updateViewLayout(decorView, lp)`), clamps the window inside the screen bounds, and persists `floating_x`/`floating_y` to `SharedPreferences` on `ACTION_UP`.

- Alternative considered: positioning relative to `Gravity.BOTTOM` like the reference sample. Rejected for simplicity — `TOP|START` avoids the center-offset arithmetic.

### D5. Zero insets in floating mode
Override `onComputeInsets` to report `contentTopInsets`/`visibleTopInsets` = 0 in floating mode so the host app is not resized/panned (`adjustResize` apps stay put). Non-floating behavior is unchanged (delegates to `super`).

### D6. Toggle handling mid-session
`Keyboard2` already implements `OnSharedPreferenceChangeListener`. On a `floating_keyboard` change, after `refresh_config()` we call `updateSoftInputWindowLayoutParams()` and toggle the handle's visibility so the change applies without restarting the IME.

## Risks / Trade-offs

- Some OEM/Android versions may force the IME window to `MATCH_PARENT`, degrading floating to full width → Mitigation: `applyFloatingWindow()` is idempotent and cheap; the feature degrades gracefully. Accept as an OS limitation.
- Reporting zero insets hides keyboard presence from apps that shrink/pan based on it → Mitigation: expected and desired for a floating overlay; consistent with other floating keyboards.
- Emoji/clipboard panes render in the floating window but have no drag handle → Accepted Non-Goal; user returns to the main keyboard to reposition. The panes use `fill_parent` width and fixed dp heights, which adapt to a `WRAP_CONTENT` window.
- Initial floating position is an estimate (centered horizontally, ~25% from top) when no saved position exists; the window may be slightly off-center → Mitigation: stored position overrides it from the second use onward.
- Layout changes could break `check_layout.py` checks if layouts were edited → Not applicable: only `res/layout/*.xml` and Java files change, not `srcs/layouts/*`.

## Migration Plan

New opt-in preference defaulting to off; no data migration. Rollback is disabling the toggle. No config version bump required (new pref read with a default).
