(* $Id$ *)

(* Runs ocamlc -help and extract the command-line signature *)

open Printf


let read_lines file =
  let f = open_in file in
  let lines = ref [] in
  try
    while true do
      lines := input_line f :: !lines
    done;
    assert false
  with
    | End_of_file ->
	close_in f;
	List.rev !lines
    | error ->
	close_in f; 
	raise error
;;
      

let get_help cmd =
  let temp_file = 
    Filename.temp_file "findlib" ".txt" in
  let help_out =
    try
      let code =
	Sys.command (sprintf "%s -help >%s 2>&1"
		       cmd
		       (Filename.quote temp_file)) in
      if code <> 0 then 
	raise Not_found;  (* Assume command does not exist! *)
      let lines = read_lines temp_file in
      Sys.remove temp_file;
      lines
    with error ->
      Sys.remove temp_file; raise error in
  help_out
;;


let switch1_re = Str.regexp "[ \t]*\\(-[-a-zA-Z0-9_,]+\\)[ \t]?\\(.*\\)$";;
let switch2_re = Str.regexp "[ \t]*\\(-\\)[ \t]+\\(.*\\)$";;

let argument_re = Str.regexp "[ \t]*[<[{]";;

let rec extract_signature lines =
  match lines with
    | [] ->
	[]
    | line :: lines' ->
	if Str.string_match switch1_re line 0 || Str.string_match switch2_re line 0 then (
	  let switch_name = Str.matched_group 1 line in
	  let help_text = Str.matched_group 2 line in
	  let has_arg = Str.string_match argument_re help_text 0 in
	  let help_lines, lines'' = extract_help_continuation lines' in
	  let help_text' = String.concat "\n" (help_text :: help_lines) in
	  let r = 
	    (switch_name, has_arg, help_text') in
	  r :: extract_signature lines''
	)
	else
	  extract_signature lines'
  
and extract_help_continuation lines =
  match lines with
    | [] ->
	( [], [] )
    | line :: lines' ->
	if Str.string_match switch1_re line 0 || Str.string_match switch2_re line 0 then
	  ( [], lines )
	else
	  let help_lines, lines'' = extract_help_continuation lines' in
	  (line :: help_lines, lines'')
;;


let rm_help_switch switches =
  List.filter
    (fun (name, _, _) ->
       name <> "-help" && name <> "--help")
    switches
;;


let output_some_signature f name switches =
  fprintf f "let %s_spec = Some [\n" name;
  List.iter
    (fun (switch_name, has_arg, help_text) ->
       fprintf f "  \"%s\",\n" (String.escaped switch_name);
       fprintf f "  %b,\n" has_arg;
       fprintf f "  \"%s\";\n\n" (String.escaped help_text)
    )
    switches;
  fprintf f "];;\n\n"
;;


let output_none f name =
  fprintf f "let %s_spec = None;;\n\n" name
;;


let main() =
  let f = ref stdout in
  let progs = ref [] in
  Arg.parse
    [ "-o", Arg.String (fun s -> f := open_out s),
      "<file>  Save generated module to this file";
    ]
    (fun arg -> progs := arg :: !progs)
    "usage: extract_args <options> <command> ...";

  progs := List.rev !progs;

  List.iter
    (fun prog ->
       try
	 let help_lines = get_help prog in  (* or Not_found *)
	 let switches = rm_help_switch (extract_signature help_lines) in
	 output_some_signature !f prog switches
       with
	   Not_found ->
	     output_none !f prog
    )
    !progs;

  close_out !f
;;


main();;

