# Required variables:
# 	ANDROID_PLATFORM
# Except android build tools in PATH

NAME = unexpected-keyboard

export JAVACFLAGS = -source 1.7 -target 1.7 -encoding utf8

export OCAMLFIND = ocamlfind -toolchain android
export JAVA_HOME = $(NDK_PLATFORM)/usr

BUILD_DIR ?= $(PWD)/bin

OCAML_JAVA_JAR = $(BUILD_DIR)/libs/ocaml-java/ocaml-java.jar

ARCHS = armeabi-v7a

armeabi-v7a: SWITCH = 4.04.0-armeabi

all: TARGET = all
debug: TARGET = debug

all debug: $(ARCHS) $(OCAML_JAVA_JAR)
	make -f android.Makefile \
		NAME=$(NAME) ARCHS=$(ARCHS) EXTRA_JARS=$(OCAML_JAVA_JAR) \
		BIN_DIR=$(BUILD_DIR) $(TARGET)

$(ARCHS):
	mkdir -p $(BUILD_DIR)/lib/$@
	opam switch $(SWITCH); \
	eval `opam config env` && \
	OCAML_JAVA="$(BUILD_DIR)/lib/$@/libs/ocaml-java"; \
	make -C libs/ocaml-java \
		BUILD_DIR="$${OCAML_JAVA}/bin" \
		TARGET_DIR="$${OCAML_JAVA}" \
		OCAMLFIND="$(OCAMLFIND)" \
		JAVA_INCLUDES="-I $(JAVA_HOME)/include" \
		$${OCAML_JAVA}/javacaml.cmxa \
		&& \
	make -C libs/ocaml-java \
		BUILD_DIR="$${OCAML_JAVA}/bin" \
		TARGET_DIR="$${OCAML_JAVA}" \
		$${OCAML_JAVA}/ocaml-java-ppx \
		&& \
	make -f ocaml.Makefile \
		NAME="$(BUILD_DIR)/lib/$@/lib$(NAME).so" \
		OBJ_DIR="$(BUILD_DIR)/lib/$@" \
		OCAMLJAVA_DIR="$${OCAML_JAVA}" all

$(OCAML_JAVA_JAR):
	make -C libs/ocaml-java \
		BUILD_DIR=$(@D)/bin TARGET_DIR=$(@D) \
		$@

install:
	adb install -r $(BUILD_DIR)/$(NAME).apk

installd:
	adb install -r $(BUILD_DIR)/$(NAME).debug.apk

.PHONY: all debug apk $(ARCHS) install installd
