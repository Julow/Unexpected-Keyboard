(* $Id$
 * ----------------------------------------------------------------------
 *
 *)

open Fl_metascanner

exception No_such_package of string * string
  (* (name, reason) *)

type package =
    { package_name : string;
      package_dir : string;
      package_meta : string;
      package_defs : Fl_metascanner.pkg_definition list;
      package_priv : package_priv
    }
and package_priv =
    { mutable missing_reqs : (string * string) list;
        (* If non-empty the package is broken. This may be set by
	   add_all_relations, and should be checked before using the
	   package later. Each element corresponds to No_such_package.
	 *)
    }
;;


module Fl_metaentry =
  struct
    type t = package
    type id_t = string
    let id m = m.package_name
  end
;;


module Fl_metastore =
  Fl_topo.Make(Fl_metaentry)
;;


module StringSet = Set.Make(String);;


let has_prefix s pref =
  String.length s >= String.length pref &&
  String.sub s 0 (String.length pref) = pref
;;


let ocamlpath = ref [];;
let ocamlstdlib = ref "";;

let conf_ignore_dups_in = ref ([] : string list)

let store = Fl_metastore.create();;
  (* We collect here only nodes, but no relations. First copy [store]
   * and put relations into the copy.
   *)


let init path stdlib ignore_dups_in =
  ocamlpath := path;
  ocamlstdlib := stdlib;
  conf_ignore_dups_in := ignore_dups_in
;;


let packages_in_meta_file ?(directory_required = false)
                          ~name:package_name ~dir:package_dir ~meta_file () =
  (* Parses the META file whose name is [meta_file]. In [package_name], the
   * name of the main package must be passed. [package_dir] is the
   * directory associated with the package by default (i.e. before
   * it is overriden by the "directory" directive).
   *
   * directory_required: If true, a "directory" directive is necessary.
   *
   * Returns the [package] records found in this file. The "directory"
   * directive is already applied.
   *)
  let rec flatten_meta pkg_name_prefix pkg_dir (pkg_name_component,pkg_expr) =
    (* Turns the recursive [pkg_expr] into a flat list of [package]s.
     * [pkg_dir] is the default package directory. [pkg_name_prefix] is
     * the name prefix to prepend to the fully qualified package name, or
     * "". [pkg_name_component] is the local package name.
     *)
    (* Determine the final package directory: *)
    let d =
      (* The value of "directory", or "" if not applicable *)
      try
	lookup "directory" [] pkg_expr.pkg_defs
      with
	  Not_found ->
	    if pkg_name_prefix="" && directory_required then
	      failwith ("The `directory' directive is required in this META definition");

	    ""
    in
    let d' =
      if d = "" then
	pkg_dir
      else
	match d.[0] with
          | '^'
	  | '+' -> 
	      let rest = String.sub d 1 (String.length d - 1) in
	      if rest = "" then
		!ocamlstdlib
	      else
		Filename.concat !ocamlstdlib rest
	  | _ -> 
	      if Filename.is_relative d then
		Filename.concat pkg_dir d
	      else
		d
    in
    let p_name =
      if pkg_name_prefix = "" then
	pkg_name_component
      else
	pkg_name_prefix ^ "." ^ pkg_name_component in
    let p =
      { package_name = p_name;
	package_dir = d';
        package_meta = meta_file;
	package_defs = pkg_expr.pkg_defs;
	package_priv = { missing_reqs = [] }
      } in
    (* Check for exists_if: *)
    let p_exists =
      try
	let def =
	  List.find (fun def -> def.def_var = "exists_if") p.package_defs  in
	let files = Fl_split.in_words def.def_value in
	List.exists 
	  (fun file -> Sys.file_exists (Filename.concat d' file))
	  files
      with Not_found -> true in

    if p_exists then
      p :: (List.flatten
	      (List.map (flatten_meta p_name d') pkg_expr.pkg_children))
    else
      []
  in

  let ch = open_in meta_file in
  try
    let pkg_expr = Fl_metascanner.parse ch in
    let packages = flatten_meta "" package_dir (package_name, pkg_expr) in
    close_in ch;
    packages
  with
      Failure s ->
	close_in ch;
	failwith ("While parsing '" ^ meta_file ^ "': " ^ s)
    | Stream.Error s ->
	close_in ch;
	failwith ("While parsing '" ^ meta_file ^ "': " ^ s)
    | any ->
	close_in ch;
	raise any
;;


let query package_name =

  let package_name_comps = Fl_split.package_name package_name in
  if package_name_comps = [] then invalid_arg "Fl_package_base.query";
  let main_name = List.hd package_name_comps in

  let process_file_and_lookup ?directory_required package_dir meta_file =
    let packages =
      packages_in_meta_file
	?directory_required ~name:main_name ~dir:package_dir ~meta_file () in
    let p =
      ( try
	  List.find
	    (fun p -> p.package_name = package_name)
	    packages
	with
	    Not_found ->
	      raise (No_such_package (package_name, ""))
      ) in
    List.iter (Fl_metastore.add store) packages;
    p
  in

  let rec run_ocamlpath path =
    match path with
      [] -> raise(No_such_package(package_name, ""))
    | dir :: path' ->
	let package_dir = Filename.concat dir main_name in
	let meta_file_1 = Filename.concat package_dir "META" in
	let meta_file_2 = Filename.concat dir ("META." ^ main_name) in
	if Sys.file_exists meta_file_1 then
	  process_file_and_lookup package_dir meta_file_1
	else
	  if Sys.file_exists meta_file_2 then
	    process_file_and_lookup ~directory_required:true dir meta_file_2
	      (* Note: It is allowed to have relative "directory" directives.
	       * The base directory is [dir] in this case.
	       *)
	  else
	    run_ocamlpath path'
  in

  try
    Fl_metastore.find store package_name
  with
    Not_found ->
      run_ocamlpath !ocamlpath
;;


exception Package_loop of string
  (* A package is required by itself. The arg is the name of the
   * package
   *)


let fixup_thread_needed_1 predlist =
  (* When the thread fixup is required to apply, 1st criterion *)
  List.mem "mt" predlist
;;


let fixup_thread_needed_2 pkg =
  (* When the thread fixup is required to apply, 2nd criterion *)
  (pkg <> "unix" && pkg <> "threads" && not (has_prefix pkg "threads."))
;;


let fixup_thread_base predlist pkg =
  (* Add the package "threads" if required *)
  if fixup_thread_needed_1 predlist && fixup_thread_needed_2 pkg then
    [ "threads" ]
  else
    []
;;


let query_requirements ~preds:predlist package_name =
  (* Part of [requires] implementation: Load all required packages, but
   * do not add relations
   *)
  let m = query package_name in
    (* may raise No_such_package *)
  let r =
    try Fl_metascanner.lookup "requires" predlist m.package_defs
	with Not_found -> ""
  in
  let ancestors = Fl_split.in_words r @
		  fixup_thread_base predlist package_name in
  List.iter
    (fun p ->
      try
	let _ = query p in      (* may raise No_such_package *)
        ()
      with
	  No_such_package(pname,_) ->
	    raise(No_such_package(pname, "required by `" ^ package_name ^ "'"))
    )
    ancestors;
  ancestors
;;


let add_relations s ancestors package_name =
  (* Part of [requires] implementation: Adds the relations from [package_name]
   * to [ancestors]. Target store is [s].
   *)
  List.iter
    (fun p ->
      try
	Fl_metastore.let_le s p package_name  (* add relation *)
      with
	| Fl_topo.Inconsistent_ordering ->
	    raise(Package_loop p)
	| Not_found ->
	    (* A relation to a package not part of [s]. We ignore it here. *)
	    ()
    )
    ancestors
;;


let add_all_relations predlist s =
  (* Adds all relations for the packages currently defined in [s].
     Note that missing requirements are not reported immediately (we do
     not know here which part of the graph [s] is really accessed), and
     instead the error is added to the missing_reqs field, where
     it should be checked before used.
   *)
  let pkgs = ref [] in
  Fl_metastore.iter_up
    (fun p -> pkgs := p :: !pkgs)
    s;

  List.iter
    (fun p ->
       let pkg = p.package_name in
       try
	 let pkg_ancestors = query_requirements predlist pkg in
	 add_relations s pkg_ancestors pkg
       with
	 | No_such_package(n,reason) ->
	     p.package_priv.missing_reqs <- 
	       (n,reason) :: p.package_priv.missing_reqs
    )
    !pkgs
;;


let fixup_thread_deps s =
  (* All packages (except "threads", "threads.*", and "unix") are made
   * dependent on "threads"
   *)
  let pkgs = ref [] in
  Fl_metastore.iter_up
    (fun p -> pkgs := p.package_name :: !pkgs)
    s;

  List.iter
    (fun pkg ->
       if fixup_thread_needed_2 pkg then (
	 try
	   Fl_metastore.let_le s "threads" pkg  (* add relation *)
	 with
	     Not_found ->
	       (* Because "threads" does not exist! Normally this is an
		* error, because "threads" is also magically added by
		* query_requirements. However, there are situations
		* where it cannot be expected that required packages
		* are loaded, so ignore this case.
		*)
	       ()
       )
    )
    !pkgs
;;


let requires ~preds:predlist package_name =
  (* returns names of packages required by [package_name], the fully qualified
   * name of the package. It is checked that the packages really exist.
   * [predlist]: list of true predicates
   * May raise [No_such_package] or [Package_loop].
   *)
  let ancestors = query_requirements predlist package_name in
  let store' = Fl_metastore.copy store in     (* work with a copy *)
  add_relations store' ancestors package_name;
  if List.mem "mt" predlist then fixup_thread_deps store';
  ancestors
;;


let requires_deeply ~preds:predlist package_list =
  (* returns names of packages required by the packages in [package_list],
   * either directly or indirectly.
   * It is checked that the packages really exist.
   * The list of names is sorted topologically; first comes the deepest
   * ancestor.
   * [predlist]: list of true predicates
   * - raises [Not_found] if there is no 'package'
   * - raises [Failure] if some of the ancestors do not exist
   *)

  let pkgset = ref StringSet.empty in

  let rec query_packages pkglist =
    match pkglist with
      pkg :: pkglist' ->
	if not(StringSet.mem pkg !pkgset) then begin
	  let pkg_ancestors = query_requirements predlist pkg in
	  pkgset := StringSet.add pkg !pkgset;
          query_packages pkg_ancestors
	end;
	query_packages pkglist'
    | [] ->
	()
  in

  (* First query for all packages, such that they are loaded: *)
  query_packages package_list;

  (* Now make a copy of the store, and add the relations: *)
  let store' = Fl_metastore.copy store in
  add_all_relations predlist store';
  if List.mem "mt" predlist then fixup_thread_deps store';

  (* Finally, iterate through the graph. Note that the graph may
   * contain more members than required, so we have to test explicitly
   * whether the packages are contained in pkgset.
   *)

  let l = ref [] in

  Fl_metastore.iter_up_at
    (fun m ->
       if StringSet.mem m.package_name !pkgset then (
	 if m.package_priv.missing_reqs <> [] then (
	   let (n,reason) = List.hd m.package_priv.missing_reqs in
	   raise(No_such_package(n,reason))
	 );
	 l := m.package_name :: !l
       )
    )
    store'
    package_list;

  List.rev !l
;;


(**********************************************************************)

(* The following two functions do not use !ocamlpath, because there may
 * be duplicates in it.
 *)

let package_definitions ~search_path package_name =
  (* Return all META files defining this [package_name] that occur in the
   * directories mentioned in [search_path]
   *)

  let package_name_comps = Fl_split.package_name package_name in
  if package_name_comps = [] then invalid_arg "Fl_package_base.package_definitions";
  let main_name = List.hd package_name_comps in

  let rec run_ocamlpath path =
    match path with
      [] -> []
    | dir :: path' ->
	let package_dir = Filename.concat dir main_name in
	let meta_file_1 = Filename.concat package_dir "META" in
	let meta_file_2 = Filename.concat dir ("META." ^ main_name) in
	if Sys.file_exists meta_file_1 then
	  meta_file_1 :: run_ocamlpath path'
	else
	  if Sys.file_exists meta_file_2 then
	    meta_file_2 :: run_ocamlpath path'
	  else
	    run_ocamlpath path'
  in
  run_ocamlpath search_path
;;


let in_report_search_path identify_dir d =
  (* Whether package dir d is to be considered for generating reports.
     d is sorted out when the ignore_dups_in option is set
   *)
  List.for_all
    (fun id ->
      try identify_dir d <> identify_dir id
      with _ -> Fl_split.norm_dir d <> Fl_split.norm_dir id
    )
    !conf_ignore_dups_in
;;


let package_conflict_report_1 identify_dir () =
  let remove_dups_from_path p =
    (* Removes directories which are physically the same from the path [p],
     * and returns the shortened path
     *)

    let dir_identity = Hashtbl.create 20 in

    let rec remove p =
      match p with
	  d :: p' ->
	    begin try
	      let id = identify_dir d in   (* may raise exceptions *)
	      if Hashtbl.mem dir_identity id then
		remove p'
	      else begin
		Hashtbl.add dir_identity id ();
		d :: (remove p')
	      end
	    with error ->
	      (* Don't know anything, so the "directory" remains in the path *)
	      d :: (remove p')
	    end
	| [] ->
	    []
    in

    remove p
  in

  (* If we have ignore_dups_in this directory is removed from our search
     path first
   *)
  let search_path0 =
    List.filter (in_report_search_path identify_dir) !ocamlpath in

  (* Now eliminate all duplicates *)
  let search_path =
    remove_dups_from_path search_path0 in

  Fl_metastore.iter_up
    (fun pkg ->
       (* Check only main packages: *)
       let package_name_comps = Fl_split.package_name pkg.package_name in
       match package_name_comps with
	   [_] ->
	     (* pkg is a main package *)
	     ( let c = package_definitions search_path pkg.package_name in
	       match c with
		   []
		 | [_] ->
		     ()
		 | _ ->
		     Printf.eprintf "findlib: [WARNING] Package %s has multiple definitions in %s\n"
		     pkg.package_name
		     (String.concat ", " c)
	     )
	 | _ ->
	     ()
    )
    store;
  flush stderr
;;


let package_conflict_report ?identify_dir () =
  match identify_dir with
      None   -> package_conflict_report_1 (fun s -> s) ()
    | Some f -> package_conflict_report_1 f ()
;;

let check_prefix ?prefix f =
  match prefix with
  | None -> true
  | Some prefix ->
    let len = String.length prefix in
    String.length f >= len && String.sub f 0 len = prefix

let load_base ?prefix () =
  (* Ensures that the cache is completely filled with every package
   * of the system that match prefix
  *)
  let list_directory d =
    try
      Array.to_list(Sys.readdir d)
    with
	Sys_error msg ->
	  prerr_endline ("findlib: [WARNING] cannot read directory " ^ msg);
	  []
  in

  let process_file ?directory_required main_name package_dir meta_file =
    try
      let _ = Fl_metastore.find store main_name in
      (* Note: If the main package is already loaded into the graph, we
       * do not even look at the subpackages!
       *)
      ()
    with
	Not_found ->
	  let packages =
	  try
	    packages_in_meta_file
	      ?directory_required ~name:main_name ~dir:package_dir ~meta_file ()
	  with
	      Failure s ->
		prerr_endline ("findlib: [WARNING] " ^ s); []
	  in
	  List.iter (Fl_metastore.add store) packages;
	    (* Nothing evil can happen! *)
  in

  let rec run_ocamlpath path =
    match path with
      [] -> ()
    | dir :: path' ->
        let files = list_directory dir in
	List.iter
          (fun f ->
             if check_prefix ?prefix f
             then
	     (* If f/META exists: Add package f *)
	     let package_dir = Filename.concat dir f in
	     let meta_file_1 = Filename.concat package_dir "META" in
	     if Sys.file_exists meta_file_1 then
	       process_file f package_dir meta_file_1
	     else
	       (* If f is META.pkgname: Add package pkgname *)
	       (* We skip over filenames ending in '~' *)
	       if String.length f >= 6 && String.sub f 0 5 = "META." &&
		  String.sub f (String.length f - 1) 1 <> "~" then begin
		 let name = String.sub f 5 (String.length f - 5) in
		 let meta_file_2 = Filename.concat dir f in
		 process_file ~directory_required:true name dir meta_file_2
	       end;
	  )
	  files;
	run_ocamlpath path'
  in

  run_ocamlpath !ocamlpath
;;


let list_packages ?prefix () =
  load_base ?prefix ();

  let l = ref [] in

  Fl_metastore.iter_up
    (fun m ->
       if check_prefix ?prefix m.package_name then
         l := m.package_name :: !l
    )
    store;

  !l
;;


let package_users ~preds pl =
  (* Check that all packages in [pl] really exist, or raise No_such_package: *)
  List.iter
    (fun p -> let _ = query p in ())
    pl;
  load_base();
  let store' = Fl_metastore.copy store in
  add_all_relations preds store';
  if List.mem "mt" preds then fixup_thread_deps store';

  let l = ref [] in

  Fl_metastore.iter_down_at
    (fun m ->
       if m.package_priv.missing_reqs <> [] then (
	 let (n,reason) = List.hd m.package_priv.missing_reqs in
	 raise(No_such_package(n,reason))
       );
       l := m.package_name :: !l
    )
    store'
    pl;

  !l
;;


let module_conflict_report_1 identify_dir incpath =
  (* Find any *.cmi files occurring twice in incpath.
   *)
  let dir_of_module = Hashtbl.create 100 in
  let dirs = ref [] in

  let examine_dir d =
    try
      let d    = Fl_split.norm_dir d in
      let d_id = identify_dir d in
      
      (* Is d new? *)
      if not (List.mem d_id !dirs) then begin
	dirs := d_id :: !dirs;
	(* Yes: Get all files ending in .cmi *)
	try
	  let d_all = Array.to_list(Sys.readdir d) in   (* or Sys_error *)
	  let d_cmi = 
	    List.filter
	      (fun n -> Filename.check_suffix n ".cmi")
	      d_all in
	  (* Add the modules to dir_of_module: *)
	  List.iter
	    (fun m ->
	       try
		 let entry = Hashtbl.find dir_of_module m in (* or Not_found *)
		 entry := d :: !entry
	       with
		   Not_found ->
		     Hashtbl.add dir_of_module m (ref [d])
	    )
	    d_cmi
	with
	    Sys_error msg ->
	      prerr_endline ("findlib: [WARNING] cannot read directory " ^ msg)
      end
    with
      | _ -> ()  (* identify_dir fails *)
  in

  let print_report() =
    Hashtbl.iter
      (fun m dlist ->
	 match !dlist with
	     []
	   | [_] ->
	       ()
	   | _ ->
	       Printf.eprintf "findlib: [WARNING] Interface %s occurs in several directories: %s\n"
		 m
		 (String.concat ", " !dlist)
      )
      dir_of_module
  in

  (* If we have ignore_dups_in this directory is removed from our search
     path first
   *)
  let incpath1 =
    List.filter (in_report_search_path identify_dir) incpath in


  List.iter examine_dir incpath1;

  print_report();
  flush stderr
;;


let module_conflict_report ?identify_dir incpath =
  match identify_dir with
      None   -> module_conflict_report_1 (fun s -> s) incpath
    | Some f -> module_conflict_report_1 f incpath
;;
