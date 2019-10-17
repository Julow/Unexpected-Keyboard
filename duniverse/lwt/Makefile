# Default rule
.PHONY: default
default: build

# build the usual development packages
.PHONY: build
build:
	dune build

# run unit tests for package lwt
.PHONY: test
test: build
	dune runtest

# Install dependencies needed during development.
.PHONY : dev-deps
dev-deps :
	opam install . --deps-only --yes

# Use Dune+odoc to generate static html documentation.
# Currently requires ocaml 4.03.0 to install odoc.
.PHONY: doc
doc:
	dune build @doc

# Build HTML documentation with ocamldoc
.PHONY: doc-api-html
doc-api-html: build
	$(MAKE) -C docs api/html/index.html

# Build wiki documentation with wikidoc
# requires ocaml 4.03.0 and pinning the repo
# https://github.com/ocsigen/wikidoc
.PHONY: doc-api-wiki
doc-api-wiki: build
	$(MAKE) -C docs api/wiki/index.wiki

# Packaging tests. These are run with Lwt installed by OPAM, typically during
# CI. To run locally, run the install-for-packaging-test target first.
.PHONY: packaging-test
packaging-test:
	ocamlfind query lwt
	for TEST in `ls -d test/packaging/*/*` ; \
	do \
	    $(MAKE) -wC $$TEST || exit 1 ; \
		echo ; \
		echo ; \
	done

.PHONY: install-for-packaging-test
install-for-packaging-test: clean
	opam pin add --yes --no-action lwt .
	opam pin add --yes --no-action lwt_ppx .
	opam pin add --yes --no-action lwt_react .
	opam reinstall --yes lwt lwt_ppx lwt_react

.PHONY: uninstall-after-packaging-test
uninstall-after-packaging-test:
	opam remove --yes lwt lwt_ppx lwt_react
	opam pin remove --yes lwt
	opam pin remove --yes lwt_ppx
	opam pin remove --yes lwt_react

# ppx_let integration test.
.PHONY : ppx_let-test
ppx_let-test :
	dune build test/ppx_let/test.exe
	dune exec test/ppx_let/test.exe

.PHONY : ppx_let-test-deps
ppx_let-test-deps :
	opam install --yes --unset-root ppx_let

.PHONY: clean
clean:
	dune clean
	find . -name '.merlin' | xargs rm -f
	rm -fr docs/api
	rm -f src/jbuild-ignore src/unix/lwt_config
	for TEST in `ls -d test/packaging/*/*` ; \
	do \
	    $(MAKE) -wC $$TEST clean ; \
	done
	rm -rf _coverage/

EXPECTED_FILES := \
    --expect src/core/ \
    --expect src/react/ \
    --expect src/unix/ \
    --do-not-expect src/unix/config/ \
    --do-not-expect src/unix/lwt_gc.ml \
    --do-not-expect src/unix/lwt_throttle.ml \
    --do-not-expect src/unix/unix_c/

.PHONY: coverage
coverage: clean coverage-only

.PHONY : coverage-only
coverage-only :
	BISECT_ENABLE=yes $(MAKE) build
	BISECT_ENABLE=yes dune runtest --force
	bisect-ppx-report html $(EXPECTED_FILES)
	bisect-ppx-report summary
	@echo See _coverage/index.html
