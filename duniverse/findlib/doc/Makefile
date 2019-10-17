DOCBOOK_HTML  = /usr/share/sgml/docbook/stylesheet/dsssl/modular/html
DOCBOOK_PRINT = /usr/share/sgml/docbook/stylesheet/dsssl/modular/print
SRC = $(PWD)/src

EXPORTED = findlib.mli fl_package_base.mli fl_metascanner.mli \
	   fl_dynload.mli topfind.mli

.PHONY: guide-html ref-html ref-man

default: guide-html ref-html ref-man README QUICKSTART

guide-html: guide-html/TIMESTAMP
ref-html: ref-html/TIMESTAMP
ref-man: ref-man/TIMESTAMP

guide-html/TIMESTAMP: src/*.sgml src/*.mod QUICKSTART.xml common.xml config.xml
	mkdir -p guide-html
	cd guide-html; \
	rm -f *.htm*; \
	jade -t sgml -D$(DOCBOOK_HTML) -D$(SRC) findlib.sgml; \
	true
	readme -html QUICKSTART.xml >guide-html/quickstart.html
	touch guide-html/TIMESTAMP

ref-html/TIMESTAMP: src/*.sgml src/*.mod common.xml config.xml $(EXPORTED:%=../src/findlib/%)
	mkdir -p ref-html
	cd ref-html; \
	rm -f *.htm*; \
	jade -t sgml -D$(DOCBOOK_HTML) -D$(SRC) findlib_ref.sgml; \
	true
	mkdir -p ref-html/lib
	cd ../src/findlib && \
	ocamldoc -html -d $(PWD)/ref-html/lib -stars -t "The Findlib Library" $(EXPORTED)
	touch ref-html/TIMESTAMP

src/findlib_reference.xml: src/*.sgml src/*.mod
	sgml2xml -D$(DOCBOOK_HTML) -D$(SRC) \
		findlib_reference.sgml >src/findlib_reference.xml ; \
	true

ref-man/TIMESTAMP: src/findlib_reference.xml
	mkdir -p ref-man
	cd ref-man; \
	rm -f *.[0-9] TIMESTAMP; \
	db2man <../src/findlib_reference.xml
	touch ref-man/TIMESTAMP

# Unfortunately, output of ocamldoc -man is too bad to be useful.
#	cd ../src/findlib && \
#	ocamldoc -man -man-mini -d $(PWD)/ref-man -man-suffix 3 -stars -t "The Findlib Library" $(EXPORTED)

.SUFFIXES: .xml .sgml

.sgml.xml: 
	sx -xndata $< >$@; true



clean:
	rm -rf guide-html guide-man
	rm -f src/*.xml

distclean:
	rm -f src/*.xml
	rm -f src/*~
	rm -f *~

QUICKSTART: QUICKSTART.xml
	readme -text QUICKSTART.xml >QUICKSTART

README: README.xml
	readme -text README.xml >README

