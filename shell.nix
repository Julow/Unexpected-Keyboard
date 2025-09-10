{ pkgs ? import <nixpkgs> {
  config.android_sdk.accept_license = true;
  config.allowUnfree = true;
} }:

let
  jdk = pkgs.openjdk17;
  build_tools_version = "34.0.0";

  android = pkgs.androidenv.composeAndroidPackages {
    buildToolsVersions = [ build_tools_version ];
    platformVersions = [ "35" ];
    abiVersions = [ "armeabi-v7a" ];
  };

  emulators = let
    mk_emulator = { platformVersion, device ? "pixel_6", abiVersion ? "x86_64", systemImageType ? "default" }:
      pkgs.androidenv.emulateApp rec {
        name = "emulator_api${platformVersion}";
        inherit platformVersion abiVersion systemImageType;
        androidAvdFlags = "--device ${device}";
      };
    # Allow to install several emulators in the same environment
    link_emulator = version_name: args: {
      name = "bin/emulate_android_${version_name}";
      path = "${mk_emulator args}/bin/run-test-emulator";
    };
  in pkgs.linkFarm "emulator" [
    (link_emulator "5" { platformVersion = "21"; })
    # (link_emulator "14" { platformVersion = "34"; })
    # There's no 'default' image for Android 15
    (link_emulator "15" { platformVersion = "35"; systemImageType = "google_apis"; })
  ];

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
  buildInputs = [
    pkgs.findutils
    pkgs.fontforge
    jdk
    android.androidsdk
    gradle_wrapped
    emulators
  ];
  JAVA_HOME = jdk.home;
  inherit ANDROID_SDK_ROOT;
}
