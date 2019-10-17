
all: build

OCAML_VERSION=$(shell ocaml -vnum)
SELECT_CMD=$(shell ocaml select_version.ml $(OCAML_VERSION))

select:
	$(SELECT_CMD)

TARGETS=$(addprefix src/, seq.cma seq.cmxa seq.cmxs)
build: select
	ocamlbuild $(TARGETS)

clean:
	rm src/seq.ml src/seq.mli || true
	ocamlbuild -clean

TOINSTALL=$(wildcard _build/src/*)

install:
	ocamlfind install seq META $(TOINSTALL)

.PHONY: build clean install
