.PHONY: all clean test doc

all:
	dune build

clean:
	dune clean

test:
	dune runtest --profile=release

doc:
	dune build @doc
