TOP=../..
include $(TOP)/Makefile.config

.PHONY: all opt install uninstall clean

all: make_wizard$(EXEC_SUFFIX)

opt:
	true

make_wizard$(EXEC_SUFFIX): make_wizard.ml
	ocamlc -o make_wizard$(EXEC_SUFFIX) -I +labltk -I ../findlib unix.cma str.cma labltk.cma \
	 	findlib.cma make_wizard.ml

install:
	cp make_wizard$(EXEC_SUFFIX) make_wizard.pattern $(prefix)$(OCAML_SITELIB)/findlib

# uninstall: Nothing to do, because the removal of the findlib core also
# deinstalls the make_wizard
uninstall:
	true

# ----------------------------------------------------------------------

tree: lx_spots.mli lx_spots.ml lx_tree.mli lx_tree.ml test_tree.ml
	ocamlfind ocamlc -o tree -package labltk,unix,str -linkpkg \
		lx_spots.mli lx_spots.ml lx_tree.mli lx_tree.ml test_tree.ml

tree_editor: lx_spots.mli lx_spots.ml lx_tree.mli lx_tree.ml tree_editor.ml
	ocamlfind ocamlc -o tree_editor -package labltk,unix,str -linkpkg \
		lx_spots.mli lx_spots.ml lx_tree.mli lx_tree.ml tree_editor.ml

# ----------------------------------------------------------------------

clean:
	rm -f *.cmi *.cmo
	rm -f make_wizard$(EXEC_SUFFIX) # tree tree_editor
