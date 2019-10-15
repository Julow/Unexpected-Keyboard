export NDK_HOME = $HOME/.opam/4.04.0-armeabi/android-ndk
export NDK_PLATFORM = $(NDK_HOME)/platforms/android-24/arch-arm/

export ANDROID_HOME = /opt/android-sdk/
export ANDROID_BUILD_TOOLS = $(ANDROID_HOME)/build-tools/27.0.3
export ANDROID_PLATFORM = $(ANDROID_HOME)/platforms/android-27

NAME = unexpected-keyboard

export JAVACFLAGS = -source 1.7 -target 1.7 -encoding utf8

export OCAMLFIND = ocamlfind -toolchain android
export JAVA_HOME = $(NDK_PLATFORM)/usr

BUILD_DIR ?= $(shell pwd)/bin

OCAML_QUERY = ocamlfind -toolchain android query

EXTRA_JARS := \
	$(shell $(OCAML_QUERY) ocamljava-jar)/ocaml-java.jar

ARCHS = armeabi-v7a

armeabi-v7a: SWITCH = 4.04.0-android-arm

all: TARGET = all
debug: TARGET = debug

all debug: $(ARCHS)
	make -f android.Makefile \
		NAME=$(NAME) ARCHS=$(ARCHS) EXTRA_JARS=$(EXTRA_JARS) \
		BIN_DIR=$(BUILD_DIR) $(TARGET)

$(ARCHS):
	mkdir -p $(BUILD_DIR)/lib/$@
	set -e; \
	opam switch $(SWITCH); \
	eval `opam config env`; \
	cd srcs; \
	TARGET=_build/default.android/main/main.so; \
	jbuilder build -x android "$$TARGET"; \
	cp "$$TARGET" "$(BUILD_DIR)/lib/$@/lib$(NAME).so"

install:
	adb install -r $(BUILD_DIR)/$(NAME).apk

installd:
	adb install -r $(BUILD_DIR)/$(NAME).debug.apk

.PHONY: all debug apk $(ARCHS) install installd
