{ pkgs ? import <nixpkgs> {
  config.android_sdk.accept_license = true;
  config.allowUnfree = true;
} }:

let
  jdk = pkgs.openjdk17;
  build_tools_version = "33.0.1";

  android = pkgs.androidenv.composeAndroidPackages {
    buildToolsVersions = [ build_tools_version ];
    platformVersions = [ "34" ];
    abiVersions = [ "armeabi-v7a" ];
  };

  ANDROID_SDK_ROOT = "${android.androidsdk}/libexec/android-sdk";

  gradle = pkgs.gradle.override { java = jdk; };
  # Without this option, aapt2 fails to run with a permissions error.
  gradle_wrapped = pkgs.runCommandLocal "gradle-wrapped" {
    nativeBuildInputs = with pkgs; [ makeBinaryWrapper ];
  } ''
    mkdir -p $out/bin
    ln -s ${gradle}/bin/gradle $out/bin/gradle
    wrapProgram $out/bin/gradle \
    --add-flags "-Dorg.gradle.project.android.aapt2FromMavenOverride=${ANDROID_SDK_ROOT}/build-tools/${build_tools_version}/aapt2"
  '';

in pkgs.mkShell {
  buildInputs =
    [ pkgs.findutils pkgs.fontforge jdk android.androidsdk gradle_wrapped ];
  JAVA_HOME = jdk.home;
  inherit ANDROID_SDK_ROOT;
}
