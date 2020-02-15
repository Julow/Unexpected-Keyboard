# Required variables:
# 	NAME
# 	ARCHS		Libraries architectures: bin/lib/<arch>/lib$(NAME).so
# 	EXTRA_JARS	Path to libraries jar files, separated by `:`

BIN_DIR = bin
OBJ_DIR = $(BIN_DIR)/objs
GEN_DIR = $(BIN_DIR)/gen
RES_DIR = res
SRC_DIR = srcs
ASSETS_DIR = assets

ANDROID_FRAMEWORK = $(ANDROID_PLATFORM)/android.jar
MANIFEST_FILE = AndroidManifest.xml

ASSETS_FILES = $(shell find $(ASSETS_DIR) -type f 2>/dev/null)
LIBS_FILES = $(foreach a,$(ARCHS),$(BIN_DIR)/lib/$(a)/lib$(NAME).so)

AAPT = $(ANDROID_BUILD_TOOLS)/aapt
DX = $(ANDROID_BUILD_TOOLS)/dx
ZIPALIGN = $(ANDROID_BUILD_TOOLS)/zipalign

all: $(BIN_DIR)/$(NAME).apk
debug: $(BIN_DIR)/$(NAME).debug.apk

.PHONY: all debug

#

PACKAGE_NAME = juloo.keyboard2

JAVA_FILES = $(shell find $(SRC_DIR) -name '*.java')
# A$B.class files are ignored
CLASS_FILES = $(JAVA_FILES:$(SRC_DIR)/%.java=$(OBJ_DIR)/%.class)

$(BIN_DIR)/$(NAME).dex: $(CLASS_FILES)

#

# debug apk
$(BIN_DIR)/%.debug.apk: $(BIN_DIR)/%.debug.signed.apk | $(BIN_DIR)
	$(ZIPALIGN) -fp 4 "$<" "$@"

DEBUG_KEYSTORE = $(BIN_DIR)/debug.keystore
DEBUG_PASSWD = debug0

$(BIN_DIR)/%.debug.signed.apk: $(BIN_DIR)/%.debug.unsigned.apk $(DEBUG_KEYSTORE) | $(BIN_DIR)
	jarsigner -keystore $(DEBUG_KEYSTORE) \
		-storepass $(DEBUG_PASSWD) -keypass $(DEBUG_PASSWD) \
		-signedjar "$@" "$<" debug

$(BIN_DIR)/%.debug.unsigned.apk: AAPT_PACKAGE_FLAGS+=--rename-manifest-package $(PACKAGE_NAME).debug

$(BIN_DIR)/debug.keystore: | $(BIN_DIR)
	echo y | keytool -genkeypair -dname "cn=d, ou=e, o=b, c=ug" \
		-alias debug -keypass $(DEBUG_PASSWD) -keystore "$@" \
		-keyalg rsa -storepass $(DEBUG_PASSWD) -validity 10000

# zipalign, produce the final apk
$(BIN_DIR)/%.apk: $(BIN_DIR)/%.signed.apk | $(BIN_DIR)
	$(ZIPALIGN) -fp 4 "$<" "$@"

# sign the unsigned apk
# %-keystore.conf should declare KEYSTORE, KEYNAME and OPTS
#  it is interpreted as a shell script
# OPTS can be used to pass -storepass or -keypass options to jarsigner
$(BIN_DIR)/%.signed.apk: $(BIN_DIR)/%.unsigned.apk %-keystore.conf | $(BIN_DIR)
	eval `cat $(word 2,$^)` && \
	jarsigner -keystore "$$KEYSTORE" $$OPTS -signedjar "$@" "$<" "$$KEYNAME"

# package the apk
# classes.dex: uses a dirty workaround
# assets: android tools are barely usable
$(BIN_DIR)/%.unsigned.apk: $(BIN_DIR)/%.dex $(MANIFEST_FILE) $(ASSETS_FILES) $(LIBS_FILES) | $(BIN_DIR)
	$(AAPT) package -f -M $(MANIFEST_FILE) -S $(RES_DIR) \
		-I $(ANDROID_FRAMEWORK) -F "$@" $(AAPT_PACKAGE_FLAGS)
	[ -z "$(ASSETS_FILES)" ] || $(AAPT) add "$@" $(ASSETS_FILES)
	ln -fs $(<F) $(@D)/classes.dex; cd $(@D) && \
	$(AAPT) add $(@F) classes.dex $(subst $(BIN_DIR)/,,$(LIBS_FILES))

# generate R.java
R_FILE = $(GEN_DIR)/$(subst .,/,$(PACKAGE_NAME))/R.java
RES_FILES = $(shell find $(RES_DIR) -type f)
$(R_FILE): $(RES_FILES) $(MANIFEST_FILE) | $(GEN_DIR)
	$(AAPT) package -f -m -S $(RES_DIR) -J $(GEN_DIR) \
		-M $(MANIFEST_FILE) -I $(ANDROID_FRAMEWORK)

# dex
$(BIN_DIR)/%.dex: $(R_FILE) | $(BIN_DIR)
	$(DX) --dex --output="$@" $(OBJ_DIR) $(subst :, ,$(EXTRA_JARS))

# build java classes
$(OBJ_DIR)/%.class: $(SRC_DIR)/%.java | $(OBJ_DIR)
	javac -d $(OBJ_DIR) $(JAVACFLAGS) \
		-classpath $(ANDROID_FRAMEWORK):$(EXTRA_JARS) \
		-sourcepath $(SRC_DIR):$(GEN_DIR) \
		$^

$(BIN_DIR):
	mkdir -p $@
$(OBJ_DIR):
	mkdir -p $@
$(GEN_DIR):
	mkdir -p $@
