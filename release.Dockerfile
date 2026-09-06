# This docker image is used to build the release APK in a reproducible
# environment.
# mkdir -p release
# docker build -t unexpected-keyboard-release -f release.Dockerfile .
# docker run -v "$PWD/release:/release" \
# -e RELEASE_KEYSTORE -e RELEASE_KEY_ALIAS -e RELEASE_KEYSTORE_PASSWORD \
# -e RELEASE_KEY_PASSWORD unexpected-keyboard-release

FROM gradle:8.14-jdk17

ENV ANDROID_SDK_ROOT=/home/gradle/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

RUN apt-get update && apt-get install -y --no-install-recommends wget unzip \
    && mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools" \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip \
        -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_SDK_ROOT/cmdline-tools" \
    && mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"

RUN yes | sdkmanager --licenses
RUN sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

WORKDIR /w
COPY --chown=gradle:gradle . .
RUN rm -rf build .gradle .idea

CMD sh -c 'gradle clean assembleRelease && \
  cp build/outputs/apk/release/Unexpected-Keyboard-release.apk /release'
