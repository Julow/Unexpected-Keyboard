# Contributing

Thanks for contributing :)

## Building the app

The application doesn't use Gradle and it might be hard to use some features of
Android Studio.

Fortunately, there's not many dependencies:
- OpenJDK 8
- Android SDK: build tools (minimum `28.0.1`), platform `30`
- Make sure to have the `$ANDROID_HOME` environment variable set.

For Nix users, there's a `shell.nix` for setting-up the right environment.

Building the debug apk:

```sh
make
```

If the build succeed, the debug apk is located in
`_build/juloo.keyboard2.debug.apk`.

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

### Add a localized layout

Localized layouts (a layout specific to a language) are generally accepted.
See for example: 4333575 (Bulgarian), 88e2175 (Latvian), 133b6ec (German).

This keyboard is intended for programmers. If your language uses the Latin script, make sure it is possible to type every letters on the QWERTY keyboard.
This is generally done using dead-keys, for example: 0bf7ff5 (Latvian), 573c13f (Swedish).
It is also possible to add some characters that are hidden in other languages, for example 93e84ba (ß), though the space is limited.

### Add a programming layout

A programming layout must contains every ASCII characters as well as every dead-keys.
Currently, the only example is QWERTY.

### Translations

Translations are always welcome ! See for example: 1723288 (Latvian), baf867a (French).
The app can be translated by writing `res/values-<language code>/strings.xml` (for example `values-fr`, `values-lv`), based on the default: `res/values/strings.xml` (English).

The store description is found in `metadata/android/<locale>/`, `short_description.txt` and `full_description.txt`.
The full description changes very infrequently (it was changed once in 6 years). But if it changes too much, outdated translation might be removed.

Translating changelogs is not useful because they evolve too fast. Changelogs are generally written entirely just before a release, translating them would delay releases too much. Old changelogs are not shown to anyone currently.
