(* $Id$
 * ----------------------------------------------------------------------
 *
 *)

open Findlib;;

exception Usage;;
exception Silent_error;;
  
type mode =
    M_use | M_query | M_install | M_remove | M_compiler of string | M_dep
  | M_printconf | M_list | M_browser | M_call of (string*string)
  | M_doc | M_lint | M_printppx
;;


type psubst =
    Const of string
  | Percent of string * modifier
  | Lookup of string * modifier

and modifier =
  | Plain
  | Plus
;;

let sys_error code arg =
  if arg = "" then
    Sys_error (Unix.error_message code)
  else
    Sys_error (arg ^ ": " ^ Unix.error_message code)


let slashify s =
  match Findlib_config.system with
    | "mingw" | "mingw64" | "cygwin" ->
        let b = Buffer.create 80 in
        String.iter
          (function
            | '\\' -> Buffer.add_char b '/'
            | c -> Buffer.add_char b c
          )
          s;
        Buffer.contents b
    | _ ->
	s


let out_path ?(prefix="") s =
  match Findlib_config.system with
    | "mingw" | "mingw64" | "cygwin" ->
	let u = slashify s in
	prefix ^ 
	  (if String.contains u ' ' then
	     (* Desperate attempt to fix the space problem in paths.
                Note that we invoke commands via Unix.open_process, and
                this function already quotes the arguments on win32.
                However, for -ccopt arguments, one quoting level seems
                to be lost, and we have to add another level to compensate.
                E.g. for the list of args
                  [ -ccopt; -L/my programs/include -L/somewhere ]
                we get after out_path
                  [ -ccopt; "-I/my programs/include -L/somewhere" ]
                which actually translates to
                  -ccopt "\"-I/my programs/include\" \"-L/somewhere\""
                on the command line, i.e. a double-quoted argument.
	      *)
	     "\"" ^ u ^ "\""
	   else
	     u
	  )
    | _ ->
	prefix ^ slashify s



let percent_subst ?base spec lookup s =
  (* spec = [ "%c", [ "ctext1"; "ctext2"; ... ];
   *          "%d", [ "dtext1"; "dtext2"; ... ] ]
   * All occurrences of %c in the string s are replaced as specified in spec.
   * spec is an association list with the %-notation as keys
   * and lists of strings as values. The result is a list of strings containing
   * every combination of substituted values.
   *
   * Support for the %(name) syntax: In this case, the name is taken as
   * key for the [lookup] function, which either returns the string value
   * or raises Not_found.
   *
   * "+" modifier: A "+" after "%" causes that Findlib.resolve_path is
   * called for the substitution string (e.g. %+c, %+(name)).
   *
   * Example:
   * spec = [ "%a", [ "file1" ] ]
   * lookup = function "archive" -> "file2" | _ -> raise Not_found
   * Here, %a is substituted by file1, and %(archive) is substituted by
   * file2.
   *
   * ?base: The base parameter for Findlib.resolve_path.
   *)
  let l = String.length s in

  let fail() =
    failwith "bad format string" in

  let parenthesized_name j =
    try		  
      if j+1>=l then raise Not_found;
      let k = String.index_from s (j+1) ')' in
      let name = String.sub s (j+1) (k-j-1) in
      (name, k+1)
    with Not_found ->
      fail() in

  let rec preprocess i j =
    if j<l then begin
      match s.[j] with
	'%' ->
	  if j+1<l then begin
	    let prev = Const(String.sub s i (j-i)) in
	    let c = s.[j+1] in
	    match c with
		'%' -> 
		  prev :: Const "%" :: preprocess (j+2) (j+2)
	      | '(' ->
                  let name, j_next = parenthesized_name (j+1) in
                  prev :: Lookup(name,Plain) :: preprocess j_next j_next
              | '+' ->
                  if j+2<l then begin
                    let c = s.[j+2] in
                    match c with
                      | '%' | '+' -> fail()
                      | '(' ->
                           let name, j_next = parenthesized_name (j+2) in
                           prev :: Lookup(name,Plus) :: preprocess j_next j_next
                      | _ ->
		           let name = "%" ^ String.make 1 c in
	                   prev :: Percent(name,Plus) :: preprocess (j+3) (j+3)
                  end
                  else fail()
	      | _ ->
		  let name = "%" ^ String.make 1 c in
		  prev :: Percent(name,Plain) :: preprocess (j+2) (j+2)
	  end
	  else fail()
      |	_ ->
	  preprocess i (j+1)
    end
    else
      if i<j then
	[Const(String.sub s i (j-i))]
      else
	[]
  in

  let plus_subst u =
    String.concat
      " "
      (List.map
         (Findlib.resolve_path ?base)
         (Fl_split.in_words u)) in

  let any_subst modi u =
    match modi with
      | Plain -> u
      | Plus -> plus_subst u in

  let rec subst prefix l =
    match l with
      [] -> [prefix]
    | Const s :: l' ->
	subst (prefix ^ s) l'
    | Percent(name,modi) :: l' ->
	let replacements0 =
	  try List.assoc name spec
	  with Not_found -> failwith "bad format string" in
        let replacements =
          List.map (any_subst modi) replacements0 in
	List.flatten
	  (List.map
	     (fun replacement ->
	       subst (prefix ^ replacement) l')
	     replacements)
    | Lookup(name,modi) :: l' ->
	let replacement0 =
	  try lookup name
	  with Not_found -> "" in
        let replacement =
          any_subst modi replacement0 in
	subst (prefix ^ replacement) l'
  in

  subst "" (preprocess 0 0)
;;


let rec remove_dups l =
  match l with
    x :: l' ->
      if List.mem x l' then remove_dups l' else x::remove_dups l'
  | [] -> []
;;


let arg n =
  if n < Array.length Sys.argv then Sys.argv.(n) else raise Not_found
;;


let escape_if_needed s =
  if String.contains s ' ' then "\"" ^ String.escaped s ^ "\"" else s
;;


let use_package prefix pkgnames =
  (* may raise No_such_package *)
  let pdirs =
    List.map
      (fun pname ->
         "-I " ^ out_path(package_directory pname)
      )
      pkgnames
  in

  print_endline (prefix ^ String.concat " " pdirs)
;;


let read_ldconf filename =
  let lines = ref [] in
  let f = open_in filename in
  try
    while true do
      let line = input_line f in
      if line <> "" then
	lines := line :: !lines
    done;
    assert false
  with
      End_of_file ->
	close_in f;
	List.rev !lines
    | other ->
	close_in f;
	raise other
;;


let write_ldconf filename lines new_lines =
  let f = open_out filename in
  try
    List.iter
      (fun line -> output_string f (line ^ "\n"))
      (lines @ new_lines);
    close_out f;
    prerr_endline("Updated " ^ filename);
  with
      Sys_error e ->
	prerr_endline ("ocamlfind: [WARNING] Cannot write " ^ filename);
	prerr_endline ("Reason: " ^ e);
	prerr_endline ("This file contains the directories with DLLs.");
	if new_lines <> [] then begin
	  prerr_endline ("It is recommended to add the following line(s) to this file:");
	  List.iter prerr_endline new_lines
	end
;;


let is_dll p =
  let sfx = Findlib_config.dll_suffix in
  sfx <> "" && Filename.check_suffix p sfx
;;


let identify_dir d =
  match Sys.os_type with
    | "Win32" ->
	failwith "identify_dir"   (* not available *)
    | _ ->
	let s = Unix.stat d in
	(s.Unix.st_dev, s.Unix.st_ino)
;;


let conflict_report incpath pkglist =
  (* Check whether there are several definitions for packages
   * in the current path. We remove duplicate directories first.
   * Note that all other checks are not sensitive to duplicate directories.
   *)
  Fl_package_base.package_conflict_report ~identify_dir ();

  (* Second check whether there are module conflicts *)
  let pkgpath =
    List.map Findlib.package_directory pkglist in
  Fl_package_base.module_conflict_report ~identify_dir (pkgpath @ incpath);

  (* Finally check whether there are multiple DLLs: *)
  (* Note: Only the directories mentioned in ld.conf are checked, but not the
   * directories in [incpath], and not the directories in CAML_LD_LIBRARY_PATH.
   * The idea of this check is to ensure a proper installation, and not to
   * complain about the user's special configuration.
   *)
  let ldconf = ocaml_ldconf() in
  if ldconf <> "ignore" then begin
    let dll_dirs = remove_dups (read_ldconf ldconf) in
    let dll_pairs =
      List.flatten
	(List.map
	   (fun dll_dir ->
	      let files =
		try Array.to_list (Sys.readdir dll_dir)
		with _ ->
		  prerr_endline ("ocamlfind: [WARNING] Cannot read directory " ^
				 dll_dir ^ " which is mentioned in ld.conf");
		  []
	      in
	      List.map
		(fun file -> (file, dll_dir))
		(List.filter is_dll files)
	   )
	   dll_dirs
	) in
    let dll_hash = Hashtbl.create 50 in
    List.iter
      (fun (file, dll_dir) -> Hashtbl.add dll_hash file dll_dir)
      dll_pairs;
    Hashtbl.iter
      (fun file dll_dir ->
	 let locations = Hashtbl.find_all dll_hash file in
	 if List.length locations > 1 then begin
	   prerr_endline ("ocamlfind: [WARNING] The DLL " ^ file ^
			  " occurs in multiple directories: " ^ dll_dir)
	 end
      )
      dll_hash
  end
;;


let check_package_list l =
  (* may raise No_such_package *)
  List.iter
    (fun pkg ->
       let _ = package_directory pkg in
       ()
    )
    l
;;


type verbosity =
  | Normal
  | Verbose
  | Only_show


let run_command ?filter verbose cmd args =
  let printable_cmd =
    cmd ^ " " ^ String.concat " " (List.map escape_if_needed args) in
  ( match verbose with
      | Normal ->
          ()
      | Verbose ->
          print_endline ("+ " ^ printable_cmd);
          if filter <> None then
            print_string
              ("  (output of this command is filtered by ocamlfind)\n")
      | Only_show ->
          print_endline printable_cmd
  );
  flush stdout;

  if verbose <> Only_show then (
    let filter_input, cmd_output =
      match filter with
          None -> Unix.stdin (* dummy *), Unix.stdout
        | Some f -> Unix.pipe()
    in

    (* Signals: On SIGINT, we wait until the subprocess finishes, and
     * die then. This allows us to call interactive commands as subprocesses.
     *)

    let old_sigint =
      Sys.signal Sys.sigint Sys.Signal_ignore in

    let need_exe =
      List.mem Findlib_config.system [ "win32"; "win64"; "mingw"; "mingw64" ] in

    let fixed_cmd =
      if need_exe then (
        if Filename.check_suffix cmd ".exe" then cmd else cmd ^ ".exe" 
      )
      else
        cmd in

    let pid =
      Unix.create_process
        fixed_cmd
        (Array.of_list (cmd :: args))
        Unix.stdin
        cmd_output
        Unix.stderr
    in

    begin match filter with
        Some filter_fun ->
          begin
            Unix.close cmd_output;
            let ch = Unix.in_channel_of_descr filter_input in
            try
              while true do
                let line = input_line ch in
                match filter_fun line with
                    None -> ()       (* Suppress line *)
                  | Some line' -> print_endline line'
              done;
              assert false
            with
                End_of_file ->
                  close_in ch;
                  flush stdout
          end
      | None -> ()
    end;

    let (_,status) = Unix.waitpid [] pid in
    Sys.set_signal Sys.sigint old_sigint;
    begin
      match status with
        Unix.WEXITED 0 -> ()
      | Unix.WEXITED n ->
          if verbose = Verbose then
            print_string (cmd ^ " returned with exit code " ^ string_of_int n ^ "\n");
          exit n
      | Unix.WSIGNALED _ ->
          print_string (cmd ^ " got signal and exited\n");
          exit 2
      | Unix.WSTOPPED _ ->
          failwith "Your operating system does not work correctly"
    end
  )
;;


(**************** preprocessor ******************************************)

let select_pp_packages syntax_preds packages =
  if syntax_preds = [] then
    (* No syntax predicates, no preprocessor! *)
    []
  else
    List.filter
      (fun pkg ->
         let al = try package_property syntax_preds pkg "archive"
	          with Not_found -> "" in
         let w = Fl_split.in_words al in
	 w <> []
      )
      packages


let process_pp_spec syntax_preds packages pp_opts =
  (* Returns: pp_command *)
  (* may raise No_such_package *)

  (* [packages]: all packages given on the command line. May include
   * packages for compilation and for preprocessing.
   *
   * The difficulty is now that the preprocessor packages may have
   * requirements that are non-preprocessor packages. To get exactly
   * the preprocessor packages and its requirements, we do:
   *
   * 1. Determine the subset of [packages] that are preprocessor
   *    packages by checking whether they have an "archive" for
   *    [syntax_preds], i.e. the preprocessor packages mentioned
   *    on the command line = [cl_pp_packages].
   *
   * 2. Add their requirements = [pp_packages]
   *
   * Because the packages are now mixed, we must evaluate for 
   * [syntax_preds] + "byte".
   *)

  (* One packages must now have the variable "preprocessor", usually camlp4 *)
  let cl_pp_packages = select_pp_packages syntax_preds packages in
  let pp_packages =
    package_deep_ancestors syntax_preds cl_pp_packages in

  let preprocessor_cmds =
    List.flatten
      (List.map (fun pname ->
		   try
		     [ pname,
		       package_property syntax_preds pname "preprocessor"
		     ]
		   with
 		       Not_found -> []
		)
	        pp_packages
      )
  in

  let preprocessor_cmd =
    if syntax_preds <> [] then
      match preprocessor_cmds with
	  [] ->
	    failwith("Using -syntax, but no package is selected specifying \
                     a preprocessor as required for -syntax")
	| [_, cmd] -> Some cmd
	| _ ->
	    failwith("Several packages are selected that specify \
                      preprocessors: " ^ 
		       String.concat ", "
		      (List.map
			 (fun (n,v) ->
			    "package " ^ n ^ " defines `" ^ v ^ "'")
			 preprocessor_cmds
		      )
		    )
    else
      None
  in

  let pp_i_options =
    List.flatten
      (List.map
	 (fun pkg ->
	    let pkgdir = package_directory pkg in
	      [ "-I"; slashify pkgdir ]
	 )
	 pp_packages) in

  let pp_archives =
    if preprocessor_cmd = None then
      []
    else
      List.flatten
	(List.map
	   (fun pkg ->
	      let al = 
		try package_property ("byte" :: syntax_preds) pkg "archive"
	        with Not_found -> "" in
	      Fl_split.in_words al
	   )
	   pp_packages) in

  match preprocessor_cmd with
      None -> []
    | Some cmd ->
	["-pp";
	 cmd ^ " " ^
	 String.concat " " (List.map Filename.quote pp_i_options) ^ " " ^
	 String.concat " " (List.map Filename.quote pp_archives) ^ " " ^
	 String.concat " " (List.map Filename.quote pp_opts)]
;;

(**************** ppx extensions ****************************************)

let process_ppx_spec predicates packages ppx_opts =
  (* Returns: ppx_commands *)
  (* may raise No_such_package *)

  let ppx_packages =
    package_deep_ancestors predicates packages in

  let ppx_opts =
    List.map
      (fun opt ->
         match Fl_split.in_words opt with
           | pkg :: ((_ :: _) as opts) ->
               let exists =
                 try ignore(package_directory pkg); true
                 with No_such_package _ -> false in
               if not exists then
                 failwith ("The package named in -ppxopt does not exist: " ^
                             pkg);
               pkg, opts
           | _ ->
               failwith "-ppxopt must include package name, e.g. -ppxopt \"foo,-name bar\""
      )
      ppx_opts in

  let meta_ppx_opts =
    List.concat
      (List.map
        (fun pname ->
          try
            let opts = package_property predicates pname "ppxopt" in
            (* Split by whitespace to get (package,options) combinations.
               Then, split by commas to get individual options. *)
            List.map
              (fun opts ->
                match Fl_split.in_words opts with
                | pkg :: ((_ :: _) as opts) ->
                    let exists =
                      try ignore(package_directory pkg); true
                      with No_such_package _ -> false in
                    if not exists then
                      failwith ("The package named in ppxopt variable does not exist: " ^
                                  pkg ^ " (from " ^ pname ^ ")");
                    let base = package_directory pname in
                    pkg, List.map (resolve_path ~base ~explicit:true) opts
                | _ ->
                    failwith ("ppxopt variable must include package name, e.g. " ^
                              "ppxopt=\"foo,-name bar\" (from " ^ pname ^ ")")
              )
              (Fl_split.in_words_ws opts)
          with Not_found -> []
        )
        ppx_packages
      ) in

  List.flatten
    (List.map
       (fun pname ->
          let base = package_directory pname in
          let options =
            try
              List.concat
                (List.map (fun (_, opts) -> opts)
                  (List.filter (fun (pname', _) -> pname' = pname)
                    (meta_ppx_opts @ ppx_opts)))
            with Not_found -> []
          in
          try
            let preprocessor =
              resolve_path
                ~base ~explicit:true
                (package_property predicates pname "ppx") in
            ["-ppx"; String.concat " " (preprocessor :: options)]
          with Not_found -> []
       )
       ppx_packages)

(**************** Generic argument processing *************************)

let merge_native_arguments native_spec f_unit f_string f_special_list =
  List.map
    (fun (switch_name, switch_has_arg, help_text) ->
       let f =
	 try
	   List.assoc switch_name f_special_list
	 with
	     Not_found ->
	       if switch_has_arg then 
		 f_string switch_name 
	       else 
		 f_unit switch_name in
       (switch_name, f, help_text)
    )
    native_spec
;;


let parse_args
      ?(current = Arg.current) ?(args = Sys.argv) 
      ?(align = true)
      spec anon usage =
  try
    Arg.parse_argv
      ~current
      args
      (if align then Arg.align spec else spec)
      anon
      usage
  with
    | Arg.Help text ->
        print_string text;
        exit 0
    | Arg.Bad text ->
        prerr_string text;
        exit 2  


(************************* format expansion *************************)


let expand predicates eff_packages format =
  (* may raise No_such_package *)

    (* format:
     * %p         package name
     * %d         package directory
     * %m         META file
     * %D         description
     * %v         version
     * %a         archive file(s)
     * %A         archive files as single string
     * %o         link option(s)
     * %O         link options as single string
     *)

  List.flatten
    (List.map
       (fun pkg ->
	 let dir = package_directory pkg in
	    (* May raise No_such_package *)
	 let spec =
	   [ "%p",  [pkg];
             "%d",  [out_path dir];
             "%m",  [out_path (package_meta_file pkg)];
	     "%D",  [try package_property predicates pkg "description"
		     with Not_found -> "[n/a]"];
	     "%v",  [try package_property predicates pkg "version"
	             with Not_found -> "[unspecified]"];
	     "%a",  Fl_split.in_words
	              (try package_property predicates pkg "archive"
		       with Not_found -> "");
	     "%A",  [String.concat " "
		       (Fl_split.in_words
		          (try package_property predicates pkg "archive"
			   with Not_found -> ""))];
	     "%o",  Fl_split.in_words_ws
	             (try package_property predicates pkg "linkopts"
		      with Not_found -> "");
	     "%O",  [String.concat " "
		       (Fl_split.in_words_ws
		          (try package_property predicates pkg "linkopts"
			   with Not_found -> ""))];
	   ]
	 in
	 let lookup = package_property predicates pkg in
	 percent_subst ~base:dir spec lookup format)
       eff_packages)
;;


let help_format() =
  print_endline
    "Formats for -format strings:

    %p         package name
    %d         package directory
    %m         META file
    %D         description
    %v         version
    %a         archive file(s)
    %+a        archive file(s), converted to absolute paths
    %A         archive files as single string
    %+A        archive files as single string, converted to absolute paths
    %o         link option(s)
    %O         link options as single string
    %(name)    the value of the property <name>
    %+(name)   the value of the property <name>, converted to absolute paths
               (like <archive>)";
  flush stdout



(************************** QUERY SUBCOMMAND ***************************)

let query_package () =

  let long_format =
    "package:     %p\ndescription: %D\nversion:     %v\narchive(s):  %A\nlinkopts:    %O\nlocation:    %d\n" in
  let i_format =
    "-I %d" in
  let l_format =
    if Findlib_config.system = "win32" || Findlib_config.system = "win64" then
      (* Microsoft toolchain *)
      "-ccopt \"/link /libpath:%d\""
    else
      "-ccopt -L%d" in
  let a_format =
    "%+a" in
  let o_format =
    "%o" in
  let p_format =
    "%p" in

  let predicates = ref [] in
  let format = ref "%d" in
  let separator = ref "\n" in
  let prefix = ref "" in
  let suffix = ref "\n" in
  let recursive = ref false in
  let descendants = ref false in
  let pp = ref false in
  let qe = ref false in
  let qo = ref false in
  
  let packages = ref [] in

  let append_predicate s =
    let pl = Fl_split.in_words s in
    predicates := !predicates @ pl
  in


  parse_args
    [ "-predicates", Arg.String append_predicate,
                  "      specifies comma-separated list of assumed predicates";
      "-format", Arg.String (fun s -> format := s),
              "<fmt>      specifies the output format";
      "-separator", Arg.String (fun s -> separator := s),
                 "       specifies the string that separates multiple answers";
      "-prefix", Arg.String (fun s -> prefix := s),
              "<p>        a string printed before the first answer";
      "-suffix", Arg.String (fun s -> suffix := s),
              "<s>        a string printed after the last answer";
      "-recursive", Arg.Set recursive,
                 "       select direct and indirect ancestors/descendants, too";
      "-r", Arg.Set recursive,
         "               same as -recursive";
      "-descendants", Arg.Unit (fun () ->  descendants := true; recursive := true),
                   "     query descendants instead of ancestors; implies -recursive";
      "-d", Arg.Unit (fun () ->  descendants := true; recursive := true),
         "               same as -descendants";
      "-pp", Arg.Unit (fun () -> pp := true; recursive := true),
          "              get preprocessor pkgs (predicates are taken as syntax preds)";
      "-long-format", Arg.Unit (fun () -> format := long_format),
                   "     specifies long output format";
      "-l", Arg.Unit (fun () -> format := long_format),
         "               same as -long-format";
      "-i-format", Arg.Unit (fun () -> format := i_format),
                "        prints -I options for ocamlc";
      "-l-format", Arg.Unit (fun () -> format := l_format),
                "        prints -ccopt -L options for ocamlc";
      "-a-format", Arg.Unit (fun () -> format := a_format),
                "        prints names of archives to be linked in for ocamlc";
      "-o-format", Arg.Unit (fun () -> format := o_format),
                "        prints link options for ocamlc";
      "-p-format", Arg.Unit (fun () -> format := p_format),
                "        prints package names";
      "-help-format", Arg.Unit help_format,
                   "     lists the supported formats for -format";
      "-qe", Arg.Set qe,
          "              do not print most errors, just set the exit code";
      "-qo", Arg.Set qo,
          "              do not print regular output";
    ]
    (fun p -> packages := !packages @ Fl_split.in_words p)
"usage: ocamlfind query [ -predicates <p>  | -format <f> |
                         -long-format     | -i-format   |
                         -l-format        | -a-format   |
			 -o-format        | -p-format   |
                         -prefix <p>      | -suffix <s> |
                         -separator <s>   |
                         -descendants     | -recursive  ] package ...";

  ignore(config_file());  (* ensure findlib is initialized *)
  try
    let predicates1 =
      if !pp then
        "preprocessor" :: "syntax" :: !predicates
      else
        !predicates in
    let packages1 =
      if !pp then
        let predicates2 =
          List.filter (fun p -> p <> "byte" && p <> "native") predicates1 in
        select_pp_packages predicates2 !packages
      else
        !packages in
    let eff_packages =
      if !recursive then begin
        if !descendants then
          Fl_package_base.package_users predicates1 packages1
        else
          package_deep_ancestors predicates1 packages1
      end
      else
        packages1
    in

    let answers = expand predicates1 eff_packages !format in

    if not !qo then (
      print_string !prefix;
      print_string (String.concat !separator answers);
      print_string !suffix;
    )
  with
    ( Findlib.No_such_package _
    | Failure _
    | Sys_error _
    ) when !qe -> raise Silent_error
;;


(**************** OCAMLC/OCAMLMKTOP/OCAMLOPT subcommands ****************)

type pass_file_t =
    Pass of string
  | Impl of string   (* Forces module implementation: -impl <file> *)
  | Intf of string   (* Forces module interface: -intf <file> *)
  | Cclib of string  (* Option for the C linker: -cclib <opt> *)
;;


let contracted_ocamlmklib_options =
  [ "-l"; "-L"; "-R"; "-F"; "-Wl,-rpath,"; "-Wl,-R" ]
    (* The ocamlmklib options where the argument is directly attached to the
       switch (e.g. -L<path> instead of -L <path>)
     *)


let ocamlc which () =

  (* let destdir = ref (default_location()) in *)
  
  let switches = ref [] in
  let pass_options = ref [] in
  let pass_files = ref [] in
  let incpath = ref [] in
  let only_show = ref false in

  let dll_pkgs = ref [] in
  let dll_pkgs_all = ref false in

  let linkpkg = ref false in

  let packages = ref [] in
  let predicates = ref [] in
  let dontlink = ref [] in

  let syntax_preds = ref [] in
  let pp_opts = ref [] in
  let ppx_opts = ref [] in
  let pp_specified = ref false in

  let type_of_threads =
    try package_property [] "threads" "type_of_threads"
    with Not_found -> "ignore"
  in
  let threads_default =
    match type_of_threads with
	"posix" -> `POSIX_threads
      | "vm"    -> `VM_threads
      | _       -> `None
  in
  let threads = ref `None in
  let support_threads() =
    if threads_default = `None then
      failwith "threading is not supported on this platform" in

  let add_switch name =
    Arg.Unit (fun () ->
                switches := name :: !switches;
                pass_options := !pass_options @ [name]) in
  let add_spec_fn name s =
    pass_options := !pass_options @ [name; s] in
  let add_spec name = 
    Arg.String (add_spec_fn name) in
  let add_contracted_spec_fn name s =
    pass_options := !pass_options @ [name ^ s] in
  let add_contracted_spec name =
    Arg.String (add_contracted_spec_fn name) in
  let add_pkg =
    Arg.String (fun s -> packages := !packages @ (Fl_split.in_words s)) in
  let add_pred =
    Arg.String (fun s -> predicates := !predicates @ (Fl_split.in_words s)) in
  let add_dontlink =
    Arg.String (fun s -> dontlink := !dontlink @ (Fl_split.in_words s)) in
  let add_syntax_pred =
    Arg.String (fun s -> syntax_preds := !syntax_preds @ (Fl_split.in_words s)) in
  let add_pp_opt =
    Arg.String (fun s -> pp_opts := !pp_opts @ [s]) in
  let add_dll_pkg =
    Arg.String (fun s -> dll_pkgs := !dll_pkgs @ (Fl_split.in_words s)) in
  let ignore_error = ref false in

  let native_spec_opt =
    match which with
      | "ocamlc"     -> Ocaml_args.ocamlc_spec
      | "ocamlcp"    -> Ocaml_args.ocamlcp_spec
      | "ocamlmklib" -> Ocaml_args.ocamlmklib_spec
      | "ocamlmktop" -> Ocaml_args.ocamlmktop_spec
      | "ocamlopt"   -> Ocaml_args.ocamlopt_spec
      | "ocamloptp"  -> Ocaml_args.ocamloptp_spec
      | _            -> None in
  let native_spec =
    match native_spec_opt with
      | None -> failwith ("Not supported in your configuration: " ^ which)
      | Some s -> s in

  let arg_spec =
    List.flatten
      [ [
          "-package", add_pkg,
            "<name>   Refer to package when compiling";
          "-linkpkg", Arg.Set linkpkg,
            "          Link the packages in";
          "-predicates", add_pred,
            "<p>   Add predicate <p> when resolving package properties";
          "-dontlink", add_dontlink,
            "<name>  Do not link in package <name> and its ancestors";
          "-syntax", add_syntax_pred,
            "<p>       Use preprocessor with predicate <p>";
          "-ppopt", add_pp_opt,
            "<opt>      Append option <opt> to preprocessor invocation";
          "-ppxopt", Arg.String (fun s -> ppx_opts := !ppx_opts @ [s]),
            "<pkg>,<opts>  Append options <opts> to ppx invocation for package <pkg>";
          "-dllpath-pkg", add_dll_pkg,
            "<pkg> Add -dllpath for this package";
          "-dllpath-all", Arg.Set dll_pkgs_all,
            "      Add -dllpath for all linked packages";
          "-ignore-error", Arg.Set ignore_error,
            "     Ignore the 'error' directive in META files";
          "-passopt", Arg.String (fun s -> pass_options := !pass_options @ [s]),
            "<opt>    Pass option <opt> directly to ocamlc/opt/mklib/mktop";
          "-passrest", Arg.Rest (fun s -> pass_options := !pass_options @ [s]),
            "         Pass all remaining options directly";
          "-only-show", Arg.Set only_show,
            "         Only show the constructed command, but do not exec it\nSTANDARD OPTIONS:";
        ];

        merge_native_arguments 
	  native_spec 
	  add_switch
	  add_spec
          (
	    [ "-cclib", 
	      Arg.String (fun s -> pass_files := !pass_files @ [ Cclib s ]);
              
	      "-I", (Arg.String
		       (fun s ->
		          let s = resolve_path s in
                          if Sys.file_exists s then incpath := s :: !incpath;  (* reverted below *)
		          add_spec_fn "-I" (slashify s) ));
              
	      "-impl", 
	      Arg.String (fun s -> pass_files := !pass_files @ [ Impl(slashify s) ]);
              
	      "-intf", 
	      Arg.String (fun s -> pass_files := !pass_files @ [ Intf(slashify s) ]);
              
	      "-pp", 
	      Arg.String (fun s -> pp_specified := true; add_spec_fn "-pp" s);
	      
	      "-thread", 
              Arg.Unit (fun _ -> support_threads(); threads := threads_default);
            
	      "-vmthread", 
              Arg.Unit (fun _ -> support_threads(); threads := `VM_threads);
              
	      "-", 
	      Arg.String (fun s -> pass_files := !pass_files @  [ Pass s ]);
              
	    ] @
            if which = "ocamlmklib" then
              List.map
                (fun opt ->
                   (opt, add_contracted_spec opt)
                )
                contracted_ocamlmklib_options
            else
              []
          )
      ] in

  let (current,args) =
    if which = "ocamlmklib" then
      (* Special processing for -L, -R etc. *)
      let c = !(Arg.current) in
      let l = Array.length Sys.argv in
      let args1 = Array.sub Sys.argv (c+1) (l-c-1) in
      let args2 =
        Array.append
          [| Sys.argv.(0) |]
          (Fl_args.rewrite_contracted_args
             arg_spec
             contracted_ocamlmklib_options
             args1
          ) in
      (ref 0, args2)
    else
      (Arg.current, Sys.argv) in

  parse_args
    ~current
    ~args
    arg_spec
    (fun s -> pass_files := !pass_files @ [ Pass s])
    ("usage: ocamlfind " ^ which ^ " [options] file ...");

  (* ---- Start requirements analysis ---- *)
  
  begin match which with
    "ocamlc"     -> predicates := "byte" :: !predicates;
  | "ocamlcp"    -> predicates := "byte" :: !predicates;
  | "ocamlmklib" -> predicates := "byte" :: "native" :: !predicates;
  | "ocamlmktop" -> predicates := "byte" :: "create_toploop" :: !predicates;
  | "ocamlopt"   -> predicates := "native" :: !predicates;
  | "ocamloptp"  -> predicates := "native" :: !predicates;
  | _            -> failwith "unsupported backend"
  end;

  incpath := List.rev !incpath;

  ( match !threads with
	`None ->
	  ()

      | `VM_threads ->
	  if which = "ocamlopt" then
	    failwith "ocamlopt does not support multi-threaded programs for your configuration";
	  pass_options := !pass_options @ [ "-vmthread" ];
	  predicates := "mt" :: "mt_vm" :: !predicates;

      | `POSIX_threads ->
	  pass_options := !pass_options @ [ "-thread" ];
	  predicates := "mt" :: "mt_posix" :: !predicates;
  );

  if List.mem "-p" !switches then
    predicates := "gprof" :: !predicates;

  if Findlib_config.ocaml_has_autolinking &&
     not (List.mem "-noautolink" !switches)
  then
    predicates := "autolink" :: !predicates;

  if !syntax_preds <> [] then begin
    predicates := "syntax" :: !predicates;
    syntax_preds := "preprocessor" :: "syntax" :: !syntax_preds;
  end;

  let verbose = 
    if List.mem "-verbose" !switches then Verbose else
      if !only_show then Only_show else
        Normal in

  if !pp_specified && !syntax_preds <> [] then
    prerr_endline("ocamlfind: [WARNING] -pp overrides the effect of -syntax partly");

  (* check packages: *)
  check_package_list !packages;
  check_package_list !dontlink;

  let eff_packages =
    package_deep_ancestors !predicates !packages in

  let eff_dontlink =
    package_deep_ancestors !predicates !dontlink in

  let eff_link =
    List.flatten
      (List.map
	 (fun pkg -> if List.mem pkg eff_dontlink then [] else [pkg])
	 eff_packages) in


  let eff_packages_dl =
    remove_dups (List.map package_directory eff_packages) in

  let eff_link_dl =
    remove_dups (List.map package_directory eff_link) in

  (* Conflict report: *)
  conflict_report (!incpath @ ["."; Findlib.ocaml_stdlib() ]) eff_packages;

  (* ---- End of requirements analysis ---- *)

  (* Add the pkg_<name> predicates: *)
  predicates := List.map (fun pkg -> "pkg_" ^ pkg) eff_packages @ !predicates;

  (* Check on [warning] directives: *)
  List.iter
    (fun pkg ->
       try
         let warning = package_property !predicates pkg "warning" in
         prerr_endline("ocamlfind: [WARNING] Package `" ^ pkg ^
                         "': " ^ warning)
       with
	   Not_found -> ()
    )
    eff_packages;

  (* Check on [error] directives: *)
  List.iter
    (fun pkg ->
       try
	 let error = package_property !predicates pkg "error" in
	 if !ignore_error then
	   prerr_endline("ocamlfind: [WARNING] Package `" ^ pkg ^
			 "' signals error: " ^ error)
	 else
	   failwith ("Error from package `" ^ pkg ^ "': " ^ error)
       with
	   Not_found -> ()
    )
    eff_packages;

  if verbose = Verbose then begin
    if !syntax_preds <> [] then
      print_string ("Effective set of preprocessor predicates: " ^
		    String.concat "," !syntax_preds ^ "\n");
    print_string ("Effective set of compiler predicates: " ^
		  String.concat "," !predicates ^ "\n");
  end;

  let stdlibdir = Fl_split.norm_dir (Findlib.ocaml_stdlib()) in
  let threads_dir = Filename.concat stdlibdir "threads" in
  let vmthreads_dir = Filename.concat stdlibdir "vmthreads" in

  let create_toploop =
    List.mem "create_toploop" !predicates && List.mem "findlib" eff_link in
  let have_dynload =
    List.mem "findlib.dynload" eff_link in
  let initl_file_needed =
    create_toploop || have_dynload in

  let initl_file_name =
    if initl_file_needed then
      Filename.temp_file "findlib_initl" ".ml"
    else
      ""
  in

  (* initl_file_name: the initialization code inserted at the end of
   *   the cma/cmo list (initl = init last)
   *)

  if initl_file_needed then begin
    (* Generate initializer for "findlib_top.cma" *)
    let initl = open_out_gen
		  [Open_wronly; Open_trunc; Open_text]
		  0o777
		  initl_file_name in
    try
      List.iter
        (fun pkg ->
           Printf.fprintf
             initl
             "Findlib.record_package Findlib.Record_core %S;;\n"
             pkg
        )
        eff_packages;
      output_string initl
	("Findlib.record_package_predicates [" ^
	 String.concat ";"
	   (List.map
	      (fun pred -> "\"" ^ String.escaped pred ^ "\"")
              !predicates
           ) ^
	   "];;\n");
      close_out initl;
    with
      any ->
	close_out initl;
	Sys.remove initl_file_name;
	raise any
  end;

  if initl_file_needed && verbose <> Only_show then
    at_exit
      (fun () ->
	let tr f x = try f x with _ -> () in
	tr Sys.remove initl_file_name;
	tr Sys.remove (Filename.chop_extension initl_file_name ^ ".cmi");
	tr Sys.remove (Filename.chop_extension initl_file_name ^ ".cmo");
      );

  let exclude_list = [ stdlibdir; threads_dir; vmthreads_dir ] in
  (* Don't generate -I options for these directories because there is
   * also some magic in ocamlc/ocamlopt that would not work otherwise
   *)

  let i_options =
    List.flatten
      (List.map
	 (fun pkgdir ->
	    let npkgdir = Fl_split.norm_dir pkgdir in
	    if List.mem npkgdir exclude_list then
	      []
	    else
	      [ "-I"; slashify pkgdir;
		(* "-ccopt"; out_path ~prefix:"-I" pkgdir; -- see comment *)
	      ])
	 eff_packages_dl) in
  (* We no longer emit -ccopt options, because ocamlc/ocamlopt already
     do that for each -I if the C compiler needs to be invoked
     (so far I tracked it, ocamlc/ocamlopt have always done this, even
      back in 1996).
   *)

  let l_options = [] in
  (* Also, no longer -ccopt -L options. Current ocamlc/ocamlopt do that
     for each -I option passed to them anyway, so we can omit that here.
     See ocaml change (quite old, but I was not aware of it):
      http://camlcvs.inria.fr/cgi-bin/cvsweb/ocaml/asmcomp/asmlink.ml.diff?r1=1.38;r2=1.39
   *)
(*
  let l_options =
    List.flatten
      (List.map
	 (fun pkgdir ->
	    let npkgdir = Fl_split.norm_dir pkgdir in
	    if List.mem npkgdir exclude_list then
	      []
	    else
	      if Findlib_config.system = "win32" || Findlib_config.system = "win64" then
		(* Microsoft toolchain *)
		[ "-ccopt"; out_path ~prefix:"/link /libpath:" pkgdir ]
	      else
		[ "-ccopt"; out_path ~prefix:"-L" pkgdir; ])
	 eff_link_dl) in
 *)

  let archives =
    List.flatten
      (List.map
	 (fun pkg ->
	   let al = try package_property !predicates pkg "archive"
	            with Not_found -> "" in
           let al_ext =
             if have_dynload && pkg = "findlib.dynload" then
               [ initl_file_name ]
             else
               [] in
	   let pkg_dir =
	     if pkg = "threads" then   (* MAGIC *)
	       match !threads with
		   `None -> stdlibdir
		 | `VM_threads -> vmthreads_dir
		 | `POSIX_threads -> threads_dir
	     else
	       package_directory pkg in
	   let pkg_dir = slashify pkg_dir in
	   List.map
	     (fun arch ->
		resolve_path ~base:pkg_dir arch)
	     (Fl_split.in_words al @ al_ext)
	 )
	 eff_link)
    @
    (if create_toploop then
       [ initl_file_name ]
     else
       []
    )
  in

  let linkopts =
    List.flatten
      (List.map
	 (fun pkg ->
	   let ol = try package_property !predicates pkg "linkopts"
	            with Not_found -> "" in
	   Fl_split.in_words_ws ol)
	 (List.rev eff_link)) in

  let pp_command =
    if !pp_specified then
      []
    else
      process_pp_spec !syntax_preds !packages !pp_opts
  in

  let ppx_commands =
    process_ppx_spec !predicates !packages !ppx_opts
  in

  let pass_files' =
    List.flatten
      (List.map
	 (function
	      Pass s ->
		if s <> "" && s.[0] = '-'
		then [ "-"; String.sub s 1 (String.length s - 1) ]
		else [ resolve_path s ]
	    | Impl s ->
		[ "-impl"; resolve_path s ]
	    | Intf s ->
		[ "-intf"; resolve_path s ]
	    | Cclib s ->
		[ "-cclib"; s ]
	 )
	 !pass_files)
  in

  let dll_dirs =
    remove_dups
      ((List.map package_directory !dll_pkgs) @ 
       (if !dll_pkgs_all then eff_link_dl else [])) in

  let dll_options =
    List.flatten
      (List.map
	 (fun pkg -> ["-dllpath";  slashify pkg] )
	 dll_dirs) in

  let mklib_options =
    ["-ocamlc"; Findlib.command `ocamlc;
     "-ocamlopt"; Findlib.command `ocamlopt] in

  let arguments =
    (if which = "ocamlmklib" then mklib_options else []) @
    !pass_options @    (* other options from the command line *)
    i_options @        (* Generated -I options from package analysis *)
    pp_command @       (* Optional preprocessor command *)
    ppx_commands @     (* Optional ppx extension commands *)
    (if !linkpkg then l_options else []) @  (* Generated -ccopt -L options *)
    (if !linkpkg then archives else []) @   (* Gen file names to link *)
    pass_files' @                           (* File names from cmd line *)
    (if !linkpkg then linkopts else []) @   (* Generated link options *)
    dll_options                             (* Generated -dllpath options *)
  in

  let actual_command =
    match which with
	"ocamlc"     -> Findlib.command `ocamlc
      | "ocamlopt"   -> Findlib.command `ocamlopt
      | "ocamlcp"    -> Findlib.command `ocamlcp
      | "ocamlmklib" -> Findlib.command `ocamlmklib
      | "ocamlmktop" -> Findlib.command `ocamlmktop
      | "ocamloptp"  -> Findlib.command `ocamloptp
      | _            -> assert false
  in

  run_command verbose actual_command arguments
;;


(************************************************************************)

let ocamldoc() =

  let packages = ref [] in
  let predicates = ref [] in
  let syntax_preds = ref [] in
  let pp_opts = ref [] in
  let ppx_opts = ref [] in
  let pp_specified = ref false in

  let verbose = ref Normal in

  let options = ref [] in

  let native_spec =
    match Ocaml_args.ocamldoc_spec with
      | None -> failwith "Not supported in your configuration: ocamldoc"
      | Some s -> s in

  parse_args
    ~align:false
    ( Arg.align
        [ "-package",
	  Arg.String (fun s -> 
		        packages := !packages @ Fl_split.in_words s),
	  "<name>  Add this package to the search path";
          
	  "-predicates",
	  Arg.String (fun s ->
		        predicates := !predicates @ Fl_split.in_words s),
	  "<p>  Add predicate <p> when calculating dependencies";

	  "-syntax",
	  Arg.String (fun s ->
		        syntax_preds := !syntax_preds @ Fl_split.in_words s),
	  "<p>  Use preprocessor with predicate <p>";

	  "-ppopt",
	  Arg.String (fun s -> pp_opts := !pp_opts @ [s]),
	  "<opt>  Append option <opt> to preprocessor invocation";

          "-ppxopt",
          Arg.String (fun s -> ppx_opts := !ppx_opts @ [s]),
          "<pkg>,<opts>  Append options <opts> to ppx invocation for package <pkg>";
 
	  "-thread",
	  Arg.Unit (fun () -> predicates := "mt" :: "mt_posix" :: !predicates),
	  "  Assume kernel multi-threading when doing dependency analyses";
          
	  "-vmthread",
	  Arg.Unit (fun () -> predicates := "mt" :: "mt_vm" :: !predicates),
	  "  Assume bytecode multi-threading when doing dependency analyses";
          
          "-passopt",
          Arg.String (fun s -> options := !options @ [s]),
          "<opt>  Pass this option directly to ocamldoc";
          
          "-passrest",
          Arg.Rest (fun s -> options := !options @ [s]),
          "  Pass all remaining options directly to ocamldoc";

          "-only-show",
          Arg.Unit (fun () -> verbose := Only_show),
          "  Only show the constructed command but do not exec it";
          
	  "-verbose",
	  Arg.Unit (fun () -> verbose := Verbose),
	  "  Be verbose\nSTANDARD OPTIONS:";
        ]
      @
      ( merge_native_arguments
	  native_spec
	  (fun s -> 
	     Arg.Unit (fun () ->
			 options := !options @ [s]))
	  (fun s ->
	     Arg.String (fun arg ->
			   options := !options @ [s; arg]))
	  [ "-v", Arg.Unit (fun () -> verbose := Verbose);
	    "-pp", Arg.String (fun s ->
				 pp_specified := true;
				 options := !options @ ["-pp"; s]);
	  ]
      )
    )
    (fun s -> options := !options @ [s])
    "usage: ocamlfind ocamldoc <options> <files>...";

  check_package_list !packages;

  if !syntax_preds <> [] then (
    predicates := "syntax" :: !predicates;
    syntax_preds := "preprocessor" :: "syntax" :: !syntax_preds;
  );

  if !verbose = Verbose then begin
    if !syntax_preds <> [] then
      print_string ("Effective set of preprocessor predicates: " ^
		    String.concat "," !syntax_preds ^ "\n");
    print_string ("Effective set of compiler predicates: " ^
		  String.concat "," !predicates ^ "\n");
  end;

  if !pp_specified && !syntax_preds <> [] then
    prerr_endline("Warning: -pp overrides the effect of -syntax partly");

  let pp_command =
    if !pp_specified then
      []
    else
      process_pp_spec !syntax_preds !packages !pp_opts
  in

  let ppx_commands =
    process_ppx_spec !predicates !packages !ppx_opts
  in

  let eff_packages =
    package_deep_ancestors !predicates !packages in

  (* Check on [error] directives (turned into warnings): *)
  List.iter
    (fun pkg ->
       try
	 let error = package_property !predicates pkg "error" in
	 prerr_endline("ocamlfind: [WARNING] Package `" ^ pkg ^
			 "' signals error: " ^ error)
       with
	   Not_found -> ()
    )
    eff_packages;

  let eff_packages_dl =
    remove_dups (List.map package_directory eff_packages) in

  let arguments =
    (List.flatten (List.map (fun d -> [ "-I"; slashify d ]) eff_packages_dl)) @
    pp_command @
    ppx_commands @
    !options in

  let actual_command = Findlib.command `ocamldoc in

  run_command !verbose actual_command arguments
;;


(************************************************************************)

(* From ocamldep source code: *)
let depends_on_char, continuation_char =
  match Sys.os_type with
  | "Unix" | "Win32" | "Cygwin" -> ':', '\\'
  | "MacOS" -> '\196', '\182'
  | _ -> assert false
;;


let suppress_targets suffix =
  (* If [line] begins with "target: dependencies ...", and [target] is a
   * file name ending in [suffix], this line is suppressed, and all
   * follow-up lines.
   *)
  let do_suppress = ref false in
  fun line ->
    let target =
      try
	let k = String.index_from line 0 depends_on_char in (* or Not_found *)
	let target_string = String.sub line 0 k in
	if String.contains target_string ' ' then raise Not_found;
	Some target_string
      with
	  Not_found -> None
    in
    begin match target with
	Some target_string ->
	  do_suppress := Filename.check_suffix target_string suffix;
      | None ->
	  ()
    end;
    if !do_suppress then
      None
    else
      Some line
;;


let ocamldep () =

  let switches = ref [] in
  let pass_options = ref [] in
  let pass_files = ref [] in

  let packages = ref [] in
  let predicates = ref [] in
  let syntax_preds = ref [] in
  let pp_opts = ref [] in
  let ppx_opts = ref [] in
  let pp_specified = ref false in

  let verbose = ref Normal in
  let native_filter = ref false in
  let bytecode_filter = ref false in

  let add_switch name =
    Arg.Unit (fun () ->
                switches := name :: !switches;
                pass_options := !pass_options @ [name]) in
  let add_spec_fn name s =
    pass_options := !pass_options @ [name; s] in
  let add_spec name = Arg.String (add_spec_fn name) in
  let add_pred =
    Arg.String (fun s -> predicates := !predicates @ (Fl_split.in_words s)) in
  let add_syntax_pred =
    Arg.String (fun s -> syntax_preds := !syntax_preds @ (Fl_split.in_words s)) in
  let add_pp_opt =
    Arg.String (fun s -> pp_opts := !pp_opts @ [s]) in
  let add_pkg =
    Arg.String (fun s -> packages := !packages @ (Fl_split.in_words s)) in

  let native_spec =
    match Ocaml_args.ocamldep_spec with
      | None -> failwith "Not supported in your configuration: ocamldep"
      | Some s -> s in

  parse_args
    ( [
	"-syntax", add_syntax_pred,
                "<p>       Use preprocessor with predicate <p>";
	"-package", add_pkg,
	         "<p>      Add preprocessor package <p>";
        "-predicates", add_pred,
                    "<p>  Add predicate <p> when calculating dependencies";
	"-ppopt", add_pp_opt,
               "<opt>      Append option <opt> to preprocessor invocation";
  "-ppxopt", Arg.String (fun s -> ppx_opts := !ppx_opts @ [s]),
          "<pkg>,<opts>  Append options <opts> to ppx invocation for package <pkg>";
	"-passopt", Arg.String (fun s -> pass_options := !pass_options @ [s]),
                 "<opt>    Pass option <opt> directly to ocamlc/opt/mktop";
        "-passrest", Arg.Rest (fun s -> pass_options := !pass_options @ [s]),
                  "         Pass all remaining options directly";
	"-native-filter", Arg.Set native_filter,
	               "    Output only dependencies for native code (implies -native)";
	"-bytecode-filter", Arg.Set bytecode_filter,
	                 "  Output only dependencies for bytecode";
        "-only-show",  Arg.Unit (fun () -> verbose := Only_show),
                   "        Only show the constructed command but do not exec it";
          
	"-verbose", Arg.Unit (fun () -> verbose := Verbose),
	         "          Print calls to external commands\nSTANDARD OPTIONS:";
      ]
      @
	( merge_native_arguments
	    native_spec
	    add_switch
	    add_spec
	    [ "-I",
	      Arg.String (fun s -> add_spec_fn "-I" (slashify (resolve_path s)));

	      "-pp", Arg.String (fun s -> pp_specified := true;
		 	           add_spec_fn "-pp" s);
	    ]
	)
    )
    (fun s -> pass_files := !pass_files @ [ s])
    ("usage: ocamlfind ocamldep [options] file ...");

  check_package_list !packages;

  if !native_filter && !bytecode_filter then
    failwith "The options -native-filter and -bytecode-filter are incompatible";

  if !native_filter && not (List.mem "-native" !switches) then
    pass_options := "-native" :: !pass_options;

  if !syntax_preds <> [] then
    syntax_preds := "preprocessor" :: "syntax" :: !syntax_preds;

  if !verbose = Verbose && !syntax_preds <> [] then
    print_string ("Effective set of preprocessor predicates: " ^
		  String.concat "," !syntax_preds ^ "\n");

  if !pp_specified && !syntax_preds <> [] then
    prerr_endline("Warning: -pp overrides the effect of -syntax partly");

  let pp_command =
    if !pp_specified then
      []
    else
      process_pp_spec !syntax_preds !packages !pp_opts
  in

  let ppx_commands =
    process_ppx_spec !predicates !packages !ppx_opts
  in

  let arguments =
    !pass_options @
    pp_command @
    ppx_commands @
    !pass_files
  in

  let actual_command = Findlib.command `ocamldep in
  let filter =
    if !native_filter then
      (* Suppress when target is ".cmo": *)
      Some (suppress_targets ".cmo")
    else
      if !bytecode_filter then
	(* Suppress when target is ".cmx": *)
	Some (suppress_targets ".cmx")
      else
	None
  in

  run_command ?filter !verbose actual_command arguments
;;


(************************************************************************)

let ocamlbrowser () =
  (* let switches = ref [] in *)
  let pass_options = ref [] in
  let add_all = ref false in

  let packages = ref [] in

(*
  let add_switch name =
    Arg.Unit (fun () ->
                switches := name :: !switches;
                pass_options := !pass_options @ [name]) in
 *)
  let add_spec_fn name s =
    pass_options := !pass_options @ [name; s] in
(* let add_spec name = Arg.String (add_spec_fn name) in *)
  let add_pkg =
    Arg.String (fun s -> packages := !packages @ (Fl_split.in_words s)) in

  parse_args
      [
	"-I", Arg.String (fun s -> add_spec_fn "-I" (slashify(resolve_path s))),
           "<dir>          Add <dir> to the list of include directories";
	"-all", Arg.Set add_all,
	     "              Add all packages to include path";
	"-package", add_pkg,
	         "<p>      Add package <p> to include path";
	"-passopt", Arg.String (fun s -> pass_options := !pass_options @ [s]),
                 "<opt>    Pass option <opt> directly to ocamlbrowser";
        "-passrest", Arg.Rest (fun s -> pass_options := !pass_options @ [s]),
                 "          Pass all remaining options directly";
      ]
      (fun s -> raise (Arg.Bad ("Unexpected argument: " ^ s)))
      ("usage: ocamlfind ocamlbrowser [options] file ...");

  if !add_all then packages := Fl_package_base.list_packages();
  check_package_list !packages;

  let arguments =
    !pass_options @
    (List.flatten
       (List.map
	  (fun pkg ->
	     let dir = Findlib.package_directory pkg in
	     [ "-I"; slashify dir ]
	  )
	  !packages
       )
    )
  in

  let actual_command = Findlib.command `ocamlbrowser in

  run_command Normal actual_command arguments
;;


(************************************************************************)


let copy_file ?(rename = (fun name -> name)) ?(append = "") src dstdir =
  (* A system-independent function to copy the file src to dstdir *)
  let outname = rename (Filename.basename src) in
  let ch_in = open_in_bin src in
  (* Determine the permissions of the file: the permissions of the
   * user bits are extended to all groups (user, group, world bits),
   * and the umask is applied to the result.
   * Furthermore, the mtime of the file is preserved. This seems to be
   * important for BSD-style archives (otherwise the system is confused
   * and wants that ranlib is run again). For simplicity, the atime is
   * set to the mtime, too.
   *)
  let s = Unix.stat src in
  let perm = s.Unix.st_perm in
  let user_perm = (perm land 0o700) lsr 6 in
  let perm' = user_perm lor (user_perm lsl 3) lor (user_perm lsl 6) in
  try
    let outpath = Filename.concat dstdir outname in
    if Sys.file_exists outpath then
      prerr_endline ("ocamlfind: [WARNING] Overwriting file " ^ outpath);
    let ch_out = open_out_gen
		   [Open_wronly; Open_creat; Open_trunc; Open_binary]
		   perm'
		   outpath in
    try
      let buflen = 4096 in
      let buf = String.create buflen in   (* FIXME: Bytes.create *)
      let pos = ref 0 in
      let len = ref (input ch_in buf 0 buflen) in
      while !len > 0 do
	output ch_out buf !pos !len;
	len := input ch_in buf !pos buflen;
      done;
      output_string ch_out append;
      close_out ch_out;
      close_in ch_in;
      Unix.utimes outpath s.Unix.st_mtime s.Unix.st_mtime;

      prerr_endline("Installed " ^ outpath);
    with
	exc -> close_out ch_out; raise exc
  with
      exc -> close_in ch_in; raise exc
;;


let install_create_directory pkgname dstdir =
  try
    Unix.mkdir dstdir 0o777
  with
      Unix.Unix_error(Unix.EEXIST,_,_) ->
	()
    | Unix.Unix_error(Unix.ENOENT,_,_)
    | Unix.Unix_error(Unix.ENOTDIR,_,_) ->
	failwith ("Bad configuration: Cannot mkdir " ^ dstdir ^ " because a path component does not exist or is not a directory")
    | Unix.Unix_error(e,_,_) ->
	failwith ("Cannot mkdir " ^ dstdir ^ ": " ^
		  Unix.error_message e)
;;


let create_owner_file pkg file =
  let outpath = file ^ ".owner" in
  let f = open_out outpath in
  try
    output_string f (pkg ^ "\n");
    close_out f;
    prerr_endline("Installed " ^ outpath);
  with
      exc -> close_out f; raise exc
;;

let trim_cr s =
  let len = String.length s in
  if len > 0 && String.get s (len-1) = '\r' then
    String.sub s 0 (len-1)
  else
    s

let find_owned_files pkg dir =
  let files = Array.to_list(Sys.readdir dir) in
  List.filter
    (fun file ->
       let owner_file =
	 if Filename.check_suffix file ".owner" then
	   file
	 else
	   file ^ ".owner" in
       (List.mem owner_file files) && (
         try
           let fd =
             Unix.openfile (Filename.concat dir owner_file) [Unix.O_RDONLY] 0 in
           let f =
             Unix.in_channel_of_descr fd in
           try
             let line = trim_cr (input_line f) in
             let is_my_file = (line = pkg) in
             close_in f;
             is_my_file
           with
             | End_of_file -> close_in f; false
             | exc -> close_in f; raise exc
         with
           | Unix.Unix_error(Unix.ENOENT,_,_) ->
               (* the owner file might have been removed by a package
                  removal that is being done in parallel
                *)
               false
           | Unix.Unix_error(code, _, arg) ->
               raise(sys_error code arg)
       )
    )
    files
;;



exception Missing_archives of Fl_metascanner.pkg_expr

let rec patch_archives pkgdir pkg =
  (* First remove all missing files from archive variables: *)
  let defs' =
    List.map
      (fun def ->
	 if def.Fl_metascanner.def_var = "archive" then (
	   let files = Fl_split.in_words def.Fl_metascanner.def_value in
	   let files' =
	     List.filter
	       (fun file -> 
		  let p = Findlib.resolve_path ~base:pkgdir file in
		  Sys.file_exists p)
	       files in
	   { def with
	       Fl_metascanner.def_value = String.concat " " files'
	   }
	 )
	 else def
      )
      pkg.Fl_metascanner.pkg_defs in
  (* Remove empty archive variables: *)
  let defs'' =
    List.filter
      (fun def ->
	 def.Fl_metascanner.def_var <> "archive" ||
	 Fl_split.in_words def.Fl_metascanner.def_value <> []
      )
      defs' in
  (* Return the package or raise Not_found if all archives vanished: *)
  let children = 
    (* Recursive patch, remove all Not_found packages: *)
    List.flatten
      (List.map
	 (fun (name, child) ->
	    try [ name, patch_archives pkgdir child ]
	    with Missing_archives _ -> []
	 )
	 pkg.Fl_metascanner.pkg_children) in
  let pkg' =
    { Fl_metascanner.pkg_defs = defs'';
      pkg_children = children
    } in
  if List.exists (fun def -> def.Fl_metascanner.def_var = "archive") defs'' then
    pkg'
  else
    raise (Missing_archives pkg')
;;


let rec patch_pkg pkgdir pkg patches =
  match patches with
    | [] -> pkg
    | (`Version v) :: patches' ->
	let def =
	  { Fl_metascanner.def_var = "version";
	    def_flav = `BaseDef;
	    def_preds = [];
	    def_value = v 
	  } in
	let defs =
	  List.filter
	    (fun d -> d.Fl_metascanner.def_var <> "version")
	    pkg.Fl_metascanner.pkg_defs in
	let pkg' =
	  { pkg with
	      Fl_metascanner.pkg_defs = def :: defs
	  } in
	patch_pkg pkgdir pkg' patches'
    | (`Rmpkg n) :: patches' ->
	let children =
	  List.filter
	    (fun (name,_) -> name <> n)
	    pkg.Fl_metascanner.pkg_children in
	let pkg' =
	  { pkg with
	      Fl_metascanner.pkg_children = children
	  } in
	patch_pkg pkgdir pkg' patches'
    | `Archives :: patches' ->
	let pkg' = 
	  try patch_archives pkgdir pkg 
	  with
	      Missing_archives p -> p in
	patch_pkg pkgdir pkg' patches'
;;


exception Skip_file;;

type which = Auto | Dll | No_dll;;

let meta_pkg meta_name =
  let f = open_in meta_name in
  try
    let pkg = Fl_metascanner.parse f in
    close_in f;
    pkg
  with
  | Failure s
  | Stream.Error s ->
    close_in f;
    failwith ("Cannot parse '" ^ meta_name ^ "': " ^ s)

let char_lowercase_ascii c =
  (* Char.lowercase_ascii and String.lowercase_ascii first available in
     OCaml-4.03, but we want to support earlier versions too
   *)
  if (c >= 'A' && c <= 'Z')
  then Char.unsafe_chr(Char.code c + 32)
  else c

let string_lowercase_ascii =
  String.map char_lowercase_ascii


let install_package () =
  let destdir = ref (default_location()) in
  let metadir = ref (meta_directory()) in
  let ldconf  = ref (ocaml_ldconf()) in
  let don't_add_directory_directive = ref false in
  let pkgname = ref "" in
  let auto_files = ref [] in
  let dll_files = ref [] in
  let nodll_files = ref [] in
  let which = ref Auto in
  let add_files = ref false in
  let optional = ref false in
  let patches = ref [] in

  let keywords =
    [ "-destdir", (Arg.String (fun s -> destdir := s)),
              ("<path>    Set the destination directory (default: " ^
	       !destdir ^ ")");
      "-metadir", (Arg.String (fun s -> metadir := s)),
              ("<path>    Install the META file into this directory (default: "^
	       (if !metadir = "" then "none" else !metadir) ^ ")");
      "-ldconf", (Arg.String (fun s -> ldconf := s)),
             ("<path>     Update this ld.conf file (default: " ^ !ldconf ^ ")");
      "-dont-add-directory-directive", (Arg.Set don't_add_directory_directive),
                                    " never append directory='...' to META";
      "-dll", Arg.Unit (fun () -> which := Dll),
           "              The following files are DLLs";
      "-nodll", Arg.Unit (fun () -> which := No_dll),
             "            The following files are not DLLs";
      "-add", Arg.Unit (fun () -> add_files := true),
           "              Add files to the package";
      "-optional", Arg.Set optional,
                "         The following files are optional";
      "-patch-version", Arg.String (fun s -> patches := !patches @ [`Version s]),
                     "<v> Set the package version to <v>";
      "-patch-rmpkg", Arg.String (fun s -> patches := !patches @ [`Rmpkg s]),
                   "<n>   Remove the subpackage <n>";
      "-patch-archives", Arg.Unit (fun () -> patches := !patches @ [`Archives]),
                      "   Remove non-existing archives";
    ] in
  let errmsg = "usage: ocamlfind install [options] <package_name> <file> ..." in

  parse_args
        keywords
	(fun s ->
	   if !pkgname = ""
	   then pkgname := s
	   else
	     if not !optional || Sys.file_exists s then
	       match !which with
		   Auto -> auto_files := s :: !auto_files
		 | Dll  -> dll_files := s :: !dll_files
		 | No_dll -> nodll_files := s :: !nodll_files
	)
	errmsg;
  if !pkgname = "" then (Arg.usage keywords errmsg; exit 1);
  if not (Fl_split.is_valid_package_name !pkgname) then
    failwith "Package names must not contain the character '.'!";

  let pkgdir = Filename.concat !destdir !pkgname in
  let dlldir = Filename.concat !destdir Findlib_config.libexec_name in
  let has_metadir = !metadir <> "" in
  let meta_dot_pkg = "META." ^ !pkgname in

  (* The list of all files to install: *)
  let full_list  = !auto_files @ !dll_files @ !nodll_files in
  (* Check whether there are DLLs: *)
  let (l1,l2)    = List.partition is_dll !auto_files in
  let dll_list   = l1 @ !dll_files in
  let nodll_list = l2 @ !nodll_files in
  let have_libexec = Sys.file_exists dlldir in
  let pkgdir_list = if have_libexec then nodll_list else full_list in
  let pkgdir_eff_list =
    (* The files that will be placed into pkgdir: *)
    List.map
      (fun f ->
	 if f = meta_dot_pkg then "META" else f)
      (List.filter
	 (fun f ->
	    not has_metadir ||
	      (f <> "META" && f <> meta_dot_pkg))
	 pkgdir_list) in
  
  (* Check whether META exists: (And check syntax) *)
  let meta_name =
    try
      List.find
	(fun p ->
	   let b = Filename.basename p in
	   b = "META" || b = meta_dot_pkg)
	nodll_list
    with
      | Not_found ->
	  if !add_files then (
	    let m1 = Filename.concat !metadir meta_dot_pkg in
	    let m2 = Filename.concat pkgdir "META" in
	    if Sys.file_exists m1 then
	      m1
	    else
	      if Sys.file_exists m2 then
		m2
	      else
		failwith "Cannot find META in package dir"
	  )
	  else
	    failwith "The META file is missing" in

  let meta_pkg = meta_pkg meta_name in

  if not !add_files then (
    (* Check for frequent reasons why installation can go wrong *)
    if Sys.file_exists (Filename.concat !metadir meta_dot_pkg) then
      failwith ("Package " ^ !pkgname ^ " is already installed\n - (file " ^ Filename.concat !metadir meta_dot_pkg ^ " already exists)");

    if Sys.file_exists (Filename.concat pkgdir "META") then
      failwith ("Package " ^ !pkgname ^ " is already installed\n - (file " ^ pkgdir ^ "/META already exists)");
  );
  List.iter
    (fun f ->
       let f' = Filename.concat pkgdir f in
       if Sys.file_exists f' then
	 failwith ("Conflict with file: " ^ f'))
    pkgdir_eff_list;

  if have_libexec then begin
    List.iter
      (fun dll ->
	 let b = Filename.basename dll in
	 if Sys.file_exists (Filename.concat dlldir b) then
	   failwith ("Conflict with another package: Library " ^ b ^
		     " is already installed");
      )
      dll_list
  end;

  (* Create the package directory: *)
  install_create_directory !pkgname pkgdir;

  (* Now copy the files into the package directory (except META): *)
  List.iter
    (fun p ->
       try
	 copy_file
	   ~rename: (fun f ->
			 if f = "META" || f = meta_dot_pkg then 
			   raise Skip_file
			 else
			   f)
	   p
	   pkgdir
       with
	   Skip_file -> ()
    )
    pkgdir_list;

  (* Copy the DLLs into the libexec directory if necessary *)
  if have_libexec then begin
    List.iter
      (fun p ->
	 copy_file p dlldir;
	 create_owner_file !pkgname
	   (Filename.concat dlldir (Filename.basename p))
      )
      dll_list
  end;

  (* Extend ld.conf if necessary: *)
  if dll_list <> [] && !ldconf <> "ignore" && not have_libexec then begin
    if Sys.file_exists !ldconf then
      begin
	let lines = read_ldconf !ldconf in
	write_ldconf !ldconf lines [ pkgdir ]
      end
    else
      prerr_endline("ocamlfind: [WARNING] You have installed DLLs but there is no ld.conf")
  end;

  if dll_list <> [] && have_libexec && !ldconf <> "ignore" then begin
    (* Check whether libexec is mentioned in ldconf *)
    (* FIXME: We have to be careful with case-insensitive filesystems. 
       Currently, we only check for Win32, but also OS X may have ci 
       filesystems. So some better check would be nice.
     *)
    let lines = read_ldconf !ldconf in
    let dlldir_norm = Fl_split.norm_dir dlldir in
    let dlldir_norm_lc = string_lowercase_ascii dlldir_norm in
    let ci_filesys = (Sys.os_type = "Win32") in
    let check_dir d =
      let d' = Fl_split.norm_dir d in
      (d' = dlldir_norm) || 
        (ci_filesys && string_lowercase_ascii d' = dlldir_norm_lc) in
    if not (List.exists check_dir lines) then
      prerr_endline("ocamlfind: [WARNING] You have installed DLLs but the directory " ^ dlldir_norm ^ " is not mentioned in ld.conf");
  end;

  (* Finally, write the META file: *)
  let write_meta append_directory dir name =
    (* If there are patches, write the patched META, else copy the file: *)
    if !patches = [] then
      copy_file 
	~rename:(fun _ -> name)
        ?append:(if append_directory then
		   Some("\ndirectory=\"" ^ pkgdir ^ 
			  "\" # auto-added by ocamlfind\n")
		 else
		   None)
	meta_name
	dir
    else (
      let p = Filename.concat dir name in
      let patched_pkg = patch_pkg pkgdir meta_pkg !patches in
      let out = open_out p in
        Fl_metascanner.print out patched_pkg;
      if append_directory then
	output_string out ("\ndirectory=\"" ^ pkgdir ^ 
			     "\" # auto-added by ocamlfind\n");
      close_out out;
      prerr_endline ("Installed " ^ p);
    )
  in
  if not !add_files then (
    if has_metadir then
      write_meta true !metadir meta_dot_pkg
    else
      write_meta false pkgdir "META";
  );

  (* Check if there is a postinstall script: *)
  let postinstall = Filename.concat !destdir "postinstall" in
  if Sys.file_exists postinstall then
    run_command Verbose postinstall [ slashify !destdir; !pkgname ]
;;


let reserved_names = [ Findlib_config.libexec_name; "postinstall"; "postremove" ];;

let remove_package () =
  let destdir = ref (default_location()) in
  let destdir_set = ref false in
  let metadir = ref (meta_directory()) in
  let ldconf  = ref (ocaml_ldconf()) in
  let pkgname = ref "" in

  let keywords =
    [ "-destdir", (Arg.String (fun s -> destdir := s; destdir_set := true)),
              ("<path>      Set the destination directory (default: " ^
	       !destdir ^ ")");
      "-metadir", (Arg.String (fun s -> metadir := s)),
              ("<path>      Remove the META file from this directory (default: " ^
	       (if !metadir = "" then "none" else !metadir) ^ ")");
      "-ldconf", (Arg.String (fun s -> ldconf := s)),
             ("<path>       Update this ld.conf file (default: " ^ !ldconf ^ ")");
    ] in
  let errmsg = "usage: ocamlfind remove [options] <package_name>" in

  parse_args
        keywords
	(fun s ->
	   if !pkgname = ""
	   then pkgname := s
	   else raise (Arg.Bad "too many arguments")
	)
	errmsg;
  if !pkgname = "" then (Arg.usage keywords errmsg; exit 1);
  if List.mem !pkgname reserved_names then
    failwith ("You are not allowed to remove this thing by ocamlfind!");
  if not (Fl_split.is_valid_package_name !pkgname) then
    failwith "Package names must not contain the character '.'!";

  let meta_dot_pkg = "META." ^ !pkgname in
  let has_metadir = !metadir <> "" in
  let pkgdir = Filename.concat !destdir !pkgname in
  let dlldir = Filename.concat !destdir Findlib_config.libexec_name in
  let have_libexec = Sys.file_exists dlldir in

  (* Warn if there is another package with the same name: *)
  let other_pkgdir =
    try Findlib.package_directory !pkgname with No_such_package _ -> "" in
  if other_pkgdir <> "" && not !destdir_set then begin
    (* Is pkgdir = other_pkgdir? - We check physical identity: *)
    try
      let s_other_pkgdir = Unix.stat other_pkgdir in
      try
	let s_pkgdir = Unix.stat pkgdir in
	if (s_pkgdir.Unix.st_dev <> s_other_pkgdir.Unix.st_dev) ||
	   (s_pkgdir.Unix.st_ino <> s_other_pkgdir.Unix.st_ino)
	then
	  prerr_endline("ocamlfind: [WARNING] You are removing the package from " ^ pkgdir ^ " but the currently visible package is at " ^ other_pkgdir ^ "; you may want to specify the -destdir option");
      with
	  Unix.Unix_error(Unix.ENOENT,_,_) ->
	    prerr_endline("ocamlfind: [WARNING] You are trying to remove the package from " ^ pkgdir ^ " but the currently visible package is at " ^ other_pkgdir ^ "; you may want to specify the -destdir option");
    with
	Unix.Unix_error(_,_,_) -> ()    (* ignore, it's only a warning *)
  end;

  (* First remove the META file. If it is already gone, assume that a
     parallel running removal removed it already.
   *)

  (* If there is a metadir, remove the META file from it: *)
  let meta_removal_ok =
    if has_metadir then (
      let f = Filename.concat !metadir meta_dot_pkg in
      try
        Unix.unlink f;
        prerr_endline ("Removed " ^ f);
        true
      with
        | Unix.Unix_error(Unix.ENOENT,_,_) ->
            prerr_endline ("ocamlfind: [WARNING] No such file: " ^ f);
            false
        | Unix.Unix_error(code, _, arg) ->
            raise(sys_error code arg)
    ) else
      let f = Filename.concat pkgdir "META" in
      try
        Unix.unlink f;
        prerr_endline ("Removed " ^ f);
        true
      with
        | Unix.Unix_error(Unix.ENOENT,_,_) ->
            prerr_endline ("ocamlfind: [WARNING] No such file: " ^ f);
            false
        | Unix.Unix_error(code, _, arg) ->
            raise(sys_error code arg) in

  if meta_removal_ok then (

    (* Remove files from libexec directory: *)
    if have_libexec then begin
      let dll_files = find_owned_files !pkgname dlldir in
      List.iter
        (fun file ->
           let absfile = Filename.concat dlldir file in
           Sys.remove absfile;
           prerr_endline ("Removed " ^ absfile)
        )
        dll_files
    end;

    (* Remove the files from the package directory: *)
    if Sys.file_exists pkgdir then begin
      let files = Sys.readdir pkgdir in
      Array.iter (fun f -> Sys.remove (Filename.concat pkgdir f)) files;
      Unix.rmdir pkgdir;
      prerr_endline ("Removed " ^ pkgdir)
    end
    else
      prerr_endline("ocamlfind: [WARNING] No such directory: " ^ pkgdir);

    (* Modify ld.conf *)
    if !ldconf <> "ignore" then begin
      if Sys.file_exists !ldconf then
        begin
          let lines = read_ldconf !ldconf in
          let d = Fl_split.norm_dir pkgdir in
          let exists = List.exists (fun p -> Fl_split.norm_dir p = d) lines in
          if exists then begin
            let lines' = List.filter (fun p -> Fl_split.norm_dir p <> d) lines in
            write_ldconf !ldconf lines' []
          end
        end
    end;

    (* Check if there is a postremove script: *)
    let postremove = Filename.concat !destdir "postremove" in
    if Sys.file_exists postremove then
      run_command Verbose postremove [ slashify !destdir; !pkgname ]
  )
;;


let list_packages() =

  let descr = ref false in

  let keywords =
    [ "-describe", Arg.Set descr,
                "    Output package descriptions";
    ] in
  let errmsg = "usage: ocamlfind list [options]" in

  parse_args
      keywords
      (fun _ -> Arg.usage keywords errmsg; exit 1)
      errmsg;

  Findlib.list_packages ~descr:!descr stdout;
  Fl_package_base.package_conflict_report ~identify_dir ()
;;


let print_configuration() =
  let dir s =
    if Sys.file_exists s then
      s
    else
      s ^ " (not found)"
  in

  let var = ref None in
  let errmsg = "usage: ocamlfind printconf (conf|path|destdir|metadir|effmetadir|stdlib|ldconf)" in

  parse_args
        []
	(fun s ->
	   if !var <> None then raise(Arg.Bad "Unexpected argument");
	   match s with
	       ("conf" | "path" | "destdir" | "metadir" | "metapath" | "stdlib" | "ldconf") ->
		 var := Some s
	     | _ ->
		 raise(Arg.Bad "Bad argument");
	)
	errmsg;

  match !var with
      None ->
	print_endline "Effective configuration:";
	Printf.printf "Configuration file:\n    %s\n"
	  (dir (Findlib.config_file()));
	Printf.printf "Search path:\n";
	List.iter
	  (fun p -> Printf.printf "    %s\n" (dir p))
	  (Findlib.search_path());
	Printf.printf "Packages will be installed in/removed from:\n    %s\n"
	  (dir (Findlib.default_location()));
	Printf.printf "META files will be installed in/removed from:\n    %s\n"
	  (let md = Findlib.meta_directory() in
	   if md = "" then "the corresponding package directories" else dir md
	  );
	Printf.printf "The standard library is assumed to reside in:\n    %s\n"
	  (Findlib.ocaml_stdlib());
	Printf.printf "The ld.conf file can be found here:\n    %s\n"
	  (Findlib.ocaml_ldconf());
	flush stdout
    | Some "conf" ->
	print_endline (Findlib.config_file())
    | Some "path" ->
	List.iter print_endline (Findlib.search_path())
    | Some "destdir" ->
	print_endline (Findlib.default_location())
    | Some "metadir" ->
	print_endline (Findlib.meta_directory())
    | Some "metapath" ->
        let mdir = Findlib.meta_directory() in
        let ddir = Findlib.default_location() in
	print_endline 
          (if mdir <> "" then mdir ^ "/META.%s" else ddir ^ "/%s/META")
    | Some "stdlib" ->
	print_endline (Findlib.ocaml_stdlib())
    | Some "ldconf" ->
	print_endline (Findlib.ocaml_ldconf())
    | _ ->
	assert false
;;


let ocamlcall pkg cmd =
  let dir = package_directory pkg in
  let path = Filename.concat dir cmd in
  begin
    try Unix.access path [ Unix.X_OK ]
    with
	Unix.Unix_error (Unix.ENOENT, _, _) ->
	  failwith ("Cannot find command: " ^ path)
      | Unix.Unix_error (Unix.EACCES, _, _) ->
	  failwith ("Cannot execute: " ^ path)
      | other ->
	  Unix.handle_unix_error (fun () -> raise other) ()
  end;
  let args = Array.to_list (Array.sub Sys.argv 2 (Array.length Sys.argv -2)) in
  run_command Normal path args
;;

(** lint META file *)
let lint () =

  let meta_files = Queue.create () in

  parse_args
    ~align:false
    ( Arg.align [
        ])
    (fun s -> if Sys.file_exists s
      then Queue.add s meta_files
      else raise(Arg.Bad (Printf.sprintf "%s: file doesn't exists" s)))
    "usage: ocamlfind lint <options> <files>...";

  let error =
    Queue.fold (fun error file ->
        let pkg = meta_pkg file in
        let error = Fl_lint.warn pkg || error in
        error
      ) false meta_files in
  exit (if error then 1 else 0)
;;


(** print ppx options *)
let print_ppx () =

  let packages = ref [] in
  let predicates = ref [] in
  let ppx_opts = ref [] in

  let add_pred =
    Arg.String (fun s -> predicates := !predicates @ (Fl_split.in_words s)) in
  let add_ppx_opt =
    Arg.String (fun s -> ppx_opts := !ppx_opts @ [s]) in

  parse_args
    [ "-predicates", add_pred,
        " specifies comma-separated list of assumed predicates";
      "-ppxopt", add_ppx_opt,
        "<pkg>,<opts>  Append options <opts> to ppx invocation for package <pkg>";
    ]
    (fun p -> packages := !packages @ [p])
"usage: ocamlfind printppx [options] package ...";

  let ppx_commands =
    process_ppx_spec !predicates !packages !ppx_opts
  in
  print_endline (String.concat " " (List.map escape_if_needed ppx_commands))
;;


let rec select_mode () =
  let k = !Arg.current in
  let m_string = try arg (k+1) with Not_found -> raise Usage in
  let m =
    match m_string with
      ("use"|"-use")                       -> incr Arg.current; M_use
    | ("query"|"-query")                   -> incr Arg.current; M_query
    | ("install"|"-install")               -> incr Arg.current; M_install
    | ("remove"|"-remove")                 -> incr Arg.current; M_remove
    | ("ocamlc"|"-ocamlc"|"c")             -> incr Arg.current; M_compiler "ocamlc"
    | ("ocamlcp"|"-ocamlcp"|"cp")          -> incr Arg.current; M_compiler "ocamlcp"
    | ("ocamloptp"|"-ocamloptp"|"optp")    -> incr Arg.current; M_compiler "ocamloptp"
    | ("ocamlmklib"|"-ocamlmklib"|"mklib") -> incr Arg.current; M_compiler "ocamlmklib"
    | ("ocamlmktop"|"-ocamlmktop"|"mktop") -> incr Arg.current; M_compiler "ocamlmktop"
    | ("ocamlopt"|"-ocamlopt"|"opt")       -> incr Arg.current; M_compiler "ocamlopt"
    | ("ocamldep"|"-ocamldep"|"dep")       -> incr Arg.current; M_dep
    | ("ocamlbrowser"|"-ocamlbrowser"|"browser") -> incr Arg.current; M_browser
    | ("ocamldoc"|"-ocamldoc"|"doc")       -> incr Arg.current; M_doc
    | ("printconf"|"-printconf")           -> incr Arg.current; M_printconf
    | ("list"|"-list")                     -> incr Arg.current; M_list
    | ("lint"|"-lint")                     -> incr Arg.current; M_lint
    | ("printppx"|"-printppx")             -> incr Arg.current; M_printppx
    | "-toolchain" ->
	let t = try arg (k+2) with Not_found -> raise Usage in
	Findlib.init ~toolchain:t ();
	Arg.current := k+2;
	select_mode()
    | s when String.contains m_string '/' ->
	incr Arg.current;
	let k = String.index m_string '/' in
	let pkg = String.sub m_string 0 k in
	let cmd = String.sub m_string (k+1) (String.length m_string - k - 1) in
	M_call(pkg,cmd)
    | _ -> raise Usage
  in

  m
;;


let main() =
  try
    let m = select_mode() in
    let l = Array.length Sys.argv in
    let k = !Arg.current in
    let rest = Array.sub Sys.argv (k+1) (l-k-1) in
    match m with
      M_use            -> if rest = [| |] then raise Usage;
                          if rest.(0) = "-p" then begin
	                    if l<4 then raise Usage;
	                      use_package rest.(1)
	                          (List.tl(List.tl(Array.to_list rest)))
	                  end
	                  else
                            use_package "" (Array.to_list rest)
    | M_query          -> query_package ()
    | M_install        -> install_package()
    | M_remove         -> remove_package ()
    | M_printconf      -> print_configuration ()
    | M_list           -> list_packages()
    | M_dep            -> ocamldep()
    | M_browser        -> ocamlbrowser()
    | M_doc            -> ocamldoc()
    | M_call(pkg,cmd)  -> ocamlcall pkg cmd
    | M_compiler which -> ocamlc which ()
    | M_lint           -> lint()
    | M_printppx       -> print_ppx()
  with
    Usage ->
      prerr_endline "Usage: ocamlfind query        [-help | other options] <package_name> ...";
      prerr_endline "   or: ocamlfind ocamlc       [-help | other options] <file> ...";
      prerr_endline "   or: ocamlfind ocamlcp      [-help | other options] <file> ...";
      prerr_endline "   or: ocamlfind ocamlmklib   [-help | other options] <file> ...";
      prerr_endline "   or: ocamlfind ocamlmktop   [-help | other options] <file> ...";
      if Ocaml_args.ocamlopt_spec <> None then
	prerr_endline "   or: ocamlfind ocamlopt     [-help | other options] <file> ...";
      if Ocaml_args.ocamloptp_spec <> None then
	prerr_endline "   or: ocamlfind ocamloptp    [-help | other options] <file> ...";
      prerr_endline "   or: ocamlfind ocamldep     [-help | other options] <file> ...";
      prerr_endline "   or: ocamlfind ocamlbrowser [-help | other options]";
      prerr_endline "   or: ocamlfind ocamldoc     [-help | other options] <file> ...";
      prerr_endline "   or: ocamlfind install      [-help | other options] <package_name> <file> ...";
      prerr_endline "   or: ocamlfind remove       [-help | other options] <package_name>";
      prerr_endline "   or: ocamlfind printppx     [-help | other options] <package_name> ...";
      prerr_endline "   or: ocamlfind printconf    [-help] [variable]";
      prerr_endline "   or: ocamlfind lint         [-help] <file>";
      prerr_endline "   or: ocamlfind list";
      prerr_endline "   or: ocamlfind pkg/cmd arg ...";
      prerr_endline "Select toolchain with:";
      prerr_endline "  ocamlfind -toolchain <t> <command>";
      prerr_endline "Abbreviations:";
      prerr_endline "  e.g. ocamlfind opt instead of ocamlfind ocamlopt";
      exit 2
  | Failure f ->
      prerr_endline ("ocamlfind: " ^ f);
      exit 2
  | Sys_error f ->
      prerr_endline ("ocamlfind: " ^ f);
      exit 2
  | Findlib.No_such_package(pkg,info) ->
      prerr_endline ("ocamlfind: Package `" ^ pkg ^ "' not found" ^
		     (if info <> "" then " - " ^ info else ""));
      exit 2
  | Findlib.Package_loop pkg ->
      prerr_endline ("ocamlfind: Package `" ^ pkg ^ "' requires itself");
      exit 2
  | Silent_error ->
      exit 2
;;


try
  Sys.catch_break true;
  main()
with
  any ->
    prerr_endline ("Uncaught exception: " ^ Printexc.to_string any);
    let raise_again =
      try ignore(Sys.getenv "OCAMLFIND_DEBUG"); true
      with Not_found -> false
    in
    if raise_again then raise any;
    exit 3
;;
