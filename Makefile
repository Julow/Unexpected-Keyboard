# Required variables:
# 	ANDROID_PLATFORM
# Except android build tools in PATH

NAME = unexpected-keyboard

export JAVACFLAGS = -source 1.7 -target 1.7 -encoding utf8

export OCAMLFIND = ocamlfind -toolchain android
export JAVA_HOME = $(NDK_PLATFORM)/usr

OCAML_JAVA_JAR = $(PWD)/bin/libs/ocaml-java/ocaml-java.jar

ARCHS = armeabi-v7a

armeabi-v7a: SWITCH = 4.04.0-armeabi

all: $(ARCHS) $(OCAML_JAVA_JAR)
	make -f android.Makefile \
		NAME=$(NAME) ARCHS=$(ARCHS) EXTRA_JARS=$(OCAML_JAVA_JAR)

$(ARCHS):
	mkdir -p bin/lib/$@
	opam switch $(SWITCH); \
	eval `opam config env` && \
	OCAML_JAVA="$${PWD}/bin/lib/$@/libs/ocaml-java"; \
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
		NAME="bin/lib/$@/lib$(NAME).so" \
		OBJ_DIR="bin/lib/$@" \
		OCAMLJAVA_DIR="$${OCAML_JAVA}" all

$(OCAML_JAVA_JAR):
	make -C libs/ocaml-java \
		BUILD_DIR=$(@D)/bin TARGET_DIR=$(@D) \
		$@

install:
	adb install -r bin/$(NAME).apk

container:
	docker build -t $(NAME)-build - < Dockerfile
	docker run -it --rm -v"`pwd`:/app" $(NAME)-build bash

.PHONY: all apk $(ARCHS) container
