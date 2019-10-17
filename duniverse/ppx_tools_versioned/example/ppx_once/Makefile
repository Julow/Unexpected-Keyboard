PACKAGE=ppx_once
OCAMLC=ocamlfind c
OCAMLOPT=ocamlfind opt
FLAGS=-package ocaml-migrate-parsetree,ppx_tools_versioned.metaquot_405
TARGETS=ppx_once ppx_once.cmo ppx_once.cmx ppx_once.cmxs

all: build
	
clean:
	rm -f *.o *.cm* $(TARGETS)

build: $(TARGETS)

install: build
	ocamlfind install $(PACKAGE) META $(TARGETS)

uninstall:
	ocamlfind remove $(PACKAGE)

reinstall:
	$(MAKE) uninstall
	$(MAKE) install

%.cmo: %.ml
	$(OCAMLC) $(FLAGS) -c $^

%.cmx: %.ml
	$(OCAMLOPT) $(FLAGS) -c $^

ppx_once.cmxs: ppx_once.cmx
	$(OCAMLOPT) -o $@ -shared $^

ppx_once: ppx_once.cmx standalone.ml
	$(OCAMLOPT) $(FLAGS) -o $@ -linkpkg $^
