# Configuration

PACKAGE_NAME = juloo.keyboard2

ANDROID_PLATFORM_VERSION = android-30
JAVA_VERSION = 1.8

SRC_DIR = srcs
RES_DIR = res

EXTRA_JARS =

# /

debug: _build/$(PACKAGE_NAME).debug.apk
release: _build/$(PACKAGE_NAME).apk

installd: _build/$(PACKAGE_NAME).debug.apk
	adb install "$<"

clean:
	rm -rf _build/*.dex _build/class _build/gen _build/*.apk _build/*.unsigned-apk \
		_build/*.idsig _build/assets

.PHONY: release debug installd clean

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

# Debug signing

DEBUG_KEYSTORE = _build/debug.keystore
DEBUG_PASSWD = debug0

$(DEBUG_KEYSTORE):
	echo y | keytool -genkeypair -dname "cn=d, ou=e, o=b, c=ug" \
		-alias debug -keypass $(DEBUG_PASSWD) -keystore "$@" \
		-keyalg rsa -storepass $(DEBUG_PASSWD) -validity 10000

_build/%.debug.apk: _build/%.debug.unsigned-apk $(DEBUG_KEYSTORE)
	$(ANDROID_BUILD_TOOLS)/apksigner sign --in "$<" --out "$@" \
		--ks $(DEBUG_KEYSTORE) --ks-key-alias debug --ks-pass "pass:$(DEBUG_PASSWD)"

# Debug apk

_build/$(PACKAGE_NAME).debug.unsigned-apk: AAPT_PACKAGE_FLAGS+=--rename-manifest-package $(PACKAGE_NAME).debug --product debug

# Release signing

# %-keystore.conf should declare KEYSTORE, KEYNAME and KEYSTOREPASS
#  it is interpreted as a shell script
_build/%.apk: _build/%.unsigned-apk %-keystore.conf
	eval `cat $(word 2,$^)` && \
	$(ANDROID_BUILD_TOOLS)/apksigner sign --in "$<" --out "$@" \
		--ks "$$KEYSTORE" --ks-key-alias "$$KEYNAME" --ks-pass "pass:$$KEYSTOREPASS"

# Package

_build/%.unsigned-apk: _build/%.unaligned-apk
	$(ANDROID_BUILD_TOOLS)/zipalign -fp 4 "$<" "$@"

APK_EXTRA_FILES = classes.dex assets/special_font.ttf

_build/%.unaligned-apk: $(addprefix _build/,$(APK_EXTRA_FILES)) $(MANIFEST_FILE)
	$(ANDROID_BUILD_TOOLS)/aapt package -f -M $(MANIFEST_FILE) -S $(RES_DIR) \
		-I $(ANDROID_PLATFORM)/android.jar -F "$@" $(AAPT_PACKAGE_FLAGS)
	cd $(@D) && $(ANDROID_BUILD_TOOLS)/aapt add $(@F) $(APK_EXTRA_FILES)

# R.java

GEN_DIR = _build/gen
R_FILE = $(GEN_DIR)/$(subst .,/,$(PACKAGE_NAME))/R.java

$(R_FILE): $(RES_FILES) $(MANIFEST_FILE)
	mkdir -p "$(@D)"
	$(ANDROID_BUILD_TOOLS)/aapt package -f -m -S $(RES_DIR) -J $(GEN_DIR) \
		-M $(MANIFEST_FILE) -I $(ANDROID_PLATFORM)/android.jar

# Special font

SPECIAL_FONT_GLYPHS = $(wildcard srcs/special_font/*.svg)
SPECIAL_FONT_SCRIPT = srcs/special_font/build.pe

_build/assets/special_font.ttf: $(SPECIAL_FONT_SCRIPT) $(SPECIAL_FONT_GLYPHS)
	mkdir -p $(@D)
	fontforge -lang=ff -script $(SPECIAL_FONT_SCRIPT) $@ $(SPECIAL_FONT_GLYPHS)

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
	$(ANDROID_BUILD_TOOLS)/d8 --output $(@D) $(OBJ_DIR)/*/*/* $(subst :, ,$(EXTRA_JARS))
