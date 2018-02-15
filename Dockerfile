FROM ocaml/ocaml:debian-stable

RUN apt-get update

# ============================================================================ #
# Android sdk

RUN apt-get install -y android-sdk

ENV ANDROID_HOME "/usr/lib/android-sdk/"
ENV ANDROID_BUILD_TOOLS "${ANDROID_HOME}/build-tools/24.0.0"
ENV ANDROID_PLATFORM "${ANDROID_HOME}/platforms/android-23"

ENV PATH "${PATH}:${ANDROID_BUILD_TOOLS}"

# ============================================================================ #
# OPAM

RUN apt-get install -y opam aspcud
RUN echo y | opam init

# ============================================================================ #
# opam-cross-android

RUN sudo apt update && sudo apt install -y gcc-multilib

RUN opam repository add android git://github.com/ocaml-cross/opam-cross-android
RUN opam update

ARG OCAML_VERSION=4.04.0

RUN opam switch install ${OCAML_VERSION}-armeabi --alias-of ${OCAML_VERSION}+32bit

RUN opam pin add ocamlbuild https://github.com/ocaml/ocamlbuild.git && \
	opam pin add topkg https://github.com/whitequark/topkg.git

RUN env ARCH=arm SUBARCH=armv7 SYSTEM=linux_eabi \
	CCARCH=arm TOOLCHAIN=arm-linux-androideabi-4.9 \
	TRIPLE=arm-linux-androideabi LEVEL=24 \
	STLVER=4.9 STLARCH=armeabi opam install -y conf-android

RUN opam install -y ocaml-android

ENV NDK_HOME=/root/.opam/4.04.0-armeabi/android-ndk
ENV NDK_PLATFORM=${NDK_HOME}/platforms/android-23/arch-arm/

# ============================================================================ #
# Some deps

RUN opam install -y ppx_tools

# ============================================================================ #

RUN mkdir /app
WORKDIR /app

CMD make
