BYTE_FILES=bytes.cmi bytes.cma
NATIVE_FILES=bytes.cmx bytes$(LIB_SUFFIX) bytes.cmxa
NATIVE_FILES_DYNLINK=bytes.cmxs
OCAMLBUILD=ocamlbuild -classic-display -no-links

TOP=../..
include $(TOP)/Makefile.config

build: all opt

all:
	$(OCAMLBUILD) $(BYTE_FILES)

opt:
	files="$(NATIVE_FILES)"; \
	if [ $(HAVE_NATDYNLINK) = 1 ]; then \
	    files="$$files $(NATIVE_FILES_DYNLINK)"; \
	fi; \
	$(OCAMLBUILD) $$files

install: all
	mkdir -p "$(prefix)$(OCAML_SITELIB)/bytes"
	cd _build/ && cp ../META $(BYTE_FILES) "$(prefix)$(OCAML_SITELIB)/bytes"
	cd _build/ && for f in $(NATIVE_FILES) $(NATIVE_FILES_DYNLINK); do if [ -f "$$f" ]; then cp $$f "$(prefix)$(OCAML_SITELIB)/bytes"; fi; done

uninstall: 
	rm -rf "$(prefix)$(OCAML_SITELIB)/bytes"

# install-self and uninstall-self use ocamlfind already. This is a bit
# questionable here.

install-self: all
	cd _build/ && ocamlfind install bytes ../META $(BYTE_FILES) -optional $(NATIVE_FILES) $(NATIVE_FILES_DYNLINK)

uninstall-self:
	ocamlfind remove bytes

clean:
	ocamlbuild -clean
