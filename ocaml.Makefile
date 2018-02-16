# Required variables:
# 	NAME
# 	OBJ_DIR
# 	OCAMLFIND
# 	OCAMLJAVA_DIR

SRC_DIR = srcs

all: $(NAME)

include ocaml.depend

OCAMLOPT = $(OCAMLFIND) ocamlopt
OCAMLDEP = $(OCAMLFIND) ocamldep
PPX = -ppx $(OCAMLJAVA_DIR)/ocaml-java-ppx
OCAMLOPT_FLAGS = \
	-I $(OCAMLJAVA_DIR)/javacaml javacaml.cmxa $(PPX) \
	$(addprefix -I ,$(OBJ_TREE))

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

ocaml.depend:
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
	} > ocaml.depend

clean:
	rm -f ocaml.depend
	rm -f $(CMX_FILES)
	rm -f $(NAME)

.PHONY: all clean
