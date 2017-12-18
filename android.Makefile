# Required variables:
# 	NAME
# 	ARCHS

BIN_DIR = bin
OBJ_DIR = $(BIN_DIR)/objs
GEN_DIR = $(BIN_DIR)/gen
RES_DIR = res
SRC_DIR = srcs
ASSETS_DIR = assets

ANDROID_FRAMEWORK = $(ANDROID_PLATFORM)/android.jar
MANIFEST_FILE = AndroidManifest.xml

ASSETS_FILES = $(shell find $(ASSETS_DIR) -type f)
LIBS_FILES = $(foreach a,$(ARCHS),$(BIN_DIR)/lib/$(a)/lib$(NAME).so)

CLASSPATHS = libs/camljava/lib/camljava.jar

all: $(BIN_DIR)/$(NAME).apk

.PHONY: all unsigned

#

PACKAGE_NAME = juloo.keyboard2

JAVA_FILES = $(shell find $(SRC_DIR) -name '*.java')
# A$B.class files are ignored
CLASS_FILES = $(JAVA_FILES:$(SRC_DIR)/%.java=$(OBJ_DIR)/%.class)

$(BIN_DIR)/$(NAME).dex: $(CLASS_FILES) $(OBJ_DIR)/camljava.jar

$(OBJ_DIR)/camljava.jar: libs/camljava/lib/camljava.jar
	ln -sf "`pwd`/$<" "$@"

#

# zipalign, produce the final apk
$(BIN_DIR)/%.apk: $(BIN_DIR)/%.signed.apk | $(BIN_DIR)
	zipalign -fp 4 "$<" "$@"

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
	aapt package -f -M $(MANIFEST_FILE) -S $(RES_DIR) \
		-I $(ANDROID_FRAMEWORK) -F "$@"
	aapt add "$@" $(ASSETS_FILES)
	ln -fs $(<F) $(@D)/classes.dex; cd $(@D) && \
	aapt add $(@F) classes.dex $(subst $(BIN_DIR)/,,$(LIBS_FILES))

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
	javac -d $(OBJ_DIR) $(JAVACFLAGS) \
		-classpath $(ANDROID_FRAMEWORK):$(CLASSPATHS) \
		-sourcepath $(SRC_DIR):$(GEN_DIR) \
		$^

$(BIN_DIR):
	mkdir -p $@
$(OBJ_DIR):
	mkdir -p $@
$(GEN_DIR):
	mkdir -p $@
