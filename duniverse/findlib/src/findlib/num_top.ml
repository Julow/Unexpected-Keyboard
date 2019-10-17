(* $Id$
 * ----------------------------------------------------------------------
 *
 *)

let print_outcome = false
let error_fmt = Format.err_formatter

let printers = [
  "Num_top_printers.nat_printer";
  "Num_top_printers.big_int_printer";
  "Num_top_printers.ratio_printer";
  "Num_top_printers.num_printer";
]

let eval_phrase s =
  let lexbuf = Lexing.from_string s in
  let phrase = !Toploop.parse_toplevel_phrase lexbuf in
  Toploop.execute_phrase print_outcome error_fmt phrase

let install_all () =
  List.fold_left
    (fun outcome phrase ->
      outcome && eval_phrase (Printf.sprintf "#install_printer %s;;" phrase))
    true printers

let _ =
  if not (install_all ()) then begin
    Format.fprintf error_fmt
      "Something weird appened while installing Num library printers";
    Format.pp_print_flush error_fmt ()
  end
