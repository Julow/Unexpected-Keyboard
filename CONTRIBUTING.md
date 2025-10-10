# Contributing

Thanks for contributing :)

## Building the app

The application uses Gradle and can be used with Android Studio, but using
Android Studio is not required. The build dependencies are:
- OpenJDK 17
- Android SDK: build tools (minimum `28.0.1`), platform `30`

Python 3 is required to update generated files but not to build the app.

For Android Studio users, no more setup is needed.

For Nix users, the right environment can be obtained with `nix-shell ./shell.nix`.
Instructions to install Nix are [here](https://wiki.nixos.org/wiki/Nix_Installation_Guide).

If you don't use Android Studio or Nix, you have to inform Gradle about the
location of your Android SDK by either:
- Setting the `ANDROID_HOME` environment variable to point to the android sdk or
- Creating the file `local.properties` and writing
  `sdk.dir=<location_of_android_home>` into it.

Building the debug apk:

```sh
./gradlew assembleDebug
```

Nix users can call gradle directly: `gradle assembleDebug`.

If the build succeeds, the debug apk is located in `build/outputs/apk/debug/app-debug.apk`.

## Debugging on your phone

First [Enable adb debugging on your device](https://developer.android.com/studio/command-line/adb#Enabling).
Then connect your phone to your computer using an USB cable or via wireless
debugging.

If you use Android Studio, this process will be automatic and you don't have to
follow this guide anymore.

And finally, install the application with:
```sh
./gradlew installDebug
```

The released version of the application won't be removed, both versions will
be installed at the same time.

## Debugging the application: INSTALL_FAILED_UPDATE_INCOMPATIBLE

`./gradlew installDebug` can fail with the following error message:

```
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':installDebug'.
> java.util.concurrent.ExecutionException: com.android.builder.testing.api.DeviceException: com.android.ddmlib.InstallException: INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package juloo.keyboard2.debug signatures do not match newer version; ignoring!
```

The application can't be "updated" because the temporary certificate has been
lost. The solution is to uninstall and install again.
The application must be enabled again in the settings.

```sh
adb uninstall juloo.keyboard2.debug
./gradlew installDebug
```

## Specifying a debug signing certificate on Github Actions

It's possible to specify the signing certificate that the automated build
should use.
After you successfully run `./gradlew asssembleDebug`, (thus a debug.keystore
exists) you can use this second command to generate a base64 stringified
version of it:

```sh
gpg -c --armor --pinentry-mode loopback --passphrase debug0 --yes "debug.keystore"
```

This will create the file `debug.keystore.asc`, paste its content into a new
Github secret named `DEBUG_KEYSTORE`.

## Guidelines

### Adding a layout

Layouts are defined in XML, see `srcs/layouts/latn_qwerty_us.xml`.
An online tool for editing layout files written by @Lixquid is available
[here](https://unexpected-keyboard-layout-editor.lixquid.com/).

Makes sure to specify the `name` attribute like in `latn_qwerty_us.xml`,
otherwise the layout won't be added to the app.

The layout file must be placed in the `srcs/layouts` directory and named
according to:
- script (`latn` for latin, etc..)
- layout name (eg. the name of a standard)
- country code (or language code if more adequate)

Then, run `./gradlew genLayoutsList` to add the layout to the app.

The last step will update the file `res/values/layouts.xml`, that you should
not edit directly.

Run `./gradlew checkKeyboardLayouts` to check some properties about your
layout. This will change the file `check_layout.output`, which you should
commit.

Layouts are CC0 licensed by default. If you do not want your layout to be
released into the public domain, add a copyright notice at the top of the file
and a mention in `srcs/layouts/LICENSE`.

#### Adding a programming layout

A programming layout must contain all ASCII characters.
The current programming layouts are: QWERTY, Dvorak and Colemak.

See for example, Dvorak, added in https://github.com/Julow/Unexpected-Keyboard/pull/16

It's best to leave free spots on the layout for language-specific symbols that
are added automatically when necessary.
These symbols are defined in `res/xml/method.xml` (`extra_keys`).

It's possible to place extra keys with the `loc` prefix. These keys are
normally hidden unless they are needed.

Some users cannot easily type the characters close the the edges of the screen
due to a bulky phone case. It is best to avoid placing important characters
there (such as the digits or punctuation).

#### Adding a localized layout

Localized layouts (a layout specific to a language) are gladly accepted.
See for example: 4333575 (Bulgarian), 88e2175 (Latvian), 133b6ec (German).

They don't need to contain every ASCII characters (although it's useful in
passwords) and dead-keys.

### Adding support for a language

Supported locales are defined in `res/xml/method.xml`.

The attributes `languageTag` and `imeSubtypeLocale` define a locale, the
attribute `imeSubtypeExtraValue` defines the default layout and the dead-keys
and other extra keys to show.

The list of language tags (generally two letters)
and locales (generally of the form `xx_XX`)
can be found in this [stackoverflow answer](https://stackoverflow.com/a/7989085)

### Updating translations

The text used in the app is written in `res/values-<language_tag>/strings.xml`.

The list of language tags can be found in this
[stackoverflow answer](https://stackoverflow.com/a/7989085)

The first part before the `_` is used, for example,
`res/values-fr/strings.xml` for French,
`res/values-lv/strings.xml` for Latvian.

Commented-out lines indicate missing translations:

```xml
  <!-- <string name="pref_layouts_add">Add an alternate layout</string> -->
```

Remove the `<!--` and `-->` parts and change the text.

### Adding a translation

The preferred method for translating the app is to use Weblate:
https://hosted.weblate.org/engage/unexpected-keyboard/

The `res/values-<language_tag>/strings.xml` file must be created by copying the
default translation in `res/values/strings.xml`, which contain the structure of
the file and the English strings.

Store descriptions in `fastlane/metadata/android/` are updated automatically.
Translating changelogs is not useful.

The app name might be partially translated, the "Unexpected" word should remain
untranslated if possible.

As translations need to be updated regularly, you can subscribe to this issue
to receive a notification when an update is needed:
https://github.com/Julow/Unexpected-Keyboard/issues/373

### Adding symbols to Shift, Fn, Compose and other modifiers

New key combinations can be added to builtin modifiers in the following files:

- Shift in `srcs/compose/shift.json`.
- Fn in `srcs/compose/fn.json`.
- Compose in `srcs/compose/compose/extra.json`.
- Other modifiers are defined in the `accent_*.json` files in `srcs/compose`.

Generated code must then be updated by running:

```
./gradlew compileComposeSequences
```

These files describe each symbols that get transformed when a given modifier is
activated, in JSON format. For example:

Example from `fn.json`, when `Fn` is activated, `<` becomes `«`:
```json
{
  "<": "«",
}
```

The result of a sequence can be a key name. See the list of key names in
[doc/Possible-key-values.md](doc/Possible-key-values.md). For example from
`fn.json`, when `Fn` is activated, space becomes `nbsp`:
```json
{
  " ": "nbsp",
}
```

Compose sequences are made of several steps. For example, the sequence
`Compose V s = Š` is defined as:
```json
{
  "V": {
    "s": "Š"
  }
}
```
