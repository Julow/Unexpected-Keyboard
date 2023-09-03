# Contributing

Thanks for contributing :)

## Building the app

The application uses Gradle and can be used with Android Studio, but using Android Studio is not required and the project can be build and operated by just using Gradle.

Fortunately, there are not many dependencies:
- OpenJDK 17
- Python 3
- Android SDK: build tools (minimum `28.0.1`), platform `30`

If you don't use Android Studio and your ANDROID_HOME environment variable is __NOT__ set, you have to inform Gradle about the location of your Android SDK:
Create the file `local.properties` in the directory of this repository and paste `sdk.dir=<location_of_android_home>` e.g. `sdk.dir=/home/user/Android/Sdk`.

If your `ANDROID_HOME` variable is set (output of `echo $ANDROID_HOME` is not empty) or you use Android Studio, you don't have to do anything.

For Nix users, the right environment can be obtained with `nix-shell ./shell.nix`.
Instructions to install Nix are [here](https://nixos.wiki/wiki/Nix_Installation_Guide).

Building the debug apk:

```sh
./gradlew assembleDebug
```

If the build succeeds, the debug apk is located in `build/outputs/apk/debug/app-debug.apk`.

## Using the local debug.keystore on the Github CI actions

It's possible to save the local debug.keystore into a github secret, so the same keystore is utilized to build the debug apk in the CI github actions.
Doing this, they wil have the same signature, thus the debug apk can be updated without having to uninstall it first.

After you successfully run `./gradlew asssembleDebug`, (thus a debug.keystore exists) you can use this second command to generate a base64 stringified version of it.

```sh
gpg -c --armor --pinentry-mode loopback --passphrase debug0 --yes "debug.keystore"
```

A file will be generated inside the project folder, called `debug.keystore.asc`

You can copy the content of this file, and with that, paste it into a new github secret in your repo settings. The secret must be named `DEBUG_KEYSTORE`.

## Debugging on your phone

First [Enable adb debugging on your device](https://developer.android.com/studio/command-line/adb#Enabling).
Then connect your phone to your computer using an USB cable or wireless
debugging.

(If you use Android Studio, this process will be automatic and you don't have to follow this guide anymore)

And finally, install the application with:
```sh
./gradlew installDebug
```

The released version of the application won't be removed, both versions will
be installed at the same time.

## Debugging the application: INSTALL_FAILED_UPDATE_INCOMPATIBLE

`./gradlew installDebug` can fail with the following error message:

```
Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Package juloo.keyboard2.debug signatures do not match previously installed version; ignoring!]
```

The application can't be "updated" because the temporary certificate has been
lost. The solution is to uninstall and install again.
The application must be enabled again in the settings.

```sh
adb uninstall juloo.keyboard2.debug
./gradlew installDebug
```

## Guidelines

### Adding a layout

Layouts are defined in XML, see `src/main/res/xml/latn_qwerty_us.xml`.
An online tool for editing layout files written by @Lixquid is available
[here](https://unexpected-keyboard-layout-editor.lixquid.com/).

Makes sure to specify the `name` attribute like in `latn_qwerty_us.xml`,
otherwise the layout won't be added to the app.

The layout file must be placed in the `src/main/res/xml/` directory and named according to:
- script (`latn` for latin, etc..)
- layout name (eg. the name of a standard)
- country code (or language code if more adequate)

Then, run `./gradlew genLayoutsList` to add the layout to the app.

The last step will update the file `src/main/res/values/layouts.xml`, that you should
not edit directly.

Run `./gradlew checkKeyboardLayouts` to check some properties about your layout. This will
change the file `check_layout.output`, which you should commit.

#### Adding a programming layout

A programming layout must contain all ASCII characters.
The current programming layouts are: QWERTY, Dvorak and Colemak.

See for example, Dvorak, added in https://github.com/Julow/Unexpected-Keyboard/pull/16

It's best to leave free spots on the layout for language-specific symbols that
are added automatically when necessary.
These symbols are defined in `src/main/res/xml/method.xml` (`extra_keys`).

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

Supported locales are defined in `src/main/res/xml/method.xml`.

The attributes `languageTag` and `imeSubtypeLocale` define a locale, the
attribute `imeSubtypeExtraValue` defines the default layout and the dead-keys
and other extra keys to show.

The list of language tags (generally two letters)
and locales (generally of the form `xx_XX`)
can be found in this [stackoverflow answer](https://stackoverflow.com/a/7989085)

### Updating translations

The text used in the app is written in `src/main/res/values-<language_tag>/strings.xml`.

The list of language tags can be found in this
[stackoverflow answer](https://stackoverflow.com/a/7989085)

The first part before the `_` is used, for example,
`src/main/res/values-fr/strings.xml` for French,
`src/main/res/values-lv/strings.xml` for Latvian.

Commented-out lines indicate missing translations:

```xml
  <!-- <string name="pref_layouts_add">Add an alternate layout</string> -->
```

Remove the `<!--` and `-->` parts and change the text.

### Adding a translation

The `src/main/res/values-<language_tag>/strings.xml` file must be created by copying the
default translation in `src/main/res/values/strings.xml`, which contain the structure of
the file and the English strings.

To check that `strings.xml` is formatted correctly, run
`./gradlew syncTranslations`. This will modify your files.

The store description is found in `metadata/android/<locale>/`,
`short_description.txt` and `full_description.txt`.
The short description must not exceed 80 characters.
Translating changelogs is not useful.

The app name might be partially translated, the "Unexpected" word should remain
untranslated.

As translations need to be updated regularly, you can subscribe to this issue
to receive a notification when an update is needed:
https://github.com/Julow/Unexpected-Keyboard/issues/373

### Adding key combinations

Key combinations are defined in `src/main/java/juloo.keyboard2/KeyModifier.java`.
For example, keys modified by the `Fn` key are defined in method
`apply_fn_char`.

Keys with special meaning are defined in `KeyValue.java` in method
`getKeyByName`. Their special action are defined in `KeyEventHandler.java` in
method `key_up`
