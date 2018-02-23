# Required variables:
# 	NAME
# 	OBJ_DIR
# 	OCAMLFIND
# 	OCAMLJAVA_DIR

SRC_DIR = srcs

DEPEND_FILE = $(OBJ_DIR)/ocaml.depend

all: $(NAME)

include $(DEPEND_FILE)

OCAMLOPT = $(OCAMLFIND) ocamlopt
OCAMLDEP = $(OCAMLFIND) ocamldep
PPX = -ppx $(OCAMLJAVA_DIR)/ocaml-java-ppx
OCAMLOPT_FLAGS = \
	-I $(OCAMLJAVA_DIR)/javacaml javacaml.cmxa $(PPX) \
	$(addprefix -I ,$(OBJ_TREE))

$(CMX_FILES): $(OCAMLJAVA_DIR)/javacaml/javacaml.cmxa \
	$(OCAMLJAVA_DIR)/ocaml-java-ppx

$(NAME): $(CMX_FILES)
	$(OCAMLOPT) -linkpkg -output-obj -ccopt "-shared" \
		-cclib "-L $(OCAMLJAVA_DIR)" \
		$(OCAMLOPT_FLAGS) \
		$^ -o $@

$(OBJ_DIR)/%.cmi: %.mli | $(OBJ_TREE)
	$(OCAMLOPT) $(OCAMLOPT_FLAGS) -c $< -o $@

$(OBJ_DIR)/%.cmx: %.ml | $(OBJ_TREE)
	$(OCAMLOPT) $(OCAMLOPT_FLAGS) -c $< -o $@

$(OBJ_TREE):
	mkdir -p $@

$(DEPEND_FILE):
	ML_FILES=`find $(SRC_DIR) -name '*.ml'`; \
	SRC_TREE=`find $(SRC_DIR) -type d`; \
	INCLUDE_FLAGS=`for d in $$SRC_TREE; do echo -I "$$d"; done`; \
	OBJ_TREE=`printf " \\$$(OBJ_DIR)/%s" $$SRC_TREE`; \
	{	printf "CMX_FILES ="; \
		for f in `$(OCAMLDEP) -sort $$INCLUDE_FLAGS $(PPX) $$ML_FILES`; do \
			printf " \$$(OBJ_DIR)/$${f%.ml}.cmx"; \
		done; echo; \
		echo "OBJ_TREE =" $$OBJ_TREE; \
		$(OCAMLDEP) -native -one-line $(PPX) $$INCLUDE_FLAGS $$ML_FILES \
		| sed 's#[^: ]\+#\$$(OBJ_DIR)/&#g'; \
	} > $(DEPEND_FILE)

clean:
	rm -f ocaml.depend
	rm -f $(CMX_FILES)
	rm -f $(NAME)

.PHONY: all clean
