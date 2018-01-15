# Required variables:
# 	ANDROID_PLATFORM
# Except android build tools in PATH

NAME = unexpected-keyboard

export JAVACFLAGS = -source 1.7 -target 1.7 -encoding utf8

export OCAMLFIND = ocamlfind -toolchain android

ARCHS = armeabi-v7a

armeabi-v7a: SWITCH = 4.04.0-armeabi

all: $(ARCHS)
	make -f android.Makefile \
		NAME=$(NAME) ARCHS=$(ARCHS)

$(ARCHS):
	mkdir -p bin/lib/$@
	opam switch $(SWITCH)
	eval `opam config env` && \
	make -f ocaml.Makefile NAME=bin/lib/$@/lib$(NAME).so OBJ_DIR=bin/lib/$@

install:
	adb install -r bin/$(NAME).apk

container:
	docker build -t $(NAME)-build - < Dockerfile
	docker run -it --rm -v"`pwd`:/app" $(NAME)-build bash

.PHONY: all apk $(ARCHS) container
