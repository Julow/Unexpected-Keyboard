BLD=../_build/default/src
SRC=../src

PKGS=\
	-package bytes -package result \
	-package bigarray -package unix \
	-package ocaml-migrate-parsetree -package ppx_tools_versioned \
	-package react

INCS=\
	-I ${BLD}/core/.lwt.objs/byte \
	-I ${BLD}/ppx/.ppx_lwt.objs/byte \
	-I ${BLD}/react/.lwt_react.objs/byte \
	-I ${BLD}/unix/.lwt_unix.objs/byte

MLIS=\
	$(wildcard ${SRC}/core/*.mli) \
	$(wildcard ${SRC}/ppx/*.mli) \
	$(wildcard ${SRC}/react/*.mli) \
	$(filter-out ${BLD}/unix/lwt_unix.cppo.mli,$(wildcard ${BLD}/unix/*.mli))

MLIS := $(filter-out %.pp.mli,$(MLIS))

DOCOPT := -colorize-code -short-functors -charset utf-8

.PHONY: doc wikidoc
doc: api/html/index.html
api/html/index.html: ${MLIS} apiref-intro
	mkdir -p api/html
	ocamlfind ocamldoc ${DOCOPT} -package ocamlbuild,uchar ${PKGS} ${INCS} -intro apiref-intro -html \
		-d api/html \
		${MLIS}

wikidoc: api/wiki/index.wiki
api/wiki/index.wiki: ${MLIS} apiref-intro
	mkdir -p api/wiki
	ocamlfind ocamldoc ${DOCOPT} -package ocamlbuild,uchar ${PKGS} ${INCS} -intro apiref-intro \
		-d api/wiki \
		-i $(shell ocamlfind query wikidoc) -g odoc_wiki.cma \
		${MLIS}

.PHONY : clean
clean :
	rm -rf api/
