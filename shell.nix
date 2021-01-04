{ pkgs ? import <nixpkgs> {
    config.android_sdk.accept_license = true;
    config.allowUnfree = true;
  } }:

let

  android = pkgs.androidenv.composeAndroidPackages {
    platformVersions = [ "29" ];
    abiVersions = [ "armeabi-v7a" ];
  };

in

pkgs.mkShell {
  buildInputs = with pkgs; [
    ant openjdk8 android.androidsdk
  ];
  shellHook = ''
    export ANDROID_HOME=${android.androidsdk}/libexec/android-sdk
  '';
}
