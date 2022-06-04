# Contributing

Thanks for contributing :)

## Building the app

The application doesn't use Gradle and it might be hard to use some features of
Android Studio.

Fortunately, there's not many dependencies:
- OpenJDK 8
- Android SDK: build tools (minimum `28.0.1`), platform `30`
- Make sure to have the `$ANDROID_HOME` environment variable set.

For Nix users, the right environment can be obtained with `nix-shell ./shell.nix`.
Instructions to install Nix are [here](https://nixos.wiki/wiki/Nix_Installation_Guide).

Building the debug apk:

```sh
make
```

If the build succeed, the debug apk is located in `_build/juloo.keyboard2.debug.apk`.

## Using the local debug.keystore on the Github CI actions

It's possible to save the local debug.keystore into a github secret, so the same keystore is utilized to build the debug apk in the CI github actions.
Doing this, they wil have the same signature, thus the debug apk can be updated without having to uninstall it first.

After you sucessfully run `make`, (thus a debug.keystore exists) you can use this second command to generate a base64 stringified version of it

```sh
cd _build
gpg -c --armor --pinentry-mode loopback --passphrase debug0 --yes "debug.keystore"
```

A file will be generated inside the local `_build/` folder, called `debug.keystore.asc`

You can copy the content of this file, and with that, paste it into a new github secret in your repo settings. 

The secret must be named `DEBUG_KEYSTORE`

## Debugging on your phone

First [Enable adb debugging on your device](https://developer.android.com/studio/command-line/adb#Enabling).
Then connect your phone to your computer using an USB cable or wireless
debugging.

And finally, install the application with:
```sh
make installd
```

The debug version of the application won't be removed, both versions will stay
installed at the same time.

The application must be enabled in the settings:
System > Languages & input > On-screen keyboard > Manage on-screen keyboards.

## Debugging the application: INSTALL_FAILED_UPDATE_INCOMPATIBLE

`make installd` can fail with the following error message:

```
adb: failed to install _build/juloo.keyboard2.debug.apk: Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Package juloo.keyboard2.debug signatures do not match previously installed version; ignoring!]
make: *** [Makefile:20: installd] Error 1
```

The application can't be "updated" because the temporary certificate has been
lost. The solution is to uninstall and install again.
The application must be enabled again in the settings.

```sh
adb uninstall juloo.keyboard2.debug
make installd
```

## Guidelines

### Adding a programming layout

A programming layout must contains every ASCII characters.
The current programming layouts are: QWERTY, Dvorak.

Layouts are defined in XML, see `res/xml/qwerty.xml`. An entry must be added to
the layout option in `res/values/arrays.xml`, to both `pref_layout_values`
(correspond to the file name) and `pref_layout_entries` (display name).
The layout must also be referenced in `srcs/juloo.keyboard2/Config.java` in
`layoutId_of_string`.

Keys with a name starting in `loc ` are hidden unless they are configured for
the user's installed languages in `res/xml/method.xml`. These keys are optional
and will be added automatically when necessary.

Some users cannot easily type the characters close the the edges of the screen
due to a bulky phone case. It is best to avoid placing important characters
there (such as the digits or punctuation).

### Adding a localized layout

Localized layouts (a layout specific to a language) are gladly accepted.
See for example: 4333575 (Bulgarian), 88e2175 (Latvian), 133b6ec (German).

They don't need to contain every ASCII characters (although it's useful in
passwords) and dead-keys.

### Adding support for a language

Supported locales are defined in `res/xml/method.xml`.

The attributes `languageTag` and `imeSubtypeLocale` define a locale, the
attribute `imeSubtypeExtraValue` defines the default layout and the dead-keys
and other extra keys to show. 

### Translations

Translations are always welcome !

See for example: 1723288 (Latvian), baf867a (French).
The app can be translated by writing `res/values-<language code>/strings.xml`
(for example `values-fr`, `values-lv`), based on the default:
`res/values/strings.xml` (English).

The store description is found in `metadata/android/<locale>/`,
`short_description.txt` and `full_description.txt`.
Translating changelogs is not useful. Changelogs are written quickly just
before a release and older ones are never shown to anyone currently.

The app name might be partially translated, the "unexpected" word should remain
untranslated.
