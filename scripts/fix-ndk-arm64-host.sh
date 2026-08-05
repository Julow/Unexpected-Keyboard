#!/usr/bin/env bash
#
# The official Android NDK only ships Linux x86_64 host toolchains and its
# ndk-build script rejects aarch64 hosts with:
#   ERROR: Unknown host CPU architecture: aarch64
#
# On aarch64 Linux hosts with qemu-user binfmt enabled, the x86_64 toolchain
# runs fine transparently. This script patches the NDK's host detection so it
# uses the linux-x86_64 prebuilt directory on aarch64 hosts.
#
# Usage: scripts/fix-ndk-arm64-host.sh [NDK_PATH]
# NDK_PATH defaults to $ANDROID_HOME/ndk (all installed versions are patched).

set -euo pipefail

SCRIPT=$0
TARGET_LINE='  aarch64) HOST_ARCH=x86_64;;'
ANCHOR='  arm64) HOST_ARCH=arm64;;'

patch_ndk() {
  local file=$1
  if ! grep -qF 'aarch64) HOST_ARCH=x86_64;;' "$file"; then
    sed -i "/$ANCHOR/a\\$TARGET_LINE" "$file"
    echo "Patched $file"
  else
    echo "Already patched: $file"
  fi
}

if [ $# -gt 0 ]; then
  for ndk in "$@"; do
    [ -f "$ndk/build/tools/ndk_bin_common.sh" ] && patch_ndk "$ndk/build/tools/ndk_bin_common.sh" \
      || echo "Skipping $ndk (not an NDK directory)"
  done
  exit 0
fi

if [ -z "${ANDROID_HOME:-}" ]; then
  echo "ERROR: set ANDROID_HOME or pass an NDK path." >&2
  exit 1
fi

if [ -d "$ANDROID_HOME/ndk" ]; then
  shopt -s nullglob
  for ndk in "$ANDROID_HOME"/ndk/*/; do
    patch_ndk "$ndk/build/tools/ndk_bin_common.sh"
  done
fi
