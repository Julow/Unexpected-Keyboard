{ pkgs ? import <nixpkgs> {
    config.android_sdk.accept_license = true;
    config.allowUnfree = true;
  } }:

let
  jdk = pkgs.openjdk8;

  android = pkgs.androidenv.composeAndroidPackages {
    buildToolsVersions = [ "30.0.3" ];
    platformVersions = [ "30" ];
    abiVersions = [ "armeabi-v7a" ];
  };

  apksigner = pkgs.apksigner.override {
    inherit (jdk) jre;
    inherit (android) build-tools;
  };

in pkgs.mkShell {
  buildInputs = [ pkgs.findutils jdk android.androidsdk apksigner ];
  ANDROID_HOME = "${android.androidsdk}/libexec/android-sdk";
}
