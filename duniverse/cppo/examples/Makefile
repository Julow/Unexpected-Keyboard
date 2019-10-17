.PHONY: all clean
all:
	../cppo debug.ml > debug.out
	../cppo french.ml > french.out
	ocamllex lexer.mll
	../cppo lexer.ml > lexer.out
clean:
	rm -f *.out lexer.ml
