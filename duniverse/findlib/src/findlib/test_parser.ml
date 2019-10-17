let with_open_in file func =
  let chan = open_in file in
  let result =
    try func chan
    with exn -> close_in chan; raise exn
  in
  close_in chan; result
;;

type test_mode = Compare_both | Only of (old_or_new * int)
and old_or_new = Old | New

let read_file file =
  let buf = Buffer.create 100 in
  with_open_in file (fun ch ->
    try while true do Buffer.add_string buf (input_line ch) done
    with End_of_file -> ());
  Buffer.contents buf

let test mode file =
  match mode with
    | Compare_both ->
      let ast1 = with_open_in file Fl_metascanner.parse in
      let ast2 = with_open_in file Fl_metascanner.parse2 in
      Printf.printf "%s tested %s\n" file (if ast1 = ast2 then "OK" else "FAIL")
    | Only (old_or_new, niter) ->
      let content = read_file file in
      for i = 1 to niter do
        let parse = match old_or_new with
          | Old -> Fl_metascanner.parse_lexing
          | New -> Fl_metascanner.parse2_lexing in
        ignore (parse (Lexing.from_string content))
      done

let rec explore mode path =
  if Sys.is_directory path then
    let traverse file = explore mode (Filename.concat path file) in
    Array.iter traverse (Sys.readdir path)
  else if Filename.basename path = "META" then
    test mode path

let () =
  let test_mode, targets = (* command-line option handling *)
    let only_old = ref false in
    let only_new = ref false in
    let niter = ref 1 in
    let targets = ref [] in
    let usage = "test_parser <options> <path> <path> ....\n   \
               recursively traverse paths and compare the two parsers \
               on each file named META"
    in
    let options = [
      ("--niter", Arg.Set_int niter, "iterate the parser for performance comparison (only in --only-old or --only-new modes)");
      ("--only-old", Arg.Set only_old, "only test the old parser");
      ("--only-new", Arg.Set only_new, "only test the new parser");
    ] in
    let action path = targets := path :: !targets in
    Arg.parse options action usage;
    let quit_with_usage () = Arg.usage options usage; exit 1 in
    let test_mode =
      match !only_old, !only_new with
        | false, false -> Compare_both
        | false, true -> Only (New, !niter)
        | true, false -> Only (Old, !niter)
        | true, true ->
          prerr_endline "--only-only and --new-only cannot be \
                       both set at the same time";
          quit_with_usage ();
    in
    if !targets = [] then quit_with_usage ();
    test_mode, !targets
  in
  let traverse path =
    if Sys.file_exists path then explore test_mode path
    else Printf.eprintf "Error: path %s does not exist and was ignored.\n" path
  in
  List.iter traverse targets










