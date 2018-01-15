# Required variables:
# 	NAME
# 	OBJ_DIR

SRC_DIR = srcs

ML_FILES = $(shell find $(SRC_DIR) -name '*.ml')
C_FILES = $(shell find $(SRC_DIR) -name '*.c')

OCAMLOPT = $(OCAMLFIND) ocamlopt

$(NAME): $(ML_FILES) $(C_FILES)
	$(OCAMLOPT) -linkpkg -output-obj \
		-ccopt "-shared -I $(NDK_PLATFORM)/usr/include" \
		$^ -o $@
