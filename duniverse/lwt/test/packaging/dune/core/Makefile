.PHONY : test
test : clean
	dune build user.exe --root .
	_build/default/user.exe

.PHONY : clean
clean :
	dune clean --root .
