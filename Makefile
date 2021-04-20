# Configuration

PACKAGE_NAME = juloo.keyboard2

ANDROID_PLATFORM_VERSION = android-29
JAVA_VERSION = 1.7

SRC_DIR = srcs
ASSETS_DIR = assets
RES_DIR = res

EXTRA_JARS =

# /

debug: _build/$(PACKAGE_NAME).debug.apk
release: _build/$(PACKAGE_NAME).apk

installd: _build/$(PACKAGE_NAME).debug.apk
	adb install "$<"

.PHONY: release debug installd

$(shell mkdir -p _build)

ifndef ANDROID_HOME
$(error ANDROID_HOME not set)
endif

ANDROID_BUILD_TOOLS = $(lastword $(sort $(wildcard $(ANDROID_HOME)/build-tools/*)))
ANDROID_PLATFORM = $(ANDROID_HOME)/platforms/$(ANDROID_PLATFORM_VERSION)

ifeq ($(shell [ -d "$(ANDROID_PLATFORM)" ] && echo ok),)
$(error Android platform not found. Want $(ANDROID_PLATFORM_VERSION), \
	found $(notdir $(wildcard $(ANDROID_HOME)/platforms/*)))
endif

JAVAC_FLAGS = -source $(JAVA_VERSION) -target $(JAVA_VERSION) -encoding utf8

# Source files

MANIFEST_FILE = AndroidManifest.xml
JAVA_FILES = $(shell find $(SRC_DIR) -name '*.java')
RES_FILES = $(shell find $(RES_DIR) -type f)
ASSETS_FILES = $(shell find $(ASSETS_DIR) -type f 2>/dev/null)

# Align

_build/%.apk: _build/%.signed-apk
	$(ANDROID_BUILD_TOOLS)/zipalign -fp 4 "$<" "$@"

# Debug signing

DEBUG_KEYSTORE = _build/debug.keystore
DEBUG_PASSWD = debug0

$(DEBUG_KEYSTORE):
	echo y | keytool -genkeypair -dname "cn=d, ou=e, o=b, c=ug" \
		-alias debug -keypass $(DEBUG_PASSWD) -keystore "$@" \
		-keyalg rsa -storepass $(DEBUG_PASSWD) -validity 10000

_build/%.debug.signed-apk: _build/%.debug.unsigned-apk $(DEBUG_KEYSTORE)
	jarsigner -keystore $(DEBUG_KEYSTORE) \
		-storepass $(DEBUG_PASSWD) -keypass $(DEBUG_PASSWD) \
		-signedjar "$@" "$<" debug

# Debug apk

_build/$(PACKAGE_NAME).debug.unsigned-apk: AAPT_PACKAGE_FLAGS+=--rename-manifest-package $(PACKAGE_NAME).debug

# Release signing

# %-keystore.conf should declare KEYSTORE, KEYNAME and OPTS
#  it is interpreted as a shell script
# OPTS can be used to pass -storepass or -keypass options to jarsigner
_build/%.signed-apk: _build/%.unsigned-apk %-keystore.conf
	eval `cat $(word 2,$^)` && \
	jarsigner -sigalg SHA1withRSA -digestalg SHA1 -keystore "$$KEYSTORE" $$OPTS -signedjar "$@" "$<" "$$KEYNAME"

# Package

_build/%.unsigned-apk: _build/classes.dex $(MANIFEST_FILE) $(ASSETS_FILES)
	$(ANDROID_BUILD_TOOLS)/aapt package -f -M $(MANIFEST_FILE) -S $(RES_DIR) \
		-I $(ANDROID_PLATFORM)/android.jar -F "$@" $(AAPT_PACKAGE_FLAGS)
	[ -z "$(ASSETS_FILES)" ] || $(ANDROID_BUILD_TOOLS)/aapt add "$@" $(ASSETS_FILES)
	cd $(@D) && $(ANDROID_BUILD_TOOLS)/aapt add $(@F) classes.dex

# R.java

GEN_DIR = _build/gen
R_FILE = $(GEN_DIR)/$(subst .,/,$(PACKAGE_NAME))/R.java

$(R_FILE): $(RES_FILES) $(MANIFEST_FILE)
	mkdir -p "$(@D)"
	$(ANDROID_BUILD_TOOLS)/aapt package -f -m -S $(RES_DIR) -J $(GEN_DIR) \
		-M $(MANIFEST_FILE) -I $(ANDROID_PLATFORM)/android.jar

# Compile java classes and build classes.dex

OBJ_DIR = _build/class
# A$B.class files are ignored
# CLASS_FILES = $(JAVA_FILES:$(SRC_DIR)/%.java=$(OBJ_DIR)/%.class) \
# 	$(R_FILE:$(GEN_DIR)/%.java=$(OBJ_DIR)/%.class)

_build/classes.dex: $(JAVA_FILES) $(R_FILE)
	mkdir -p $(OBJ_DIR)
	javac -d $(OBJ_DIR) $(JAVAC_FLAGS) \
		-classpath $(ANDROID_PLATFORM)/android.jar:$(EXTRA_JARS) \
		-sourcepath $(SRC_DIR):$(GEN_DIR) \
		$^
	$(ANDROID_BUILD_TOOLS)/dx --dex --output="$@" $(OBJ_DIR) $(subst :, ,$(EXTRA_JARS))
