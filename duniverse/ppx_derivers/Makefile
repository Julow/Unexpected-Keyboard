INSTALL_ARGS := $(if $(PREFIX),--prefix $(PREFIX),)

.PHONY: all
all:
	dune build

.PHONY: install
install:
	dune install $(INSTALL_ARGS)

.PHONY: uninstall
uninstall:
	dune uninstall $(INSTALL_ARGS)

.PHONY: reinstall
reinstall:
	$(MAKE) uninstall
	$(MAKE) install

.PHONY: clean
clean:
	dune clean
