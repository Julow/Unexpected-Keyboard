.PHONY : test
test : clean
	ocamlfind opt -package lwt.unix -c user.ml
	ocamlfind opt -package lwt.unix -thread -linkpkg user.cmx
	./a.out

.PHONY : clean
clean :
	rm -f *.cm* *.o a.out
