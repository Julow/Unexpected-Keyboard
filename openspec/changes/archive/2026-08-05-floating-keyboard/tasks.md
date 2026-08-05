## 1. Settings and configuration

- [x] 1.1 Add `pref_floating_keyboard_title` and `pref_floating_keyboard_summary` strings to `res/values/strings.xml`
- [x] 1.2 Add the `floating_keyboard` `CheckBoxPreference` (default `false`) under the Behavior category in `res/xml/settings.xml`
- [x] 1.3 Add `public boolean floating_keyboard` to `Config.java`, read it in `refresh()` from `_prefs.getBoolean("floating_keyboard", false)`
- [x] 1.4 Add size constants to `Config.java`: `FLOATING_KEYBOARD_WIDTH_RATIO` (0.7), `FLOATING_KEYBOARD_MAX_WIDTH_DP` (700), `FLOATING_KEYBOARD_HEIGHT_SCALE` (0.75)
- [x] 1.5 In `Config.refresh()`, scale `keyboard_rows_height_pixels` by `FLOATING_KEYBOARD_HEIGHT_SCALE` when `floating_keyboard` is set

## 2. Drag handle UI

- [x] 2.1 Create `srcs/juloo.keyboard2/FloatingHandleView.java`, a small View (~24dp tall) that draws three centered rounded grabber lines in `onDraw`
- [x] 2.2 Add the handle (`<juloo.keyboard2.FloatingHandleView android:id="@+id/floating_handle">`, `visibility="gone"`) as the first child of the root layout in `res/layout/keyboard.xml`

## 3. Floating window sizing and positioning

- [x] 3.1 In `Keyboard2View.onMeasure`, when `_config.floating_keyboard` compute width as `min(screenWidth * FLOATING_KEYBOARD_WIDTH_RATIO, density * FLOATING_KEYBOARD_MAX_WIDTH_DP)` instead of `dm.widthPixels`
- [x] 3.2 Add `Keyboard2.applyFloatingWindow()` setting `lp.gravity = TOP|START`, `lp.width` = `floatingWindowWidth()` (`min(screenWidth * FLOATING_KEYBOARD_WIDTH_RATIO, density * FLOATING_KEYBOARD_MAX_WIDTH_DP)`), `lp.height = WRAP_CONTENT`, and `lp.x/lp.y` from the stored position
- [x] 3.3 Branch `updateSoftInputWindowLayoutParams()` to call `applyFloatingWindow()` when `_config.floating_keyboard` and keep the existing full-height logic otherwise
- [x] 3.4 Override `onConfigureWindow(Window, boolean, boolean)` in `Keyboard2` to call `super` then `applyFloatingWindow()`
- [x] 3.5 Override `onComputeInsets(Insets)` in `Keyboard2` to report zero `contentTopInsets`/`visibleTopInsets` when floating, delegating to `super` otherwise

## 4. Dragging and persistence

- [x] 4.1 In `create_keyboard_view()`, attach an `OnTouchListener` to `floating_handle` that moves the window by updating `lp.x/lp.y` via `WindowManager.updateViewLayout(decorView, lp)`
- [x] 4.2 Clamp the dragged position within the visible screen area during `ACTION_MOVE`
- [x] 4.3 Persist `floating_x`/`floating_y` to `SharedPreferences` on `ACTION_UP`; load them in `Keyboard2` and fall back to a sensible default position (centered horizontally, ~25% from top) when unset
- [x] 4.4 Toggle `floating_handle` visibility based on `_config.floating_keyboard` in `onStartInputView()` and `refresh_config()`
- [x] 4.5 In `onSharedPreferenceChanged`, after `refresh_config()`, call `updateSoftInputWindowLayoutParams()` and update the handle visibility so toggling the setting applies immediately

## 5. Verification

- [x] 5.1 Build with `./gradlew assembleDebug` and fix any compile errors
- [x] 5.2 Run `./gradlew test` (and `./gradlew checkKeyboardLayouts` if layouts changed) and fix failures
- [x] 5.3 Manually verify the spec scenarios: toggle floating in settings, window is ~70% width, draggable within screen, position persists, app behind is not resized, typing/suggestions/emoji/clipboard still work, and disabling the toggle restores the full-width keyboard
