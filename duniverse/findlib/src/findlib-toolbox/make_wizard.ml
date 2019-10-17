(* $Id$
 * ----------------------------------------------------------------------
 *
 *)


open Tk;;
open Widget;;

(**********************************************************************)
(* GLOBAL VARIABLES                                                   *)
(**********************************************************************)

(* General *)

let wiz_package_name = ref "";;
let wiz_package_version = ref "";;
let wiz_package_description = ref "";;

(* Preprocessor *)

let wiz_enable_camlp4 = ref false;;
let wiz_camlp4_syntax = ref "camlp4o";;
let wiz_camlp4_extensions = ref [];;  (* list of package names *)
let wiz_camlp4_selected = ref [];;    (* subset of wiz_camlp4_extensions *)
let wiz_camlp4_options = ref "";;

(* Prerequisites *)

let wiz_all_packages = ref [];;       (* list of package names *)
let wiz_required_packages = ref [];;  (* subset of wiz_all_packages *)

(* Build Library *)

let wiz_available = ref [];;          (* list of module names *)
let wiz_byte_enable = ref true;;
let wiz_nat_enable = ref true;;
let wiz_objects = ref [];;            (* subset of wiz_available *)
let wiz_source_suffixes = ref ".ml .mli .mll .mly";;

(* Build Executables *)

let wiz_executables = ref [];;        (* list of executable names *)
let wiz_exec_objects = (ref [] : (string * (string list ref)) list ref);;       
  (* an alist: for every executable, the corresponding list of modules is
   * stored.
   *)
let wiz_exec_native = (ref [] : (string * bool ref) list ref);;
  (* an alist: for every executable, whether it is natively compiled or not *)

(* Generate *)

let wiz_makefile_name = ref "Makefile";;
let wiz_local_makefile_name = ref "";;
let wiz_make_default = ref "byte" ;;


(**********************************************************************)
(* AUXILIARY FUNCTIONS                                                *)
(**********************************************************************)

let find_pos x l =
  let rec find k l =
    match l with
	h :: l' ->
	  if x = h then k else find (k+1) l'
      | [] ->
	  raise Not_found
  in
  find 0 l
;;


let rec delete_at k l =
  match l with
      h :: l' -> if k <= 0 then l' else h :: (delete_at (k-1) l')
    | []      -> []
;;
    

let rec insert_at k x l =
  if k <= 0 then
    x :: l
  else
    match l with
	h :: l' -> h :: (insert_at (k-1) x l')
      | []      -> []   (* insert beyond end *)
;;


let rec remove_dups l =
  (* Remove duplicate members in a sorted list *)
  match l with
      x :: (y :: l' as l1) when x = y ->
	remove_dups l1
    | x :: l' ->
	x :: remove_dups l'
    | [] ->
	[]
;;


(**********************************************************************)
(* SAVE/LOAD STATE                                                    *)
(**********************************************************************)

let save_var name printer out var =
  output_string out ("V" ^ name ^ "\n");
  printer out var
;;

let save_string out var =
  (* [var] must not contain linefeeds *)
  output_string out ("S" ^ var ^ "\n")
;;

let save_bool out var =
  output_string out ("B" ^ string_of_bool var ^ "\n")
;;

let save_list printer out var =
  output_string out ("L" ^ string_of_int (List.length var) ^ "\n");
  List.iter (printer out) var
;;

let save_pair lprinter rprinter out (lvar,rvar) =
  output_string out "P\n";
  lprinter out lvar;
  rprinter out rvar
;;

let save_ref printer out var =
  (* Actually doesn't save the reference! *)
  printer out !var
;;

let save_state out =
  save_var "wiz_package_name" save_string out !wiz_package_name;
  save_var "wiz_package_version" save_string out !wiz_package_version;
  save_var "wiz_package_description" save_string out !wiz_package_description;
  save_var "wiz_enable_camlp4" save_bool out !wiz_enable_camlp4;
  save_var "wiz_camlp4_syntax" save_string out !wiz_camlp4_syntax;
  save_var "wiz_camlp4_extensions" (save_list save_string) out
    !wiz_camlp4_extensions;
  save_var "wiz_camlp4_selected"  (save_list save_string) out
    !wiz_camlp4_selected;
  save_var "wiz_camlp4_options" save_string out !wiz_camlp4_options;
  save_var "wiz_all_packages" (save_list save_string) out
    !wiz_all_packages;
  save_var "wiz_required_packages" (save_list save_string) out
    !wiz_required_packages;
  save_var "wiz_available" (save_list save_string) out
    !wiz_available;
  save_var "wiz_byte_enable" save_bool out !wiz_byte_enable;
  save_var "wiz_nat_enable" save_bool out !wiz_nat_enable;
  save_var "wiz_objects" (save_list save_string) out
    !wiz_objects;
  save_var "wiz_source_suffixes" save_string out !wiz_source_suffixes;
  save_var "wiz_executables" (save_list save_string) out
    !wiz_executables;
  save_var "wiz_exec_objects" 
    (save_list (save_pair save_string (save_ref (save_list save_string))))
    out
    !wiz_exec_objects;
  save_var "wiz_exec_native"
    (save_list (save_pair save_string (save_ref save_bool)))
    out
    !wiz_exec_native;
  save_var "wiz_makefile_name" save_string out !wiz_makefile_name;
  save_var "wiz_local_makefile_name" save_string out !wiz_local_makefile_name;
  save_var "wiz_make_default" save_string out !wiz_make_default;
;;

let save() =
  let f = open_out ".make-wizard" in
  save_state f;
  close_out f
;;

let check_char inch c_expected =
  let c = input_char inch in
  if c <> c_expected then failwith "Cannot read .make-wizard"
;;

let load_string inch =
  check_char inch 'S';
  let line = input_line inch in
  (* prerr_endline ("String = " ^ line); *)
  line
;;

let load_bool inch =
  check_char inch 'B';
  bool_of_string(input_line inch)
;;

let load_list parse inch =
  check_char inch 'L';
  let n = int_of_string(input_line inch) in
  let l = ref [] in
  for i = 1 to n do
    l := parse inch :: !l;
  done;
  List.rev !l
;;

let load_pair lparse rparse inch =
  check_char inch 'P';
  ignore(input_line inch);
  let l = lparse inch in
  let r = rparse inch in
  (l,r)
;;

let load_ref parse inch =
  ref(parse inch)
;;

let load_var var parse inch =
  let value = parse inch in
  var := value
;;

let load_variables spec inch =
  try
    while true do
      try
	check_char inch 'V';
        let name = input_line inch in
	(* prerr_endline name;*)
	let loader = List.assoc name spec in  (* or Not_found *)
	loader inch
      with
	  Not_found ->
	    ()
    done;
    assert false
  with
      End_of_file -> 
	()
;;

let load_state inch =
  load_variables
    [ "wiz_package_name", 
         (load_var wiz_package_name load_string);
      "wiz_package_version",
         (load_var wiz_package_version load_string);
      "wiz_package_description",
         (load_var wiz_package_description load_string);
      "wiz_enable_camlp4",
         (load_var wiz_enable_camlp4 load_bool);
      "wiz_camlp4_syntax",
         (load_var wiz_camlp4_syntax load_string);
      "wiz_camlp4_extensions",
	 (load_var wiz_camlp4_extensions (load_list load_string));
      "wiz_camlp4_selected",
         (load_var wiz_camlp4_selected (load_list load_string));
      "wiz_camlp4_options",
         (load_var wiz_camlp4_options load_string);
      "wiz_all_packages",
         (load_var wiz_all_packages (load_list load_string));
      "wiz_required_packages",
         (load_var wiz_required_packages (load_list load_string));
      "wiz_available",
         (load_var wiz_available (load_list load_string));
      "wiz_byte_enable",
         (load_var wiz_byte_enable load_bool);
      "wiz_nat_enable",
         (load_var wiz_nat_enable load_bool);
      "wiz_objects",
         (load_var wiz_objects (load_list load_string));
      "wiz_source_suffixes",
         (load_var wiz_source_suffixes load_string);
      "wiz_executables",
         (load_var wiz_executables (load_list load_string));
      "wiz_exec_objects",
         (load_var wiz_exec_objects (load_list
				       (load_pair
					  load_string
					  (load_ref (load_list load_string)))));
      "wiz_exec_native",
         (load_var wiz_exec_native (load_list
				      (load_pair
					 load_string
					 (load_ref load_bool))));
      "wiz_makefile_name",
         (load_var wiz_makefile_name load_string);
      "wiz_local_makefile_name",
         (load_var wiz_local_makefile_name load_string);
      "wiz_make_default",
         (load_var wiz_make_default load_string);
    ]
    inch
;;


let load() =
  let f = open_in ".make-wizard" in
  load_state f;
  close_in f
;;


(**********************************************************************)
(* PARSE PATTERN FILE                                                 *)
(**********************************************************************)

type sectiondata =
    Sect_const of string
  | Sect_var of string

let section_re = Str.regexp "^\\[\\([A-Za-z_0-9-]+\\)\\]$" ;;
let var_re = Str.regexp "\\[\\([A-Za-z_0-9-]+\\)\\]" ;;

let parse_pattern inch =
  let rec parse_section name sect =
    try
      let line = input_line inch in
      if String.length line >= 2 && line.[0] = '#' && line.[1] = '#' then
	(* Comment line *)
	parse_section name sect
      else
	if Str.string_match section_re line 0 then
	  (* New section begins *)
	  let name' = Str.matched_group 1 line in
	  (name, List.rev sect) :: parse_section name' []
	else
	  (* Normal data region *)
	  let plist = Str.full_split var_re line in
	  let slist =
	    List.map
	      (function
		   Str.Text t -> Sect_const t
		 | Str.Delim d -> Sect_var (String.sub d 1 (String.length d - 2))
	      )
	      plist @ [ Sect_const "\n" ] in
	  parse_section name (List.rev slist @ sect)
    with
	End_of_file ->
	  [ name, List.rev sect ]
  in

  parse_section "_preamble_" []
;;

let load_pattern() =
  let where = Filename.dirname (Sys.argv.(0)) in
  let name = Filename.concat where "make_wizard.pattern" in
  let f = open_in name in
  let p = parse_pattern f in
  close_in f;
  p
;;

(**********************************************************************)
(* MAKEFILE GENERATOR                                                 *)
(**********************************************************************)

let dollar_re = Str.regexp "\\$";;
let meta_re = Str.regexp "[\\\\\\\"]";;

let mkquote s =
  (* Quote "$" *)
  Str.global_replace dollar_re "$$" s
;;

let metaquote s =
  (* Quote backslash and double quotes for META files *)
  Str.global_replace meta_re "\\\\0" s
;;

let makemake() =
  let b = Buffer.create 1024 in
  let sections = load_pattern() in

  let write section vars =
    let sectlist = 
      try List.assoc section sections
      with Not_found -> failwith ("Cannot find section: " ^ section) in
    List.iter
      (function
	   Sect_const s -> 
	     Buffer.add_string b s
	 | Sect_var v ->
	     let s = 
	       try List.assoc v vars 
	       with Not_found -> failwith ("No such variable: " ^ v) in
	     Buffer.add_string b s
      )
      sectlist
  in
  
  let is_byte_exec execname =
    try not (!(List.assoc execname !wiz_exec_native))
    with Not_found -> true
  in

  let byte_execs =
    List.map
      fst
      (List.filter
	 (fun (execname,_) -> is_byte_exec execname)
	 !wiz_exec_objects
      )
  in
  
  let nat_execs =
    List.map
      fst
      (List.filter
	 (fun (execname,_) -> not(is_byte_exec execname))
	 !wiz_exec_objects
      )
  in
  
  let byte_exec_modules =
    remove_dups
      (List.sort compare
	 (List.flatten
	    (List.map
	       (fun (_, l) -> !l)
	       (List.filter
		  (fun (execname, _) -> is_byte_exec execname)
		  !wiz_exec_objects
	       )
	    )
	 )
      )
  in

  let nat_exec_modules =
    remove_dups
      (List.sort compare
	 (List.flatten
	    (List.map
	       (fun (_, l) -> !l)
	       (List.filter
		  (fun (execname, _) -> not(is_byte_exec execname))
		  !wiz_exec_objects
	       )
	    )
	 )
      )
  in

  let required_packages =
    (* magically add "camlp4" if missing *)
    if !wiz_enable_camlp4 then (
      ( if not (List.mem "camlp4" !wiz_required_packages) then
	  [ "camlp4" ]
	else
	  []
      ) @ !wiz_camlp4_selected @ !wiz_required_packages
    )
    else
      !wiz_required_packages
  in

  let variables =
    [ "name", 
          mkquote !wiz_package_name;
      "makefile_name", 
          mkquote !wiz_makefile_name;
      "version", 
          mkquote(metaquote !wiz_package_version);
      "description", 
          mkquote(metaquote !wiz_package_description);
      "byte_objects", 
          String.concat " " (List.map 
			       (fun m -> String.uncapitalize m ^ ".cmo")
			       !wiz_objects);
      "nat_objects",
          String.concat " " (List.map 
			       (fun m -> String.uncapitalize m ^ ".cmx")
			       !wiz_objects);
      "byte_executables",
          String.concat " " byte_execs;
      "byte_exec_objects",
          String.concat " " (List.map 
			       (fun m -> String.uncapitalize m ^ ".cmo")
			       byte_exec_modules);
      "nat_executables",
          String.concat " " nat_execs;
      "nat_exec_objects",
          String.concat " " (List.map 
			       (fun m -> String.uncapitalize m ^ ".cmx")
			       nat_exec_modules);
      "prereqs",
          String.concat " " required_packages;
      "ppopts",
          if !wiz_enable_camlp4 then
	    "-syntax " ^ !wiz_camlp4_syntax ^
	    (String.concat " "
	       (List.map
		  (fun opt -> " -ppopt " ^ mkquote(Filename.quote opt))
		  (Fl_split.in_words_ws !wiz_camlp4_options)
	       ))
	  else
            ""; 
      "mtopts",
          if List.mem "threads" !wiz_required_packages then "-thread" else "";
      "default_target",
          !wiz_make_default;
    ] in

  write "intro" variables;
  write "def_general" variables;
  if !wiz_byte_enable then write "def_byte_archive" variables;
  if !wiz_nat_enable  then write "def_native_archive" variables;
  write "def_byte_exec" variables;
  write "def_nat_exec" variables;
  write "def_props" variables;
  write "def_tools" variables;
  write "rules" variables;
  write "default_target" variables;
  write "suffix_rules" variables;
  write "generate" variables;
  List.iter
    (fun (execname, modlist) ->
       let switches =
	 if is_byte_exec execname then
	   "-bytecode-filter" 
	 else 
	   "-native-filter" in
       let deptargets =
	 String.concat " " (List.map 
			      (fun m -> 
				 let m' = String.uncapitalize m in
				 m' ^ ".ml " ^ m' ^ ".mli")
			      !modlist) in
       write "makemake_exec" ( [ "switches", switches;
				 "execname", execname;
				 "deptargets", deptargets ] @ variables )
    )
    !wiz_exec_objects;
  write "byte" variables;
  write "opt" variables;
  if !wiz_byte_enable then write "byte_archive" variables;
  if !wiz_nat_enable  then write "native_archive" variables;
  List.iter
    (fun (execname, modlist) ->
       if is_byte_exec execname then begin
	 let execobjs = 
	   String.concat " " (List.map 
				(fun m -> String.uncapitalize m ^ ".cmo")
				!modlist) in
	 write "byte_exec" ( ["execname", execname;
			      "execobjs", execobjs ] @ variables )
       end
    )
    !wiz_exec_objects;
  List.iter
    (fun (execname, modlist) ->
       if not (is_byte_exec execname) then begin
	 let execobjs = 
	   String.concat " " (List.map 
				(fun m -> String.uncapitalize m ^ ".cmx")
				!modlist) in
	 write "nat_exec" ( ["execname", execname;
			     "execobjs", execobjs ] @ variables )
       end
    )
    !wiz_exec_objects;
  write "clean" variables;
  write "install" variables;

  if !wiz_local_makefile_name<> "" &&
      Sys.file_exists !wiz_local_makefile_name then 
  begin
    write "local" variables;
    let f = open_in !wiz_local_makefile_name in
    try
      while true do
	let s = input_line f in
	Buffer.add_string b s;
	Buffer.add_char b '\n';
      done;
      assert false
    with
	End_of_file -> 
	  close_in f
  end;

  Buffer.contents b
;;

(**********************************************************************)
(* GUI                                                                *)
(**********************************************************************)

let headline_font = "-*-helvetica-bold-r-normal-*-*-140-*-*-*-*-iso8859-1" ;;
let font = "-*-helvetica-medium-r-normal-*-*-120-*-*-*-*-iso8859-1" ;;

(**********************************************************************)

let top = ref Widget.default_toplevel;;
let topframe = ref Widget.dummy;;
let screens = ref [];;
let current_screen = ref 0;;

let ( !! ) = fun x -> !(!x);;

let add_screen func =
  screens := !screens @ [ func ]
;;

let add_headline frame text =
  let s1 = Frame.create ~height:15 frame in
  let w = Label.create ~text ~font:headline_font ~anchor:`W frame in
  let s2 = Frame.create ~height:10 frame in
  pack [ s1 ];
  pack ~anchor:`W [ w ];
  pack [ s2 ]
;;

let add_para frame text =
  let s1 = Frame.create ~height:5 frame in
  let w = Message.create ~padx:0 ~text ~font ~anchor:`W ~width:(pixels (`Pt 400.0)) frame in
  let s2 = Frame.create ~height:5 frame in
  pack [ s1 ];
  pack ~anchor:`W [ w ];
  pack [ s2 ]
;;


let dialog ~parent ~title ~message ~buttons ?(default = (-1)) () =
  (* Like Dialog.create, but our own style. *)
  let popup = Toplevel.create parent in
  Wm.title_set popup title;
  Wm.transient_set popup ~master:(Winfo.toplevel parent);
  add_headline popup title;
  add_para popup message;
  let f_buttons = Frame.create popup in
  let n = ref 0 in
  let r = ref (-1) in
  List.iter
    (fun text ->
       let k = !n in
       let b = 
	 Button.create ~font ~text ~command:(fun () -> r := k; destroy popup) 
	   f_buttons in
       (* --- Default buttons not yet supported because of deficiency in
	* labltk:
       if k = default then Button.configure ~default:`Active b;
	*)
       pack ~side:`Left [b];
       incr n;
    )
    buttons;
  pack [f_buttons];
  Grab.set popup;
  Tkwait.window popup;
  !r
;;


let ask_and_save frame =
  let reply = 
    dialog ~parent:frame ~title:"Save .make-wizard?"
      ~message:"Do you want to save the current state in the file .make-wizard?"
      ~buttons:["Yes"; "No"; "Cancel"]
      ~default:0
      ()
  in
  if reply = 0 then save();
  reply < 2
;;


let string_tv ?(onchange = fun () -> ()) frame v =
  let textvariable = Textvariable.create ~on:frame () in
  Textvariable.set textvariable !v;
  let rec set_handle() =
    Textvariable.handle textvariable 
      ~callback:(fun () -> 
		   v := Textvariable.get textvariable; 
		   onchange(); 
		   set_handle());
  in
  set_handle();
  textvariable
;;


let bool_tv ?(onchange = fun () -> ()) frame v =
  let textvariable = Textvariable.create ~on:frame () in
  Textvariable.set textvariable (if !v then "1" else "0");
  let rec set_handle() =
    Textvariable.handle textvariable 
      ~callback:(fun () -> 
		   v := Textvariable.get textvariable = "1"; 
		   onchange(); 
		   set_handle());
  in
  set_handle();
  textvariable
;;


let label_box frame box =
  let sub = Frame.create frame in
  let row = ref 0 in
  List.iter
    (fun (l, v) ->
       let label = Label.create ~font ~text:l ~anchor:`E sub in
       let textvariable = string_tv frame v in
       let var = Entry.create ~font ~textvariable ~width:40 sub in
       grid ~row:!row ~column:0 ~sticky:"e" [ label ];
       grid ~row:!row ~column:1 ~sticky:"w" [ var ];
       incr row;
    )
    box;
  pack ~anchor:`W [ sub ]
;;


let scrolled_listbox ?(click = fun _ _ -> ()) ?(context = fun _ _ -> ()) 
                     ?(separator = true) ?(height = 8) frame =
  let f = Frame.create frame in
  let lb = Listbox.create ~selectmode:`Multiple ~width:20 ~height
	     ~exportselection:false f in
  let sb = Scrollbar.create ~orient:`Vertical 
	   ~command:(Listbox.yview lb)
	   f in
  Listbox.configure ~yscrollcommand:(Scrollbar.set sb) ~font lb;
  let sep = Frame.create ~width:30 f in
  pack ~side:`Left [ lb ];
  pack ~side:`Left ~fill:`Y [ sb ];
  if separator then pack ~side:`Left [ sep ];

  bind ~events:[ `ButtonPressDetail 1 ] ~fields:[ `MouseY ] 
       ~action:(fun einfo ->
		  let `Num row = Listbox.nearest lb ~y:(einfo.Tk.ev_MouseY) in
		  Timer.set ~ms:0 ~callback:(fun () -> click lb row)
	       )
       lb;
  bind ~events:[ `ButtonPressDetail 3 ] ~fields:[ `MouseY ] 
       ~action:(fun einfo ->
		  let `Num row = Listbox.nearest lb ~y:(einfo.Tk.ev_MouseY) in
		  Timer.set ~ms:0 ~callback:(fun () -> context lb row)
	       )
       lb;

  (f, lb)
;;


let listbox_select lb selection =
  Listbox.selection_clear lb ~first:(`Num 0) ~last:`End;
  for i = 0 to Listbox.size lb do
    let s = Listbox.get lb (`Num i) in
    if List.mem s selection then
      Listbox.selection_set lb ~first:(`Num i) ~last:(`Num i)
  done
;;


let listbox_get_selection lb =
  List.map
    (fun index -> Listbox.get lb ~index)
    (Listbox.curselection lb)
;;


let indented_cond_frame frame enablers =
  (* Returns a subframe of [frame] that is only mapped if the boolean
   * variable [!enable] is true. [enable_text] is the text for the
   * checkbox that toggles [!enable].
   *)
  let onchange_enable = ref (fun () -> ()) in

  List.iter
    (fun (enable, enable_text) ->
       let enable_tv = bool_tv 
		       ~onchange:(fun () -> !onchange_enable())
		       frame enable in
       let cb = Checkbutton.create 
		~font ~text:enable_text ~variable:enable_tv frame in
       pack ~anchor:`W [cb];
    )
    enablers;

  let is_enabled() =
    let p = ref false in
    List.iter (fun (enable, _) -> p := !p || !enable) enablers;
    !p
  in
  
  let frame' = Frame.create frame in
  let indent = Frame.create ~width:30 frame' in
  let frame'' = Frame.create frame' in
  pack ~anchor:`W [frame'];

  let frame''_is_packed = ref false in
  let frame''_pack b =
    if b <> !frame''_is_packed then begin
      if b then 
	pack ~side:`Left [indent; frame''] 
      else
	Pack.forget [ frame'' ];
      frame''_is_packed := b
    end
  in
  frame''_pack (is_enabled());
  onchange_enable := (fun () -> frame''_pack (is_enabled()));
  frame''
;;


let double_listbox frame available objects scan =
  let boxes = Frame.create frame in
  let (f_left,lb_left)   = scrolled_listbox boxes in
  let (f_right,lb_right) = scrolled_listbox ~separator:false boxes in
  let f_buttons = Frame.create boxes in
  let b_add = Button.create ~font ~text:"Add >>" f_buttons in
  let b_del = Button.create ~font ~text:"<< Del" f_buttons in
  let b_up  = Button.create ~font ~text:"Up ^" f_buttons in
  let b_down= Button.create ~font ~text:"Down v" f_buttons in
  let b_scan= Button.create ~font ~text:"Rescan" f_buttons in
  let f_sep = Frame.create ~width:30 boxes in
  pack ~fill:`X [ b_add; b_del; b_up; b_down; b_scan ];
  pack ~side:`Left [ coe f_left; coe f_buttons; coe f_sep; coe f_right ];
  pack [ boxes ];
    
  let update() =
    let still_available = (* available - objects *)
      List.filter (fun m -> not (List.mem m !!objects)) !!available in
    Listbox.delete lb_left ~first:(`Num 0) ~last:`End;
    Listbox.insert lb_left ~index:`End ~texts:still_available;
    Listbox.delete lb_right ~first:(`Num 0) ~last:`End;
    Listbox.insert lb_right ~index:`End ~texts:!!objects;
  in
  
  let rescan() =
    !available := scan();
    update()
  in

  let add() =
    if Listbox.curselection lb_left = [] then begin
      let popup = Toplevel.create frame in
      Wm.title_set popup "Add File";
      Wm.transient_set popup ~master:(Winfo.toplevel frame);
      add_headline popup "Add File";
      add_para popup "Enter the name of the module to add. Note that you can also select the modules in the left box and press 'Add' to quickly move the modules to the right box.";
      
      let modname = ref "" in
      label_box popup [ "Name of new module: ", modname ];

      let p_buttons = Frame.create popup in
      let b_ok = Button.create ~font ~text:"OK" 
		   ~command:(fun () -> 
			       if !modname <> "" then
				 !objects := !!objects @ [ !modname ];
			       update();
			       destroy popup) p_buttons in
      let b_cancel = Button.create ~font ~text:"Cancel"
		       ~command:(fun () -> destroy popup) p_buttons in
      pack ~side:`Left [b_ok; b_cancel];

      add_para popup "";

      pack [p_buttons];
    end
    else begin
      let items = 
	List.map
	  (fun index -> Listbox.get lb_left ~index)
	  (Listbox.curselection lb_left)
      in
      !objects := !!objects @ items;
      update()
    end
  in

  let del() =
    if Listbox.curselection lb_right = [] then begin
      ignore
	(dialog
	 ~parent:!top
	 ~title:"Nothing selected"
	 ~message:"Please select the modules you want to delete in the right box!"
	 ~buttons:[ "OK" ]
	 ~default:0
	   ())
    end
    else begin
      let items = 
	List.map
	  (fun index -> Listbox.get lb_right ~index)
	  (Listbox.curselection lb_right)
      in
      !objects := List.filter (fun m -> not (List.mem m items)) !!objects;
      update()
    end
  in
  
  let move_dlg() =
    ignore
      (dialog
       ~parent:!top
       ~title:"Bad Selection"
       ~message:"Please select the (single) module you want to move in the right box!"
       ~buttons:[ "OK" ]
       ~default:0
	 ())
  in
  
  let move g () =
    if List.length (Listbox.curselection lb_right) <> 1 then
      move_dlg()
    else begin
      let index = List.hd (Listbox.curselection lb_right) in
      let `Num n = index in
      let n' = g n in
      if n' >= 0 && n' < List.length !!objects then begin
	let item = Listbox.get lb_right ~index:(index :> Tk.listbox_index) in
	!objects := delete_at n !!objects;
	!objects := insert_at n' item !!objects;
	update();
	Listbox.selection_set lb_right ~first:(`Num n') ~last:(`Num n');
      end
    end
  in
  
  if !!available = [] then rescan() else update(); 
  Button.configure ~command:add b_add;
  Button.configure ~command:del b_del;
  Button.configure ~command:(move pred) b_up;
  Button.configure ~command:(move succ) b_down;
  Button.configure ~command:rescan b_scan;
  
  (* Returns the [update] function *)
  update
;;


let redraw() =
  destroy !topframe;
  let f = Frame.create !top in
  let f1 = Frame.create ~width:5 f in
  let f2 = Frame.create f in
  let f3 = Frame.create ~width:5 f in
  pack ~expand:true ~fill:`Y [f];
  pack ~side:`Left [f1];
  pack ~side:`Left ~fill:`Y [f2];
  pack ~side:`Left [f3];
  let func = List.nth !screens !current_screen in
  topframe := coe f;
  func f2
;;


let footer frame =
  let box = Frame.create frame in
  pack ~side:`Bottom ~fill:`X [box];
  let sep1 = Frame.create ~height:15 box in
  let b = Frame.create ~height:1 ~background:`Black box in
  let sep2 = Frame.create ~height:15 box in
  pack [ sep1 ];
  pack ~fill:`X ~expand:true [b];
  pack [ sep2 ];

  let f = Frame.create box in
  let m1 = Frame.create f in
  pack ~fill:`X ~expand:true ~side:`Left [ m1 ];
  let prev_b = 
    Button.create ~font ~text:"Previous" ~state:`Disabled
    ~command:(fun () -> decr current_screen; redraw()) m1 in
  pack ~side:`Left [prev_b];
  if !current_screen > 0 then 
    Button.configure ~state:`Normal prev_b;
  let m2 = Frame.create f in
  let k = ref 0 in
  List.iter
    (fun scr ->
       let k' = !k in
       let b = 
	 Button.create ~font ~text:(string_of_int (!k+1))
	 ~command:(fun () -> current_screen := k'; redraw()) m2 in
       if k' = !current_screen then
	 Button.configure
	   ~background:`Blue ~activebackground:`Blue ~foreground:`White  b;
       pack ~side:`Left [b];
       incr k
    )
    !screens;
  pack ~side:`Left [m2];
  let m3 = Frame.create f in
  pack ~fill:`X ~expand:true ~side:`Left [ m3 ];
  let next_b =
    Button.create ~font ~text:"Next" ~state:`Disabled 
    ~command:(fun () -> incr current_screen; redraw()) m3 in
  pack ~side:`Right [next_b];
  if !current_screen < List.length !screens - 1 then
    Button.configure ~state:`Normal next_b;
  pack ~fill:`X [ f ]
;;


(**********************************************************************)

let first_time = ref true;;

let intro_screen frame =
  add_headline frame "The Makefile and META wizard";
  add_para frame "This wizard helps you creating Makefiles and META \
files for simple projects. It assumes that all your source files \
reside in a single directory, and that all source files are O'Caml \
files (no support for mixed O'Caml/C projects). ocamllex, ocamlyacc, \
and camlp4 are supported.";
  add_para frame "The wizard generates a Makefile, and the Makefile \
produces the META file. The Makefile is not perfect, and is not the ideal \
choice for everybody, but it is a starting point for your project. \
You can later fine-tune the contents of the Makefile by adding your own rules, \
and by overriding the definitions. The Makefile is commented, and not \
overly complicated.";
  add_para frame "The settings you enter here can be stored in a file \
containing the state of the wizard. This file is called .make-wizard. \
It is recommended to use this feature to save the state between the \
wizard sessions.";
  (* "" *)
  
  footer frame;
  update();

  if !first_time && Sys.file_exists ".make-wizard" then begin
    match dialog ~parent:frame ~title:"Load .make-wizard?"
      ~message:"Do you want to load the file .make-wizard, and continue your last session?"
      ~buttons:[ "Yes, please load the file"; "No, start with empty fields" ]
      ~default:0
      ()
    with
	0 -> load()
      | _ -> ()
  end;
  first_time := false;

;;


add_screen intro_screen;;

(**********************************************************************)

let general_screen frame =
  add_headline frame "General";
  add_para frame "Please enter the name of the package, the version number, \
and the description first. The name must be a single, alphanumeric string \
(including _ and -). The version is an arbitrary string, like the description. \
All fields are mandatory."; (* "" *)

  label_box frame 
    [ "Package name: ", wiz_package_name;
      "Package version: ", wiz_package_version;
      "Package description: ", wiz_package_description ];
  footer frame
;;

add_screen general_screen;;

(**********************************************************************)

let pkginfo lb row =  (* when the user right-clicks at a listbox row *)
  let pkg = Listbox.get lb (`Num row) in
  let version = 
    try Findlib.package_property [] pkg "version" with Not_found -> "N/A" in
  let description =
    try Findlib.package_property [] pkg "description" with Not_found -> "N/A"
  in
  let popup = Toplevel.create !top in
  Wm.transient_set popup ~master:(Winfo.toplevel !top);
  let f = Frame.create popup in
  let title = "About " ^ pkg ^ " (" ^ version ^ ")" in
  add_headline f title;
  Wm.title_set popup title;
  add_para f ("Description: " ^ description);
  add_para f "Modules:";
  let click sublb _ = 
    Listbox.selection_clear sublb ~first:(`Num 0) ~last:`End in
  let (f_sublb, sublb) = scrolled_listbox ~click f in
  let modules =
    try
      Fl_split.in_words
	(Findlib.package_property [] pkg "browse_interfaces")
    with
	Not_found ->
	  let dir = Findlib.package_directory pkg in
	  let files = Array.to_list(Sys.readdir dir) in
	  List.map
	    (fun name -> 
	       String.capitalize (Filename.chop_suffix name ".cmi"))
	    (List.filter
	       (fun name -> 
		  Filename.check_suffix name ".cmi")
	       files
	    )
  in
  Listbox.insert sublb ~index:`End ~texts:modules;
  pack ~anchor:`W [ f_sublb ];
  pack ~anchor:`W [ f ];
  let close = Button.create ~text:"Close" ~font
	      ~command:(fun () -> destroy popup) f_sublb in
  pack ~anchor:`Nw ~fill:`X [close]
;;

(**********************************************************************)

let preprocessor_scan_extensions() =
  (* Find out all packages with a "preprocessor" predicate *)
  let packages = Fl_package_base.list_packages() in
  let plist =
    List.filter
      (fun pkg ->
	 try
	   let _ =
	     Findlib.package_property [ "preprocessor"; "syntax" ] pkg "archive"
	   in true
	 with
	     Not_found -> false
      )
      packages in
  (* Add all selected extensions, if they do not occur yet: *)
  let plist' =
    List.filter
      (fun pkg ->
	 not (List.mem pkg plist)
      )
      !wiz_camlp4_selected in
  List.sort Pervasives.compare (plist @ plist')
;;


let preprocessor_screen frame =
  add_headline frame "Preprocessing";
  add_para frame "Here you can specify whether your source files are \
preprocessed by camlp4. Simply skip this page if you do not want to \
invoke a preprocessor, or if you don't know what this means."; (* "" *)

  let frame'' = indented_cond_frame frame 
		  [ wiz_enable_camlp4, "Enable camlp4" ] in

  let tv = string_tv frame'' wiz_camlp4_syntax in
  let rb_o = Radiobutton.create
	       ~font ~text:"Standard syntax" ~variable:tv ~value:"camlp4o"
	       frame'' in
  let rb_r = Radiobutton.create
	       ~font ~text:"Revised syntax" ~variable:tv ~value:"camlp4r"
	       frame'' in
  pack ~anchor:`W [rb_o; rb_r];
  
  add_para frame'' "Use the following packaged syntax extensions (click the \
right mouse button to find out more about a package):"; (* "" *)

  if !wiz_camlp4_extensions = [] then
    wiz_camlp4_extensions := preprocessor_scan_extensions();

  let click lb row =  (* when the user clicks at a listbox row *)
    wiz_camlp4_selected := listbox_get_selection lb;
  in

  let (f_lb,lb) = scrolled_listbox ~click ~context:pkginfo frame'' in
  Listbox.insert lb ~index:`End ~texts:!wiz_camlp4_extensions;
  listbox_select lb !wiz_camlp4_selected;
  pack ~anchor:`W [f_lb];

  let rescan = Button.create ~text:"Rescan" ~font
	       ~command:(fun () ->
			   wiz_camlp4_extensions := preprocessor_scan_extensions();
			   Listbox.delete lb ~first:(`Num 0) ~last:`End;
			   Listbox.insert lb ~index:`End ~texts:!wiz_camlp4_extensions;
			   listbox_select lb !wiz_camlp4_selected;
			)
		 f_lb in
  let clear = Button.create ~text:"Clear" ~font
		~command:(fun () ->
			    wiz_camlp4_selected := [];
			    listbox_select lb [])
		f_lb in

  pack ~anchor:`Nw ~fill:`X [ rescan; clear ];

  add_para frame'' "Specify here further options to the camlp4 invocation. \
For example, you can load camlp4 modules like pa_ifdef.cmo, and pass the -D \
options to it."; (* "" *)
  
  label_box frame'' [ "Camlp4 options: ", wiz_camlp4_options ];

  footer frame
;;

add_screen preprocessor_screen;;

(**********************************************************************)

let prerequisites_scan_packages() =
  (* Find out all packages *)
  List.sort Pervasives.compare (Fl_package_base.list_packages())
;;


let prerequisites_screen frame =
  add_headline frame "Prerequisites";
  add_para frame "If your modules use packages, you can specify these \
prerequisites here. It is sufficient to select the packages on which \
your modules depend directly. Indirect dependencies can be resolved \
by findlib automatically. Click the right mouse button to find out \
more about a package."; (* "" *)

  if !wiz_all_packages = [] then
    wiz_all_packages := prerequisites_scan_packages();

  let click lb row =  (* when the user clicks at a listbox row *)
    wiz_required_packages := listbox_get_selection lb;
  in

  let (f_lb,lb) = scrolled_listbox ~height:18 ~click ~context:pkginfo frame in
  Listbox.insert lb ~index:`End ~texts:!wiz_all_packages;
  listbox_select lb !wiz_required_packages;
  pack ~anchor:`W [f_lb];

  let rescan = Button.create ~text:"Rescan" ~font
	       ~command:(fun () ->
			   wiz_all_packages := prerequisites_scan_packages();
			   Listbox.delete lb ~first:(`Num 0) ~last:`End;
			   Listbox.insert lb ~index:`End ~texts:!wiz_all_packages;
			   listbox_select lb !wiz_required_packages;
			)
		 f_lb in
  let clear = Button.create ~text:"Clear" ~font
		~command:(fun () ->
			    wiz_required_packages := [];
			    listbox_select lb [])
		f_lb in

  pack ~anchor:`Nw ~fill:`X [ rescan; clear ];

  footer frame
;;

add_screen prerequisites_screen;;

(**********************************************************************)

let buildlib_scan_modules() =
  let files = Array.to_list(Sys.readdir ".") in
  let suffixes = Fl_split.in_words_ws !wiz_source_suffixes in
  let files' = 
    List.filter
      (fun f -> 
	 List.exists (Filename.check_suffix f) suffixes
      )
      files in
  let files'' =
    List.map
      (fun f ->
	 String.capitalize (Filename.chop_extension f)
      )
      files' in
  remove_dups (List.sort Pervasives.compare files'')
;;


let buildlib_screen frame =
  add_headline frame "Build Library";
  add_para frame "The next question is how to build the library, i.e. the \
cma or cmxa archive. It is recommended to create such an archive even if \
the real target of the build process is an executable, because you can load \
it into the toploop at once. However, make sure that you do not put the \
main program of the executable into the archive, as it is usually stripped \
off from the executable, and nothing would happen when you start it.";
(*
  add_para frame "You can select whether you want to create only a bytecode \
archive, a native archive, or both. In the latter case, the simplest way \
is to begin with the bytecode archive, and to copy your specification to \
the native box by pressing the button \"Like bytecode\".";
  add_para frame "If you do not want to create an archive at all, \
skip this page.";
*)
(* "" *)

  let frame' = indented_cond_frame frame
		 [ wiz_byte_enable, "Enable bytecode archive";
		   wiz_nat_enable,  "Enable native archive"; ] in
  add_para frame' "Move the available modules to the right-hand box in the required order.";

  let update =
    double_listbox frame' (ref wiz_available) (ref wiz_objects) buildlib_scan_modules in

  add_para frame "By default, only the suffixes .ml, .mli, .mll, and .mly are used to recognize source code files. You can add here additional suffixes (this requires that you extend the Makefile by your own rules).";

  label_box frame [ "Source code suffixes: ", wiz_source_suffixes ];

  footer frame
;;

add_screen buildlib_screen;;

(**********************************************************************)

let enter_name parent followup =
  let popup = Toplevel.create parent in
  let title = "New Executable" in
  Wm.transient_set popup ~master:(Winfo.toplevel parent);
  Wm.title_set popup title;
  let frame = Frame.create popup in
  pack [frame];
  add_headline frame title;
  add_para frame "Of course, the new executable must have a name.";
  let name = ref "" in
  label_box frame [ "Name: ", name ];
  pack [ Frame.create ~height:10 frame ];
  let buttons = Frame.create frame in
  let ok_b = Button.create ~font ~text:"OK" 
	     ~command:(fun () -> destroy popup; followup !name) buttons in
  let cancel_b = Button.create ~font ~text:"Cancel"
		  ~command:(fun () -> destroy popup) buttons in
  pack ~side:`Left [ ok_b; cancel_b ];
  pack [buttons]
;;


let buildexec_screen frame =
  add_headline frame "Build Executables";
  add_para frame "You can specify the executables to build in the following \
way. Press \"New\" and enter the name of the excutable, then add the modules \
to link in the box that appears. The cma/cmxa archive from the previous \
screen is linked anyway, so its modules cannot be selected again.";
  add_para frame "Leave the list of executables empty if you do not want to \
build any.";
  (* "" *)

  let update_listbox = ref (fun () -> ()) in
  let show = ref (fun _ -> ()) in
  let hide = ref (fun _ -> ()) in

  let click lb row =
    let name = Listbox.get lb ~index:(`Num row) in
    !show name
  in
  
  let newexec() =
    enter_name 
      frame
      (fun name ->
	 (* This function is called after the user pressed "OK" *)
	 if List.mem name !wiz_executables then begin
	   (* It is not allowed to enter the same name twice *)
	   ignore(
	     dialog ~parent:frame ~title:"Already exists"
	     ~message:"This name already exists!" ~buttons:["OK"] ())
	 end
	 else begin
	   wiz_executables := 
	     List.sort Pervasives.compare (name :: !wiz_executables);
	   wiz_exec_objects := (name, ref []) :: !wiz_exec_objects;
	   wiz_exec_native := (name, ref false) :: !wiz_exec_native;
	   !update_listbox();
	   !show name
	 end
      )
  in

  let remexec lb () = 
    let sel = listbox_get_selection lb in
    if sel = [] then begin
      ignore(
	dialog ~parent:frame ~title:"Nothing selected"
	  ~message:"Select the executable to remove first!" ~buttons:["OK"] ())
    end
    else begin
      wiz_executables :=
        List.filter (fun n -> not (List.mem n sel)) !wiz_executables;
      wiz_exec_objects :=
        List.filter (fun (n,_) -> not (List.mem n sel)) !wiz_exec_objects;
      wiz_exec_native :=
        List.filter (fun (n,_) -> not (List.mem n sel)) !wiz_exec_native;
      !update_listbox();
      !hide()
    end
  in

  let (f_lb,lb) = scrolled_listbox ~click ~height:3 frame in
  Listbox.configure ~selectmode:`Single lb;
  Listbox.insert lb ~index:`End ~texts:!wiz_executables;
  pack ~anchor:`W [f_lb];

  let new_b = 
    Button.create ~font ~text:"New" ~command:newexec f_lb in
  let remove_b =
    Button.create ~font ~text:"Remove" ~command:(remexec lb) f_lb in
  
  pack ~anchor:`Nw ~fill:`X [ new_b; remove_b ];

  let f1 = Frame.create frame in
  pack [f1];
  let frame' = Frame.create f1 in
  let frame'_packed = ref false in
  let modbox_available = ref (ref []) in
  let modbox_objects = ref (ref []) in
  let modbox_scan() =
    let mlist = buildlib_scan_modules() in
    List.filter
      (fun m -> not (List.mem m !wiz_objects)) mlist
  in
  add_para frame' "Move the available modules to the right-hand box in the required order.";
  let update_modbox =
    double_listbox frame' modbox_available modbox_objects modbox_scan in

  add_para frame' "Select the type of the executable:";
  let radio = Frame.create frame' in
  let type_ref = ref false in
  let type_ref_ref = ref type_ref in
  let type_tv = bool_tv 
		  ~onchange:(fun () -> !type_ref_ref := !type_ref) 
		  frame' type_ref in
  let byte_r = Radiobutton.create ~font ~text:"Bytecode executable"
		 ~variable:type_tv ~value:"0" radio in
  let nat_r = Radiobutton.create ~font ~text:"Native executable"
		 ~variable:type_tv ~value:"1" radio in
  pack ~side:`Left [byte_r; nat_r ];
  pack ~anchor:`W [radio];
  let update_radio() =
    Textvariable.set type_tv (if !!type_ref_ref then "1" else "0")
  in

  footer frame;

  update_listbox := (fun () ->
		       Listbox.delete lb ~first:(`Num 0) ~last:`End;
		       Listbox.insert lb ~index:`End ~texts:!wiz_executables;
		    );
  show := (fun name ->
	     let k = find_pos name !wiz_executables in
	     Listbox.selection_clear lb ~first:( `Num 0 ) ~last:`End;
             Listbox.selection_set lb ~first:(`Num k) ~last:(`Num k);
	     if not !frame'_packed then (
	       pack [frame'];
	       frame'_packed := true;
	     );
	     modbox_objects := List.assoc name !wiz_exec_objects;
	     type_ref_ref := List.assoc name !wiz_exec_native;
	     update_modbox();
	     update_radio()
	  );
  hide := (fun () ->
	     if !frame'_packed then (
	       Pack.forget [frame'];
	       frame'_packed := false;
	     );
	  )
;;

add_screen buildexec_screen;;

(**********************************************************************)

let generate_screen frame =
  add_headline frame "Generate Makefile";
  add_para frame "Finally, the Makefile can be generated from your inputs. \
You can set the name of the Makefile to a non-standard name. Furthermore, \
you can specify a second file that will be appended to the generated Makefile, \
this is useful to extend the Makefile rules by your own additions.";
  (* "" *)

  label_box frame [ "Name of Makefile: ", wiz_makefile_name;
		    "Local extensions in: ", wiz_local_makefile_name ;
		    "Default target of 'make': ", wiz_make_default;
		  ];

  add_para frame "";

  let show_b =
    Button.create ~font ~text:"Show Makefile"
      ~command:(fun () ->
		  let maketext = makemake() in
		  let popup = Toplevel.create frame in
		  Wm.title_set popup "Generated Makefile";
		  add_headline popup "Generated Makefile";
		  let t = Text.create ~width:80 ~height:25 popup in
		  pack [t];
		  Text.insert ~index:(`End,[]) ~text:maketext t;
	       )
      frame in
  let save_b =
    Button.create ~font ~text:"Save Makefile"
      ~command:(fun () ->
		  let maketext = makemake() in
		  let cont =
		    not (Sys.file_exists !wiz_makefile_name) || (
		      dialog ~parent:frame ~title:"File already exists"
		        ~message:("The file " ^ !wiz_makefile_name ^ 
				  " already exists. Overwrite?")
		        ~buttons:[ "OK"; "Cancel" ] () = 0) in
		  if cont then begin
		    let f = open_out !wiz_makefile_name in
		    output_string f maketext;
		    close_out f
		  end
	       )
      frame in

  pack [show_b; save_b];

  add_headline frame "Quit";

  add_para frame "The following button quits the wizard. You are asked whether you want to save the state first.";

  let quit_b =
    Button.create ~font ~text:"Finish & Quit"
      ~command:(fun () ->
		  if ask_and_save frame then destroy !top)
      frame in



  pack [show_b; quit_b];

  footer frame
;;

add_screen generate_screen;;
		  

(**********************************************************************)

Findlib.init();
top := openTk();
(* current_screen := 5; *)
Wm.title_set !top "findlib/make-wizard";
Wm.protocol_set !top ~name:"WM_DELETE_WINDOW"
  ~command:(fun () -> if ask_and_save !top then destroy !top);
Toplevel.configure ~width:(pixels(`Pt 450.0)) 
                   ~height:(pixels (`Pt 650.0)) (Winfo.toplevel !top);
Pack.propagate_set !top false;
topframe := coe(Frame.create !top);
redraw();
mainLoop();;


(* ======================================================================
 * History:
 * 
 * $Log: make_wizard.ml,v $
 * Revision 1.3  2002/09/22 20:12:35  gerd
 * 	Renamed modules (prefix fl_)
 *
 * Revision 1.2  2002/07/29 19:52:23  gerd
 * 	Fixes for O'Caml 3.05
 *
 * Revision 1.1  2002/05/26 14:09:07  gerd
 * 	Renaming
 *
 * Revision 1.1  2002/05/05 20:40:26  gerd
 * 	Initial revision
 *
 * 
 *)
