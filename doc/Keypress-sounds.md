# Native keypress sounds

Enable **Settings → Behavior → Sound on keypress** to hear feedback on the
on-screen keyboard. Sound is off by default and independent of vibration.
**Keypress sound volume** controls the local gain from 0–100% (default 50%).
Saved changes apply without restarting the keyboard.

A click is requested immediately on initial contact, using the center key after
modifiers. Corner swipes, circle/round-trip gestures, long-press alternatives,
and release do not add another click. The initial sound therefore may precede
selection of a different key, and cancellation cannot retract it. Held keys
request a click for each repeat using the selected key. Slider movement and
automatic modifier changes add no sound. Empty centers and slider values are
silent; physical keyboard input is unaffected.

Sounds use Android's `AudioManager.playSoundEffect(effect, volume)` with native
standard, space, delete and return effects. The explicit volume overload allows
the keyboard's own toggle and gain to work independently of Android's global
touch-sound preference. The percentage is linear gain, not perceived loudness.
Silent/vibrate modes, zero keyboard volume and zero system volume suppress
playback. Android retains control of DND, routing and other audio restrictions.
No audio focus is requested, and global settings and stream volumes are unchanged.

Native samples and latency vary by device; different effect IDs may sound the
same. Effects are prepared on a background thread when the view resets, with at
most one preparation in flight per view. Input does not wait for preparation,
so a very early first key can still encounter Android's lazy loading. The
keyboard does not unload shared system effects or provide custom sound assets.

JVM tests cover feedback callbacks, gesture/input preservation, settings,
effect selection, volume/ringer gates, preparation and failure recovery. They
use a fake audio boundary and a deterministic repeat queue; they do not verify
physical audio, Android message timing or device-specific audio policy.
