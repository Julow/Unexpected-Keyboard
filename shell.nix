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
    inherit repoJson;
  };

  # Ensure we have the needed system images
  repoJson = pkgs.fetchurl {
    url =
      "https://raw.githubusercontent.com/NixOS/nixpkgs/ebc7402410a3ce2d25622137c190d4ab83945c10/pkgs/development/mobile/androidenv/repo.json";
    hash = "sha256-4/0FMyxM+7d66qfhlY3A10RIe6j6VrW8DIilH2eQyzc=";
  };

  emulators = let
    mk_emulator = { platformVersion, device ? "pixel_6" }:
      pkgs.androidenv.emulateApp rec {
        name = "emulator_api${platformVersion}";
        inherit platformVersion;
        abiVersion = "x86_64";
        androidAvdFlags = "--device ${device}";
        # There's no 'default' image for Android 15
        systemImageType = "google_apis";
        sdkExtraArgs = { inherit repoJson; };
      };
    # Allow to install several emulators in the same environment
    link_emulator = version_name: args: {
      name = "bin/emulate_android_${version_name}";
      path = "${mk_emulator args}/bin/run-test-emulator";
    };
  in pkgs.linkFarm "emulator" [
    (link_emulator "14" { platformVersion = "34"; })
    (link_emulator "15" { platformVersion = "35"; })
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
