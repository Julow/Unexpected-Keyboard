.PHONY : test
test : clean
	ocamlfind opt -package lwt.unix -c user.ml
	ocamlfind opt -package lwt.unix -linkpkg -thread user.cmx
	./a.out

.PHONY : clean
clean :
	rm -f *.cm* *.o a.out
