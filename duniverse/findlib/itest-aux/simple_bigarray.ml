open Bigarray.Array1;;
let a = create Bigarray.int Bigarray.c_layout 4 in
a.{0} <- 5
;;

print_endline "OK";;

