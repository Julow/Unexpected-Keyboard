## Purpose

Lets users show the keyboard as a small, movable floating window on tablets and large screens instead of a full-width docked keyboard, controlled by a settings toggle.

## ADDED Requirements

### Requirement: Floating keyboard setting
The system SHALL provide a "Floating keyboard" setting that is disabled by default. Enabling it MUST switch the keyboard to floating mode; disabling it MUST return the keyboard to its normal full-width, bottom-docked layout.

#### Scenario: Enable floating mode from settings
- **WHEN** the user enables the "Floating keyboard" setting while the keyboard is shown
- **THEN** the keyboard is displayed as a small floating window

#### Scenario: Disable floating mode from settings
- **WHEN** the user disables the "Floating keyboard" setting
- **THEN** the keyboard is displayed as the normal full-width keyboard docked at the bottom of the screen

#### Scenario: Default state
- **WHEN** the app is freshly installed
- **THEN** the "Floating keyboard" setting is off and the keyboard uses the normal full-width layout

#### Scenario: Floating mode applies on every keyboard show
- **WHEN** the floating keyboard setting is enabled and the user opens a new text field
- **THEN** the keyboard appears as a floating window

### Requirement: Floating window size
In floating mode the system SHALL display the keyboard in a small window whose width is a fixed fraction of the screen width, capped at a maximum, and whose height adapts to the keyboard content.

#### Scenario: Window is smaller than the screen
- **WHEN** floating mode is enabled
- **THEN** the keyboard window width is visibly smaller than the full screen width

#### Scenario: Height adapts to content
- **WHEN** floating mode is enabled
- **THEN** the keyboard window height is only as tall as the keyboard (and candidates) content

### Requirement: Moving the floating window
In floating mode the system SHALL display a drag handle at the top of the keyboard. Dragging the handle MUST move the floating window, and the window MUST be kept within the visible screen area.

#### Scenario: Drag handle is visible
- **WHEN** floating mode is enabled
- **THEN** a drag handle is shown at the top of the keyboard

#### Scenario: Dragging moves the window
- **WHEN** the user drags the handle
- **THEN** the floating window moves to follow the drag

#### Scenario: Drag handle is hidden when docked
- **WHEN** floating mode is disabled
- **THEN** no drag handle is shown

### Requirement: Window position is remembered
The system SHALL remember the last position of the floating window and reuse it the next time the keyboard is shown in floating mode.

#### Scenario: Position persists
- **WHEN** the user moves the floating window and then hides and shows the keyboard again
- **THEN** the keyboard appears at the last position it was moved to

### Requirement: App behind the keyboard is not resized
In floating mode the system SHALL not push or resize the application behind the keyboard.

#### Scenario: Underlying app keeps its layout
- **WHEN** floating mode is enabled over an application
- **THEN** the application window behind the keyboard is not resized or panned by the keyboard's appearance

### Requirement: Keyboard functionality is preserved in floating mode
In floating mode the system SHALL keep all existing keyboard behavior working, including key input, suggestions, and the emoji and clipboard panes.

#### Scenario: Typing works in floating mode
- **WHEN** the user presses keys on the floating keyboard
- **THEN** the pressed characters are committed to the focused editor

#### Scenario: Emoji and clipboard panes remain usable
- **WHEN** the user opens the emoji or clipboard pane while in floating mode
- **THEN** the pane is shown within the floating window and remains usable
