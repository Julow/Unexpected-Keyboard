{ pkgs ? import <nixpkgs> {
  config.android_sdk.accept_license = true;
  config.allowUnfree = true;
} }:

let
  android = pkgs.androidenv.composeAndroidPackages {
    buildToolsVersions = [ "30.0.3" ];
    platformVersions = [ "30" ];
    abiVersions = [ "armeabi-v7a" ];
  };

  buildInputs =
    [ pkgs.findutils pkgs.openjdk8 android.androidsdk pkgs.fontforge ];

  # Env variable required by the Makefile
  ANDROID_HOME = "${android.androidsdk}/libexec/android-sdk";

  # Build the debug APK. Exposed as an attribute, used in CI
  debug-apk = pkgs.stdenv.mkDerivation {
    name = "unexpected-keyboard-debug";
    src = ./.;
    inherit buildInputs ANDROID_HOME;
    buildPhase = ''
      make
    '';
    installPhase = ''
      mkdir -p $out
      mv _build/*.apk $out
    '';
  };

in pkgs.mkShell { inherit buildInputs ANDROID_HOME; } // {
  inherit debug-apk;
}
