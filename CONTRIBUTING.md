# Contributing

Thanks for contributing :)

## Building the app

The application doesn't use Gradle and it might be hard to use some features of
Android Studio.

Fortunately, there's not many dependencies:
- OpenJDK 8
- Android SDK: build tools `30.0.3`, platform `30`
- Make sure to have the `$ANDROID_HOME` environment variable set.

For Nix users, there's a `shell.nix` for setting-up the right environment.

Building the debug apk:

```sh
make
```

If the build succeed, the debug apk is located in
`_build/juloo.keyboard2.debug.apk`.

## Debugging on your phone

You need to have *USB debugging* enabled on your phone, see [Android's doc](https://developer.android.com/studio/debug/dev-options#enable).

It is also possible to enable *ADB over network* in the settings and connect
with `adb connect <ip of the phone>`. Make sure to be connected to a local
network that you trust.

Install the application with:

```sh
make installd
```

The debug version of the application won't be removed, both versions will stay
installed at the same time.

The application must be enabled in the settings:
System Settings > System > Languages & Input > Virtual keyboard > Manage keyboards.

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
