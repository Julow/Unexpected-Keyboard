# Required variables:
# 	ANDROID_PLATFORM
# Except android build tools in PATH

NAME = unexpected-keyboard

export JAVACFLAGS = -source 1.7 -target 1.7 -encoding utf8

export OCAMLFIND = ocamlfind -toolchain android
export JAVA_HOME = $(NDK_PLATFORM)/usr

BUILD_DIR ?= $(shell pwd)/bin

OCAML_JAVA_JAR := $(shell ocamlfind -toolchain android query ocamljava-jar)/ocaml-java.jar

ARCHS = armeabi-v7a

armeabi-v7a: SWITCH = 4.04.0-armeabi

all: TARGET = all
debug: TARGET = debug

all debug: $(ARCHS)
	make -f android.Makefile \
		NAME=$(NAME) ARCHS=$(ARCHS) EXTRA_JARS=$(OCAML_JAVA_JAR) \
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
