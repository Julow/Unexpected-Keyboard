(* $Id$ -*- tuareg -*- *)

(* For Ocaml-3.03 and up, so you can do: #use "topfind" and get a
 * working findlib toploop.
 *)

#directory "@SITELIB@/findlib";;
  (* OCaml-4.00 requires to have #directory before we load anything *)

(* First test whether findlib_top is already loaded. If not, load it now.
 * The test works by executing the toplevel phrase "Topfind.reset" and
 * checking whether this causes an error.
 *)
let exec_test s =
  let l = Lexing.from_string s in
  let ph = !Toploop.parse_toplevel_phrase l in
  let fmt = Format.make_formatter (fun _ _ _ -> ()) (fun _ -> ()) in
  try
    Toploop.execute_phrase false fmt ph
  with
      _ -> false
in

if not(exec_test "Topfind.reset;;") then (
  Topdirs.dir_load Format.err_formatter "@SITELIB@/findlib/findlib.cma";
  Topdirs.dir_load Format.err_formatter "@SITELIB@/findlib/findlib_top.cma";
);;

(* Old: *)
(* #load "@SITELIB@/findlib/findlib.cma";; *)
(* #load "@SITELIB@/findlib/findlib_top.cma";; *)


(* The following is always executed. It is harmless if findlib was already
 * initialized
 *)

Topfind.add_predicates [ "byte"; "toploop" ];
Topfind.don't_load ["findlib"];
Topfind.announce();;
