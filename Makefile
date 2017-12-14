# Required variables:
# 	ANDROID_PLATFORM
# Except android build tools in PATH

NAME = unexpected-keyboard

all:
	make $(LIBS)
	make apk

apk:
	make -f android.Makefile \
		NAME=$(NAME) LIBS=$(LIBS)

LIBS = armeabi-v7a

armeabi-v7a: SWITCH = 4.04.0-armeabi

$(LIBS):
	mkdir -p bin/lib/$@
	opam switch $(SWITCH)
	eval `opam config env`; \
	make -f ocaml.Makefile NAME=bin/lib/$@/lib$(NAME).so OBJ_DIR=bin/lib/$@

install:
	adb install -r bin/$(NAME).apk

container:
	docker build -t $(NAME)-build - < Dockerfile
	docker run -it --rm -v"`pwd`:/app" $(NAME)-build bash

.PHONY: all apk $(LIBS) container
