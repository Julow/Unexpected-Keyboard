#

# Require ANDROID_PLATFORM variables
# Except android build tools in PATH

PACKAGE_NAME = juloo.keyboard2
NAME = $(subst .,-,$(PACKAGE_NAME))

BIN_DIR = bin
OBJ_DIR = $(BIN_DIR)/objs
GEN_DIR = $(BIN_DIR)/gen
RES_DIR = res
SRC_DIR = srcs
ASSETS_DIR = assets

JAVA_FILES = $(shell find $(SRC_DIR) -type f)
# A$B.class files are ignored
CLASS_FILES = $(JAVA_FILES:$(SRC_DIR)/%.java=$(OBJ_DIR)/%.class)

ASSETS_FILES = $(shell find $(ASSETS_DIR) -type f)

ANDROID_FRAMEWORK = $(ANDROID_PLATFORM)/android.jar
MANIFEST_FILE = AndroidManifest.xml

all: $(BIN_DIR)/$(NAME).apk
unsigned: $(BIN_DIR)/$(NAME).unsigned.apk

$(BIN_DIR)/$(NAME).dex: $(CLASS_FILES)

clean:
	rm -fd \
		$(R_FILE) \
		$(CLASS_FILES) \
		$(BIN_DIR)/classes.dex \
		$(BIN_DIR)/$(NAME).dex \
		$(BIN_DIR)/$(NAME).unsigned.apk \
		$(BIN_DIR)/$(NAME).signed.apk \
		$(BIN_DIR)/$(NAME).apk

re: clean
	make all

.PHONY: all unsigned clean re

#

# zipalign, produce the final apk
$(BIN_DIR)/%.apk: $(BIN_DIR)/%.signed.apk | $(BIN_DIR)
	zipalign -fp 4 "$<" "$@"

# sign the unsigned apk
# %-keystore.conf should declare KEYSTORE, KEYNAME and OPTS
#  it is interpreted as a shell script
# OPTS can be used to pass -storepass or -keypass options to jarsigner
$(BIN_DIR)/%.signed.apk: $(BIN_DIR)/%.unsigned.apk %-keystore.conf | $(BIN_DIR)
	eval `cat $(word 2,$^)`; \
	jarsigner -keystore "$$KEYSTORE" $$OPTS -signedjar "$@" "$<" "$$KEYNAME"

# package the apk
# classes.dex: uses a dirty workaround
# assets: android tools are barely usable
$(BIN_DIR)/%.unsigned.apk: $(BIN_DIR)/%.dex $(MANIFEST_FILE) $(ASSETS_FILES) | $(BIN_DIR)
	aapt package -f -M $(MANIFEST_FILE) -S $(RES_DIR) \
		-I $(ANDROID_FRAMEWORK) -F "$@"
	aapt add "$@" $(ASSETS_FILES)
	ln -fs $(<F) $(@D)/classes.dex; cd $(@D); \
	aapt add $(@F) classes.dex

# generate R.java
R_FILE = $(GEN_DIR)/$(subst .,/,$(PACKAGE_NAME))/R.java
RES_FILES = $(shell find $(RES_DIR) -type f)
$(R_FILE): $(RES_FILES) | $(GEN_DIR)
	aapt package -f -m -S $(RES_DIR) -J $(GEN_DIR) \
		-M $(MANIFEST_FILE) -I $(ANDROID_FRAMEWORK)

# dex
$(BIN_DIR)/%.dex: $(R_FILE) | $(BIN_DIR)
	dx --dex --output="$@" $(OBJ_DIR)

# build java classes
$(OBJ_DIR)/%.class: $(SRC_DIR)/%.java | $(OBJ_DIR)
	javac -d $(OBJ_DIR) \
		-encoding utf8 \
		-classpath $(ANDROID_FRAMEWORK) \
		-sourcepath $(SRC_DIR):$(GEN_DIR) \
		$^

$(BIN_DIR):
	mkdir -p $@
$(OBJ_DIR):
	mkdir -p $@
$(GEN_DIR):
	mkdir -p $@
