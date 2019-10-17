.PHONY : test
test : clean
	ocamlfind opt -package lwt -c user.ml
	ocamlfind opt -package lwt -linkpkg user.cmx
	./a.out

.PHONY : clean
clean :
	rm -f *.cm* *.o a.out
