INSTALL_ARGS := $(if $(PREFIX),--prefix $(PREFIX),)

default:
	jbuilder build @install

install:
	jbuilder install $(INSTALL_ARGS)

uninstall:
	jbuilder uninstall $(INSTALL_ARGS)

reinstall: uninstall reinstall

clean:
	jbuilder clean

all-supported-ocaml-versions:
	jbuilder build --workspace jbuild-workspace.dev

.PHONY: default install uninstall reinstall clean all-supported-ocaml-versions
