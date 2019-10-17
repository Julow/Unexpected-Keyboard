#!/usr/bin/env bash
DISTRO=${DISTRO:-alpine}
OCAML_VERSIONS=${OCAML_VERSIONS:-4.04 4.05 4.06 4.07}
TEST_DISTRIB=${TEST_DISTRIB:-no}
export OPAMYES=1
export OPAMJOBS=3
set -ex
case $AGENT_OS in
Linux)
  sudo add-apt-repository ppa:avsm/ppa
  sudo apt-get update
  sudo apt-get -y install opam
  opam init --auto-setup https://github.com/ocaml/opam-repository.git
  opam install depext
  git config --global user.name "Azure Pipelines CI"
  git config --global user.email "bactrian@ocaml.org"
  TEST_CURRENT_SWITCH_ONLY=yes
  eval $(opam env)
;; 
LinuxDocker) 
  sudo chown -R opam /home/opam/src
  cd /home/opam/src
  git -C /home/opam/opam-repository pull origin master
  opam update
  opam install depext
  eval $(opam env)
  ;;
Windows_NT)
  echo Preparing Cygwin environment
  SWITCH=${OPAM_SWITCH:-'4.07.1+mingw64c'}
  OPAM_DL_SUB_LINK=0.0.0.2
  case $AGENT_OSARCHITECTURE in
  "X64")
    OPAM_URL="https://github.com/fdopen/opam-repository-mingw/releases/download/${OPAM_DL_SUB_LINK}/opam64.tar.xz"
    OPAM_ARCH=opam64 ;;
  "X86")
    OPAM_URL="https://github.com/fdopen/opam-repository-mingw/releases/download/${OPAM_DL_SUB_LINK}/opam32.tar.xz"
    OPAM_ARCH=opam32 ;;
  *)
    echo "Unsupported architecture $AGENT_OSARCHITECTURE"
    exit 1 ;;
  esac
  if [ $# -gt 0 ] && [ -n "$1" ]; then
    SWITCH=$1
  fi
  export OPAM_LINT="false"
  export CYGWIN='winsymlinks:native'
  export OPAMYES=1
  set -eu
  curl -fsSL -o "${OPAM_ARCH}.tar.xz" "${OPAM_URL}"
  tar -xf "${OPAM_ARCH}.tar.xz"
  "${OPAM_ARCH}/install.sh" --quiet
  # if a msvc compiler must be compiled from source, we have to modify the
  # environment first
  case "$SWITCH" in
    *msvc32)
      eval $(ocaml-env cygwin --ms=vs2015 --no-opam --32) ;;
    *msvc64)
      eval $(ocaml-env cygwin --ms=vs2015 --no-opam --64) ;;
  esac
  opam init -c "ocaml-variants.${SWITCH}" --disable-sandboxing --enable-completion --enable-shell-hook --auto-setup default "https://github.com/fdopen/opam-repository-mingw.git#opam2"
  opam config set jobs "$OPAMJOBS"
  opam update
  is_msvc=0
  case "$SWITCH" in
    *msvc*)
        is_msvc=1
        eval $(ocaml-env cygwin --ms=vs2015)
        ;;
    *mingw*)
        eval $(ocaml-env cygwin)
        ;;
    *)
        echo "ocamlc reports a dubious system: ${ocaml_system}. Good luck!" >&2
        eval $(opam env)
  esac
  if [ $is_msvc -eq 0 ]; then
    opam install depext-cygwinports depext
  else
    opam install depext
  fi
  cd ${BUILD_SOURCES_DIR}
  git config --global user.name "Azure Pipelines CI"
  git config --global user.email "bactrian@ocaml.org"
  TEST_CURRENT_SWITCH_ONLY=yes
  ;;
Darwin)
  brew install opam ocaml
  opam init --auto-setup https://github.com/ocaml/opam-repository.git
  opam install depext
  git config --global user.name "Azure Pipelines CI"
  git config --global user.email "bactrian@ocaml.org"
  TEST_CURRENT_SWITCH_ONLY=yes
  TEST_DISTRIB=yes
  eval $(opam env)
  ;;
*)
  echo Unknown OS $AGENT_OS
  exit 1
esac

echo debug git
git show -s --format=%ct

PACKAGES=`ls -1 *.opam|xargs -I% -n 1 basename % .opam`
echo Processing $PACKAGES
# git pins fails under cygwin, need to debug
opam pin add -n -k path . 
opam --yes depext -y $PACKAGES
opam install --with-test --with-doc --deps-only .
case $TEST_CURRENT_SWITCH_ONLY in
yes)
  echo Testing current switch only
  opam switch
  dune build
  dune runtest
  ;;
*)
  echo "(lang dune 1.0)" > dune-workspace.dev
  for v in $OCAML_VERSIONS; do
    echo "(context (opam (switch $v)))" >> dune-workspace.dev
    opam install --deps-only -t --switch $v .
  done

  dune build --workspace dune-workspace.dev
  dune runtest --workspace dune-workspace.dev
  ;;
esac

if [ "$TEST_DISTRIB" = "yes" ]; then
  opam install -y dune-release odoc
  dune build @doc
  dune-release distrib
  dune runtest --force
fi
