(* $Id$
 * ----------------------------------------------------------------------
 *
 *)

module StrSet = Set.Make(String)

exception No_such_package 
  = Fl_package_base.No_such_package


exception Package_loop 
  = Fl_package_base.Package_loop

type formal_pred =
    [ `Pred of string
    | `NegPred of string
    ]

let init_called = ref false ;;

let conf_config_file = ref "";;
let conf_default_location = ref "";;
let conf_meta_directory = ref "";;
let conf_search_path = ref [];;
let conf_command = ref [];;
let conf_stdlib = ref "";;
let conf_ldconf = ref "";;
let conf_ignore_dups_in = ref ([] : string list);;

let ocamlc_default = "ocamlc";;
let ocamlopt_default = "ocamlopt";;
let ocamlcp_default = "ocamlcp";;
let ocamloptp_default = "ocamloptp";;
let ocamlmklib_default = "ocamlmklib";;
let ocamlmktop_default = "ocamlmktop";;
let ocamldep_default = "ocamldep";;
let ocamlbrowser_default = "ocamlbrowser";;
let ocamldoc_default = "ocamldoc";;


let init_manually 
      ?(ocamlc_command = ocamlc_default)
      ?(ocamlopt_command = ocamlopt_default)
      ?(ocamlcp_command = ocamlcp_default)
      ?(ocamloptp_command = ocamloptp_default)
      ?(ocamlmklib_command = ocamlmklib_default)
      ?(ocamlmktop_command = ocamlmktop_default)
      ?(ocamldep_command = ocamldep_default)
      ?(ocamlbrowser_command = ocamlbrowser_default)
      ?(ocamldoc_command = ocamldoc_default)
      ?ignore_dups_in
      ?(ignore_dups_in_list = [])
      ?(stdlib = Findlib_config.ocaml_stdlib)
      ?(ldconf = Findlib_config.ocaml_ldconf)
      ?(config = Findlib_config.config_file)
      ~install_dir
      ~meta_dir
      ~search_path () =
  conf_command := [ `ocamlc,     ocamlc_command;
		    `ocamlopt,   ocamlopt_command;
		    `ocamlcp,    ocamlcp_command;
		    `ocamloptp,  ocamloptp_command;
		    `ocamlmklib, ocamlmklib_command;
		    `ocamlmktop, ocamlmktop_command;
		    `ocamldep,   ocamldep_command;
		    `ocamlbrowser, ocamlbrowser_command;
		    `ocamldoc,   ocamldoc_command;
		  ];
  conf_config_file := config;
  conf_search_path := search_path;
  conf_default_location := install_dir;
  conf_meta_directory := meta_dir;
  conf_stdlib := stdlib;
  conf_ldconf := ldconf;
  conf_ignore_dups_in :=
    ( match ignore_dups_in with
        | None -> []
        | Some d -> [d]
    ) @ ignore_dups_in_list;
  Fl_package_base.init !conf_search_path stdlib !conf_ignore_dups_in;
  init_called := true
;;


let command_names cmd_spec =
  try
    let cmd_list = Fl_split.in_words cmd_spec in
    List.map
      (fun cmd_setting ->
	 try
	   (* cmd_setting: formal_name=actual_name *)
	   let l = String.length cmd_setting in
	   let n = String.index cmd_setting '=' in
	   let cmd_formal_name = String.sub cmd_setting 0 n in
	   let cmd_actual_name = String.sub cmd_setting (n+1) (l-n-1) in
	   cmd_formal_name, cmd_actual_name
	 with
	     Not_found ->
	       prerr_endline ("Warning: Please check the environment variable OCAMLFIND_COMMANDS");
	       "", ""
      )
      cmd_list
  with
      Not_found ->
	[]
;;

let auto_config_file() =
  let p =
    ( try Sys.getenv "OCAMLFIND_CONF" with Not_found -> "") in
  if p = "" then Findlib_config.config_file else p
                                                   
  
let init
      ?env_ocamlpath ?env_ocamlfind_destdir ?env_ocamlfind_metadir
      ?env_ocamlfind_commands ?env_ocamlfind_ignore_dups_in
      ?env_ocamlfind_ignore_dups_in_list ?env_camllib ?env_ldconf
      ?config ?toolchain () =
  
  let config_file =
    match config with
	Some f -> f
      | None -> auto_config_file()
  in

  let configd_file =
    config_file ^ ".d" in

  let vars_of_file f =
    let ch = open_in f in
    try
      let vars = 
	(Fl_metascanner.parse ch).Fl_metascanner.pkg_defs in
      close_in ch;
      vars
    with
      | error -> close_in ch; raise error in

  let vars_of_dir d =
    let files = Array.to_list (Sys.readdir d) in
    List.flatten
      (List.map
	 (fun file ->
	    if Filename.check_suffix file ".conf" then
	      vars_of_file (Filename.concat d file)
	    else
	      []
	 )
	 files)
  in

  let config_preds =
    match toolchain with
      | None -> (try [Sys.getenv "OCAMLFIND_TOOLCHAIN"] with Not_found -> [])
      | Some p -> [p] in

  let sys_ocamlc, sys_ocamlopt, sys_ocamlcp, sys_ocamloptp, sys_ocamlmklib,
      sys_ocamlmktop, sys_ocamldep, sys_ocamlbrowser, sys_ocamldoc,
      sys_search_path, sys_destdir, sys_metadir, sys_stdlib, sys_ldconf = 
    (
      let config_vars =
        if config_file <> "" &&
           not(Sys.file_exists config_file) && not(Sys.file_exists configd_file)
        then
          failwith("Config file not found - neither " ^ 
                     config_file ^ " nor the directory " ^ 
                     configd_file);
	if Sys.file_exists config_file then 
	  vars_of_file config_file
	else
	  [] in
      let configd_vars =
	if Sys.file_exists configd_file then 
	  vars_of_dir configd_file
	else
	  [] in
      let vars = config_vars @ configd_vars in
      if vars <> [] then (
        let found = ref false in
	let lookup name default =
          let explicit_preds =
            List.for_all
              (fun p -> Fl_metascanner.predicate_exists p vars) 
              config_preds in
          found := !found || explicit_preds;
	  try 
            Fl_metascanner.lookup name config_preds vars
	  with Not_found -> default
	in
        let config_tuple =
	  ( (lookup "ocamlc" ocamlc_default),
	    (lookup "ocamlopt" ocamlopt_default),
	    (lookup "ocamlcp" ocamlcp_default),
	    (lookup "ocamloptp" ocamloptp_default),
	    (lookup "ocamlmklib" ocamlmklib_default),
	    (lookup "ocamlmktop" ocamlmktop_default),
	    (lookup "ocamldep" ocamldep_default),
	    (lookup "ocamlbrowser" ocamlbrowser_default),
	    (lookup "ocamldoc" ocamldoc_default),
	    Fl_split.path (lookup "path" ""),
	    (lookup "destdir" ""),
	    (lookup "metadir" "none"),
	    (lookup "stdlib" Findlib_config.ocaml_stdlib),
	    (lookup "ldconf" Findlib_config.ocaml_ldconf)
	  ) in
        if not !found && config_preds <> [] then
          prerr_endline("ocamlfind: [WARNING] Undefined toolchain: " ^ 
                          String.concat "" config_preds);
        config_tuple
      )
      else
	( ocamlc_default, ocamlopt_default, ocamlcp_default, ocamloptp_default,
	  ocamlmklib_default,
	  ocamlmktop_default, ocamldep_default, ocamlbrowser_default,
	  ocamldoc_default,
	  [],
	  "",
          "none",
	  Findlib_config.ocaml_stdlib,
	  Findlib_config.ocaml_ldconf
	)
    )
  in

  let env_commands = 
    match env_ocamlfind_commands with
	Some x -> command_names x
      | None   -> command_names (try Sys.getenv "OCAMLFIND_COMMANDS"
				 with Not_found -> "")
  in
  let env_destdir = 
    match env_ocamlfind_destdir with
	Some x -> x
      | None   ->
	  try Sys.getenv "OCAMLFIND_DESTDIR" with Not_found -> "" 
  in
  let env_metadir = 
    match env_ocamlfind_metadir with
	Some x -> x
      | None   ->
	  try Sys.getenv "OCAMLFIND_METADIR" with Not_found -> "" 
  in
  let env_search_path = 
    Fl_split.path
      (match env_ocamlpath with
	   Some x -> x
	 | None -> 
	     try Sys.getenv "OCAMLPATH" with Not_found -> "" 
      )
  in
  let env_stdlib =
    match env_camllib with
	Some x -> x
      | None ->
	  ( try Sys.getenv "OCAMLLIB"
	    with 
		Not_found ->
		  (try Sys.getenv "CAMLLIB" with Not_found -> "" )
	  )
  in
  let env_ldconf =
    match env_ldconf with
	Some x -> x
      | None ->
	  try Sys.getenv "OCAMLFIND_LDCONF" with Not_found -> ""
  in
  let ignore_dups_in_list =
    match env_ocamlfind_ignore_dups_in, env_ocamlfind_ignore_dups_in_list with
      | Some x0, Some l -> x0 :: l
      | None, Some l -> l
      | Some x0, None -> [x0]
      | None, None ->
          try Fl_split.path (Sys.getenv "OCAMLFIND_IGNORE_DUPS_IN")
          with Not_found -> [] in

  let ocamlc, ocamlopt, ocamlcp, ocamloptp, ocamlmklib, ocamlmktop,
      ocamldep, ocamlbrowser, ocamldoc,
      search_path, destdir, metadir, stdlib, ldconf =
    (try List.assoc "ocamlc"     env_commands with Not_found -> sys_ocamlc),
    (try List.assoc "ocamlopt"   env_commands with Not_found -> sys_ocamlopt),
    (try List.assoc "ocamlcp"    env_commands with Not_found -> sys_ocamlcp),
    (try List.assoc "ocamloptp"  env_commands with Not_found -> sys_ocamloptp),
    (try List.assoc "ocamlmklib" env_commands with Not_found -> sys_ocamlmklib),
    (try List.assoc "ocamlmktop" env_commands with Not_found -> sys_ocamlmktop),
    (try List.assoc "ocamldep"   env_commands with Not_found -> sys_ocamldep),
    (try List.assoc "ocamlbrowser" env_commands with Not_found -> sys_ocamlbrowser),
    (try List.assoc "ocamldoc"   env_commands with Not_found -> sys_ocamldoc),
    (env_search_path @ sys_search_path),
    (if env_destdir = "" then sys_destdir else env_destdir),
    (if env_metadir = "" then sys_metadir else env_metadir),
    (if env_stdlib  = "" then sys_stdlib  else env_stdlib),
    (if env_ldconf  = "" then sys_ldconf  else env_ldconf)
  in

  init_manually
    ~ocamlc_command: ocamlc
    ~ocamlopt_command: ocamlopt
    ~ocamlcp_command: ocamlcp
    ~ocamloptp_command: ocamloptp
    ~ocamlmklib_command: ocamlmklib
    ~ocamlmktop_command: ocamlmktop
    ~ocamldep_command: ocamldep
    ~ocamlbrowser_command: ocamlbrowser
    ~ocamldoc_command: ocamldoc
    ~ignore_dups_in_list
    ~stdlib: stdlib
    ~ldconf: ldconf
    ~config: config_file
    ~install_dir: destdir
    ~meta_dir: metadir
    ~search_path: search_path
    ()
;;


let lazy_init() =
  if not !init_called then init()

let config_file() =
  lazy_init();
  !conf_config_file;;
                               

let default_location() = 
  lazy_init();
  !conf_default_location;;


let meta_directory() =
  lazy_init();
  if !conf_meta_directory = "none" then "" else !conf_meta_directory;;


let search_path() = 
  lazy_init();
  !conf_search_path;;


let command which =
  lazy_init();
  try 
    List.assoc which !conf_command
  with
      Not_found -> assert false
;;


let ocaml_stdlib() = 
  lazy_init();
  !conf_stdlib;;


let ocaml_ldconf() = 
  lazy_init();
  !conf_ldconf;;

let ignore_dups_in() = 
  lazy_init();
  !conf_ignore_dups_in;;

let package_directory pkg =
  lazy_init();
  (Fl_package_base.query pkg).Fl_package_base.package_dir
;;


let package_meta_file pkg =
  lazy_init();
  (Fl_package_base.query pkg).Fl_package_base.package_meta
;;


let package_property_2 predlist pkg propname =
  lazy_init();
  let l = Fl_package_base.query pkg in
  Fl_metascanner.lookup_2 propname predlist l.Fl_package_base.package_defs
;;


let package_property predlist pkg propname =
  lazy_init();
  let l = Fl_package_base.query pkg in
  Fl_metascanner.lookup propname predlist l.Fl_package_base.package_defs
;;


let package_ancestors predlist pkg =
  lazy_init();
  Fl_package_base.requires predlist pkg
;;


let package_deep_ancestors predlist pkglist =
  lazy_init();
  Fl_package_base.requires_deeply predlist pkglist
;;


let resolve_path ?base ?(explicit=false) p =
  lazy_init();
  if p = "" then "" else (
    match p.[0] with
	'^' | '+' ->
	  let stdlibdir = Fl_split.norm_dir (ocaml_stdlib()) in
	  Filename.concat
	    stdlibdir
	    (String.sub p 1 (String.length p - 1))
      | '@' ->
	  (* Search slash *)
	  ( try 
	      let k = String.index p '/' in  (* or Not_found *)
	      let pkg = String.sub p 1 (k-1) in
	      let p' = String.sub p (k+1) (String.length p - k - 1) in
	      let pkgdir = package_directory pkg in
	      Filename.concat pkgdir p'
	    with
		Not_found ->
		  let pkg = String.sub p 1 (String.length p - 1) in
		  package_directory pkg
	  )
      | _ ->
	  ( match base with
		None -> p
	      | Some b ->
		  if Filename.is_relative p &&
                       (not explicit || not (Filename.is_implicit p))
                  then
		    Filename.concat b p
		  else
		    p
	  )
  )
;;


let list_packages ?(tab = 20) ?(descr = false) ch =
  lazy_init();
  let packages = Fl_package_base.list_packages() in
  let packages_sorted = List.sort compare packages in

  List.iter
    (fun p ->
       let v_string =
	 try
	   let v = package_property [] p "version" in
	   "(version: " ^ v ^ ")"
	 with
	     Not_found -> "(version: n/a)"
       in
       let descr_string =
	 try package_property [] p "description" 
	 with Not_found -> "(no description)" in
       let spaces1 = String.make (max 1 (tab-String.length p)) ' ' in
       let spaces2 = String.make tab ' ' in
       
       if descr then (
	 output_string ch (p ^ spaces1 ^ descr_string ^ "\n");
	 output_string ch (spaces2 ^ v_string ^ "\n")
       )
       else
	 output_string ch (p ^ spaces1 ^ v_string ^ "\n");
    )
    packages_sorted
;;

let list_packages' ?prefix () =
  lazy_init();
  Fl_package_base.list_packages ?prefix ()


type rectype =
  | Record_core
  | Record_load

let rec_core = ref StrSet.empty
let rec_load = ref StrSet.empty
let rec_preds = ref []

let record_package (rt:rectype) (p:string) =
  match rt with
    | Record_core ->
        rec_core := StrSet.add p !rec_core
    | Record_load ->
        rec_load := StrSet.add p !rec_load

let recorded_packages rt =
  match rt with
    | Record_core ->
        StrSet.elements !rec_core
    | Record_load ->
        StrSet.elements (StrSet.diff !rec_load !rec_core)

let reset_recordings() =
  rec_load := StrSet.empty

let type_of_recorded_package p =
  if StrSet.mem p !rec_core then
    Record_core
  else
    if StrSet.mem p !rec_load then
      Record_load
    else
      raise Not_found

let is_recorded_package p =
  try ignore(type_of_recorded_package p); true with Not_found -> false


let rm_preds =
  [ "create_toploop"; "toploop"; "executable"; "plugin"; "autolink";
    "preprocessor"; "syntax" ]

let rm_preds_set =
  List.fold_right StrSet.add rm_preds StrSet.empty

let record_package_predicates preds =
  let preds' =
    List.filter (fun p -> not(StrSet.mem p rm_preds_set)) preds in
  rec_preds := preds'

let recorded_predicates() =
  !rec_preds

