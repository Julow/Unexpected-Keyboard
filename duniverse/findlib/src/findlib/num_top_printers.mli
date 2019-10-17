(* $Id$
 * ----------------------------------------------------------------------
 *
 *)

(**
  Printers for types defined in the "num" library. Meant to be used as printers
  in the ocaml toplevel. See num_top.mli.

  Copyright (C) 2003  Stefano Zacchiroli <zack@debian.org>

  Released under the same terms as findlib.
*)

val nat_printer : Format.formatter -> Nat.nat -> unit
val big_int_printer : Format.formatter -> Big_int.big_int -> unit
val ratio_printer : Format.formatter -> Ratio.ratio -> unit
val num_printer: Format.formatter -> Num.num -> unit

