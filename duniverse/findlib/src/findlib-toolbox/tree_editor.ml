(* $Id$
 * ----------------------------------------------------------------------
 *
 *)


module Filesys = struct

  let join_path path =
    if path = [ "" ] then 
      "/"
    else
      String.concat "/" path   (* TODO *)


  let split_re = Str.regexp "/"   (* TODO *)

  let split_path s =
    Str.split split_re s


  let list path =
    let path_name = join_path path in
    let d = Unix.opendir path_name in
    let l = ref [] in
    try
      while true do 
	let e = Unix.readdir d in
	if e <> "." && e <> ".." then
          l := e :: !l 
      done; assert false
    with
	End_of_file ->
          Unix.closedir d;
	  List.sort Pervasives.compare !l
      | err ->
	  Unix.closedir d;
	  raise err

  let is_directory path = 
    let path_name = join_path path in
    try
      (Unix.lstat path_name).Unix.st_kind = Unix.S_DIR
    with
	_ -> false

  let is_container path = 
    let path_name = join_path path in
    try
      (Unix.stat path_name).Unix.st_kind = Unix.S_DIR
    with
	_ -> false

end
;;

module Filetree_dlg = struct
  open Lx_tree.Types;;
  open Tk;;
  open Widget;;


  type node =
      { path : string list;
	mutable name : string;
      }

  let some = function Some x -> x | None -> assert false 

  let selected = ref []
  let name_tv = lazy(Textvariable.create())

  let listbox = ref None
  let dir_label = ref None
  let ok_button = ref None
  let cb_enable_ok = ref (fun _ -> true)

  let update_ok_button() =
    let dir = Filesys.join_path !selected in
    let name = Textvariable.get (Lazy.force name_tv) in
    prerr_endline ("Name: " ^ name);
    let en = !selected <> [] && name <> "" && 
	     !cb_enable_ok (Filename.concat dir name) in
    Button.configure 
      ~state:(if en then `Normal else `Disabled) (some !ok_button)

  let update_listbox d =
    (* Fill listbox: *)
    let files = Filesys.list [d] in
    let reg_files = 
      List.filter (fun file -> not(Filesys.is_container [d;file])) files in
    Listbox.delete (some !listbox) ~first:(`Num 0) ~last:`End;
    Listbox.insert (some !listbox) ~index:`End ~texts:reg_files;
    (* Set label: *)
    Label.configure ~text:(d ^ ":") (some !dir_label)

  let node_action tw tree ev evinfo =
    selected := tree.node.path;
    Lx_tree.update tw;
    let dir = Filesys.join_path tree.node.path in
    update_listbox dir;
    update_ok_button()

  let display_node tw tree =
    let (foreground,background) =
      if tree.node.path = !selected then
	(Some `White, Some `Blue)
      else
	(None, None)
    in

    Lx_tree.display_text
       ?foreground ?background
       ~events:[ `ButtonPress ] ~action:node_action tree.node.name

  let rescan_node tw tree =
    let files = Filesys.list tree.node.path in
    let subnodes =
      List.map
	(fun name ->
	   { path = tree.node.path @ [name];
	     name = name;
	   }
	)
	files in
    let dirs = List.filter 
		 (fun n -> Filesys.is_container n.path)
		 subnodes in
    let subtrees =
      List.map
	(fun n ->
	   { node = n;
	     children = [];
	     show = false;
	     scanned = false;
	     interactive = true;
	   }
	)
	dirs in
    tree.children <- subtrees

  let rec make_tree_from_path ?(loc = []) p =
    match p with
	p1 :: pr ->
	  { node = { path = loc @ [p1];
		     name = p1;
		   };
	    children = (if pr = [] then [] else 
			  [ make_tree_from_path ~loc:(loc@[p1]) pr ]);
	    show = true;
	    scanned = true;
	    interactive = true;
	  }
      | [] ->
	  assert false

  let open_dialog ?(title = "Browse...") ?(enable_ok = fun _ -> true) parent =
    selected := [];
    Textvariable.set (Lazy.force name_tv) "";

    let top = Toplevel.create parent in
    Wm.transient_set top ~master:parent;
    Wm.title_set top title;

    cb_enable_ok := enable_ok;

    let l1 = Label.create ~text:"Directories:" top in
    pack ~anchor:`W [ l1 ];

    let dir_tree =
      make_tree_from_path ("" :: Filesys.split_path (Sys.getcwd())) in
    dir_tree.node.name <- "<Root>";

    let tframe = Frame.create top in

    let tw = Lx_tree.create 
	       ~display:display_node ~rescan:rescan_node ~width:300 ~height:300
	       dir_tree
	       tframe in
    let sbar = Scrollbar.create ~orient:`Vertical tframe in
    let canvas = Lx_tree.canvas tw in
    Canvas.configure ~yscrollcommand:(Scrollbar.set sbar) ~relief:`Sunken
      ~borderwidth:3 canvas;
    Scrollbar.configure ~command:(Canvas.yview canvas) sbar;

    pack ~side:`Left ~fill:`Both ~expand:true [canvas];
    pack ~side:`Left ~fill:`Y [ sbar];
    pack ~anchor:`W ~fill:`X [tframe];

    (* Listbox *)
    let lab = Label.create ~text:(" ") top in
    let lbf = Frame.create top in
    let lb = Listbox.create ~height:10 ~font:"fixed" ~exportselection:false 
	       lbf in
    let lbs = Scrollbar.create lbf in
    Listbox.configure ~yscrollcommand:(Scrollbar.set lbs) lb;
    Scrollbar.configure ~command:(Listbox.yview lb) lbs;
    pack ~anchor:`W [lab];
    pack ~anchor:`W ~fill:`X [lbf];
    pack ~side:`Left ~expand:true ~fill:`X [lb];
    pack ~side:`Left ~fill:`Y [lbs];
    listbox := Some lb;
    dir_label := Some lab;

    (* Entry: *)
    let lab' = Label.create ~text:"Filename:" top in
    let ent = Entry.create ~font:"fixed" 
	      ~textvariable:(Lazy.force name_tv) top in
    pack ~anchor:`W [lab'];
    pack ~anchor:`W ~fill:`X [ent];

    (* Link listbox and entry: *)
    Tk.bind ~events:[ `ButtonReleaseDetail 1 ] 
      ~action:(fun _ -> 
		 match Listbox.curselection lb with 
		     [] -> ()
		   | hd :: _ ->
		       let s = Listbox.get lb ~index:hd in
		       Textvariable.set (Lazy.force name_tv) s;
		       update_ok_button()
	      )
      lb;
    Tk.bind ~events:[ `KeyPress ]
      ~action:(fun _ -> 
		 Timer.set ~ms:0 ~callback:update_ok_button)
      ent;

    let result = ref None in

    let buttons = Frame.create top in
    let ok_b = Button.create ~text:"OK"
		 ~state:`Disabled
		 ~command:(fun _ -> 
			     let name = Textvariable.get (Lazy.force name_tv) in
			     if name <> "" then begin
			       result := Some (Filesys.join_path !selected,
					       name);
			       Tk.destroy top
			     end else Bell.ring() )
		 buttons in
    let cancel_b = Button.create ~text:"Cancel" 
		     ~command:(fun _ -> Tk.destroy top) 
		     buttons in

    pack ~expand:true ~fill:`X [buttons];
    pack ~side:`Left [ ok_b; cancel_b ];
    ok_button := Some ok_b;

    Grab.set top;
    Tkwait.window top;

    !result
      (* returns None if canceled, or Some (dir, name) *)

end
;;


module S = struct
  type t = string
  let compare = compare
end
;;


module StringSet = Set.Make(S)
;;


open Lx_tree.Types;;
open Tk;;
open Widget;;


let some = Filetree_dlg.some;;

type filetype =
    Regular
  | Directory
  | Absent
;;


type node = 
    { path : string list;
      name : string;                (* last component of [path] *)
      mutable is_dir : bool;        (* from file list *)
      mutable selected : bool;      (* from file list *)
      mutable realtype : filetype;  (* from file system *)
    }
;;

type file_tree = node tree ;;

type state =
    { mutable current_directory : string;
      mutable list_file : string;          (* relative to current_directory *)
      mutable tree_widget : node tree_widget option;
      mutable info_widget : label widget option;
      mutable dir_context_menu : menu widget option;
      mutable dir_context_suffix_menu : menu widget option;
      mutable dir_context_unsuffix_menu : menu widget option;
      mutable file_context_menu : menu widget option;
      mutable context_menu_tree : node tree;
      mutable modified : bool;
      topwdg : toplevel widget;
      workframe : frame widget;
      directory_xpm : image;
      selected_xpm : image;
      unselected_xpm : image;
      mutable display : node tree_widget -> file_tree -> node display_item;
    }
;;


let dummy_tree =
  { node = { path = [];
	     name = "";
	     is_dir = false;
	     selected = false;
	     realtype = Regular;
	   };
    children = [];
    show = false;
    scanned = false;
    interactive = false;
  }
;;


let rescan_node tw tree =
  let files = Filesys.list tree.node.path in
  let missing_files =
    List.map
      (fun child -> child.node.name)
      (List.filter
	 (fun child ->
	    child.node.selected && not(List.mem child.node.name files)
	 )
	 tree.children
      )
  in
  let all_files = List.sort compare (files @ missing_files) in
  let old_children = tree.children in
  let new_children =
    List.map
      (fun filename ->
	 try
	   (* maybe the new child is the old child: *)
	   List.find 
	     (fun old_child -> old_child.node.name = filename) 
	     old_children
	 with
	     Not_found ->
	       let new_path = tree.node.path @ [filename] in
	       let is_dir = Filesys.is_directory new_path in
	       let new_tree =
		 { node = { path = new_path;
			    name = filename;
			    is_dir = is_dir;
			    selected = false;
			    realtype = Regular;  (* Will be updated below *)
			  };
		   children = [];
		   scanned = not is_dir;
		   show = false;
		   interactive = true;
		 } in
	       new_tree
      )
      all_files
  in
  (* Update the [realtype] flag: *)
  List.iter
    (fun child ->
       child.node.realtype <- 
         if List.mem child.node.name files then 
	   (if Filesys.is_directory child.node.path then Directory else Regular)
	 else
	   Absent
    )
    new_children;
  (* Store the result: *)
  tree.children <- new_children
;;


let rec selected_files_exist tree =
  tree.node.selected || selected_files_exist_in_children tree

and selected_files_exist_in_children tree =
  List.exists selected_files_exist tree.children
;;


let rec expand ?(all = false) tw tree =
  let show =
    tree.node.is_dir && (all || selected_files_exist_in_children tree) in
  tree.show <- tree.show || show;
  if show then rescan_node tw tree;
  List.iter (fun child -> expand ~all tw child) tree.children
;;


let rec collapse tw tree =
  tree.show <- false;
  List.iter (fun child -> collapse tw child) tree.children
;;


let suffix_re = Str.regexp "\\.[^.]+$";;

let rec find_suffixes ?(recursive = false) tw tree =
  assert(tree.node.is_dir);
  rescan_node tw tree;  (* questionable *)
  tree.show <- true;
  let files = 
    List.map
      (fun ch -> ch.node.name)
      tree.children in
  let suff =
    List.filter
      (fun s ->
	 s <> ""
      )
      (List.map
	 (fun name ->
	    try
	      let k = Str.search_forward suffix_re name 0 in
	      String.sub name k (String.length name - k)
	    with
		Not_found -> ""
	 )
	 files
      )
  in
  let suff_set = ref (StringSet.empty) in
  List.iter
    (fun s ->
       suff_set := StringSet.add s !suff_set)
    suff;
  if recursive then begin
    List.iter
      (fun child ->
	 if child.node.is_dir then
	   suff_set := StringSet.union !suff_set (find_suffixes ~recursive tw child)
      )
      tree.children
  end;
  !suff_set
;;


let rec select_suffixes ?(recursive = false) ?(unselect=false) suffix tree =
  prerr_endline "select_suffixes";
  List.iter
    (fun child ->
       let child_suff = 
	 try
	   let name = child.node.name in
	   let k = Str.search_forward suffix_re name 0 in
	   String.sub name k (String.length name - k)
	 with
	     Not_found -> ""
       in
       if child_suff = suffix && not (child.node.is_dir) then
	 child.node.selected <- not unselect;
       if recursive && child.node.is_dir then
	 select_suffixes ~recursive ~unselect suffix child
    )
    tree.children
;;


let rec select_all ?(recursive = false) ?(unselect=false) tree =
  prerr_endline "select_all";
  List.iter
    (fun child ->
       if not (child.node.is_dir) then
	 child.node.selected <- not unselect;
       if recursive && child.node.is_dir then
	 select_all ~recursive ~unselect child
    )
    tree.children
;;


let set_suffix_menu ?unselect tw menu tree =
  Menu.delete menu ~first:(`Num 0) ~last:`End;
  let suffixes = find_suffixes tw tree in
  StringSet.iter
    (fun s ->
       Menu.add_command 
         ~label:s 
         ~command:(fun () -> 
		     select_suffixes ?unselect s tree; 
		     Lx_tree.update tw) 
	 menu
    )
    suffixes
;;


let node_action st tw tree ev evinfo =
  (* prerr_endline "node_action"; *)
  match (ev : Lx_spots.supported_event) with
      `ButtonPress -> 
	begin match evinfo.ev_ButtonNumber with
	    1 ->
	      (* prerr_endline ("name=" ^ node.node.name); *)
	      if not(tree.node.is_dir) then begin
		(* prerr_endline "invert!"; *)
		tree.node.selected <- not tree.node.selected;
		st.modified <- true;
		prerr_endline ("update");
		Lx_tree.update tw;
		prerr_endline ("/update");
	      end
	  | 3 ->
	      (* Context menu *)
	      let menu =
		if tree.node.is_dir 
		then (
		  set_suffix_menu 
			tw (some st.dir_context_suffix_menu) tree;
		  set_suffix_menu 
		        ~unselect:true
			tw (some st.dir_context_unsuffix_menu) tree;
		  st.dir_context_menu 
		)
		else st.file_context_menu in
	      st.context_menu_tree <- tree;
	      ( match menu with
		    Some m ->
		      Menu.popup ~x:(evinfo.ev_RootX) ~y:(evinfo.ev_RootY) m;
		  | None ->
		      ()
	      )
	  | _ ->
	      ()
	end

    | `Enter ->
	begin match st.info_widget with
	    Some label ->
	      let text =
		match tree.node.realtype, tree.node.is_dir with
		    Regular,false -> 
		      tree.node.name ^ ": File is " ^
		      (if tree.node.selected then "selected" else "not selected")
		  | Regular,true ->
		      tree.node.name ^ ": Listed as file, but now a directory"
		  | Directory,true ->
		      tree.node.name ^ ": Directory"
		  | Directory,false ->
		      tree.node.name ^ ": Listed as directory, but now a file"
		  | Absent, _ ->
		      tree.node.name ^ ": does not exist"
	      in
	      Label.configure ~text label
	  | None -> ()
	end
	
    | `Leave ->
	begin match st.info_widget with
	    Some label ->
	      Label.configure ~text:" " label
	  | None -> ()
	end

    | _ ->
	()
;;


let display_node st =
  let node_action_st = node_action st in
  fun tw tree ->
    let node = tree.node in
    let image =
      if node.is_dir then
	st.directory_xpm
      else
	if node.selected then
	  st.selected_xpm
	else
	  st.unselected_xpm
    in
    
    let foreground =
      (* Blue means: A selected regular file
       * Black means: A non selected regular file
       * Red means a problem:
       *  - The file does not exist
       *  - The file has the wrong type
       *)
      match (node.realtype, node.is_dir) with
	  (Regular, false) ->
	    (if node.selected then Some `Blue else None)
	| (Directory, true) ->
	    None    (* directories can never be selected *)
	| _ ->
	    Some(`Red) 
    in
    
    let events = [ `ButtonPress; `Enter; `Leave ] in

    Lx_tree.display_text
      ~image ~events ~action:node_action_st ?foreground node.name
;;


let neutral_frame wdg =
  Frame.create ~highlightthickness:0 ~borderwidth:0 wdg
;;


let save st =
  let rec write_file file path tree =
    let full_name =
      if path = "" then 
	tree.node.name
      else
	Filename.concat path tree.node.name
    in
    if not tree.node.is_dir && tree.node.selected then begin
      output_string file full_name;
      output_string file "\n";
    end;
    List.iter (fun child -> write_file file full_name child) tree.children
  in

  match st.tree_widget with
      Some tw ->
	let tree = Lx_tree.tree tw in
	let filename = Filename.concat st.current_directory st.list_file in
	let file = open_out filename in
	List.iter (fun child -> write_file file "" child) tree.children;
	close_out file;
	st.modified <- false;
    | None ->
	()
;;


let q_save st =
  (* Question: save current file list? Returns [true] if the operation 
   * can be continued
   *)
  if st.modified then begin
    let choice =
      Dialog.create ~parent:st.topwdg ~title:"Save?" ~message:"The current tree is modified. Save it to disk?" 
      ~buttons:[ "Yes"; "No"; "Cancel" ] () in
    match choice with
	0 -> save st; true
      | 1 -> true
      | 2 -> false
  end
  else
    true
;;


let drop st =
  st.list_file <- "";
  st.modified <- false;
  Frame.configure ~width:400 ~height:600 st.workframe;
  List.iter destroy (Winfo.children st.workframe);
  st.tree_widget <- None
;;


let create_work_tree st top_node =
  let tree_frame = neutral_frame st.workframe in
  let info_frame = neutral_frame st.workframe in

  let sbar = Scrollbar.create ~orient:`Vertical tree_frame in
  let info_label = 
    Label.create ~highlightthickness:0 ~borderwidth:0 ~justify:`Left
      ~text:" " info_frame in
  pack ~anchor:`W ~ipady:4 ~ipadx:4 ~side:`Left [info_label];

  let width = Winfo.reqwidth st.workframe - Winfo.reqwidth sbar in
  let height = Winfo.reqheight st.workframe - Winfo.reqwidth info_label - 14 in
  (* TODO: why 14? I think that 8 pixels = 2*ipady is the right value. *)
  let tw = Lx_tree.create 
	     ~display:st.display ~rescan:rescan_node ~width ~height
	     ~background:`White
	     ~font:"9x15"
	     top_node
	     tree_frame in
  let canvas = Lx_tree.canvas tw in
  Canvas.configure ~yscrollcommand:(Scrollbar.set sbar) canvas;
  Scrollbar.configure ~command:(Canvas.yview canvas) sbar;

  Canvas.configure ~highlightthickness:0 canvas;

  pack ~side:`Left ~fill:`Both ~expand:true [canvas];
  pack ~side:`Left ~fill:`Y [ sbar];

  pack ~anchor:`W [tree_frame; info_frame ];

  st.tree_widget <- Some tw;
  st.info_widget <- Some info_label;

  let dcm = Menu.create ~tearoff:false st.workframe in
  let dcm_suffix = Menu.create ~tearoff:false dcm in
  let dcm_unsuffix = Menu.create ~tearoff:false dcm in
  Menu.add_cascade ~label:"Select by suffix" ~menu:dcm_suffix dcm;
  Menu.add_cascade ~label:"Unselect by suffix" ~menu:dcm_unsuffix dcm;
(*
  Menu.add_command ~label:".ml" dcm_suffix;
  Menu.add_command ~label:".mli" dcm_suffix;
  Menu.add_command ~label:".mly" dcm_suffix;
*)

  Menu.add_command ~label:"Select all files" 
                   ~command:(fun () ->
			       select_all st.context_menu_tree;
			       Lx_tree.update tw)
                   dcm;
  Menu.add_command ~label:"Unselect all files" 
                   ~command:(fun () ->
			       select_all ~unselect:true st.context_menu_tree;
			       Lx_tree.update tw)
                   dcm;
  Menu.add_separator dcm;
  Menu.add_command ~label:"Rescan directory" 
                   ~command:(fun () ->
			       rescan_node tw st.context_menu_tree;
			       Lx_tree.update tw
			    ) dcm;
  Menu.add_separator dcm;
  Menu.add_command ~label:"Expand all" 
                   ~command:(fun () -> 
			       expand ~all:true tw st.context_menu_tree;
			       Lx_tree.update tw
			    ) dcm;
  Menu.add_command ~label:"Expand as needed" 
                   ~command:(fun () -> 
			       expand tw st.context_menu_tree;
			       Lx_tree.update tw
			    ) dcm;
  Menu.add_command ~label:"Collapse" 
                   ~command:(fun () -> 
			       collapse tw st.context_menu_tree;
			       Lx_tree.update tw
			    ) dcm;
  st.dir_context_menu <- Some dcm;
  st.dir_context_suffix_menu <- Some dcm_suffix;
  st.dir_context_unsuffix_menu <- Some dcm_unsuffix;
;;


let file_new st () =
  let enable_ok filename =
    not(Sys.file_exists filename)
  in

  if q_save st then begin
    match Filetree_dlg.open_dialog ~title:"New tree" ~enable_ok st.topwdg with
	Some (dir, name) ->
	  drop st;
	  st.current_directory <- dir;
	  st.list_file <- name;
	  let basename = Filename.basename dir in
	  let initial_node =
	    { node = { name = basename;
		       path = [ dir ];
		       is_dir = true;
		       realtype = Directory;
		       selected = false;
		     };
	      children = [];
	      show = false;
	      scanned = false;
	      interactive = true;
	    } in
	  let filename = Filename.concat dir name in
	  Unix.close(Unix.openfile filename [Unix.O_RDWR; Unix.O_CREAT] 0o666);
	  create_work_tree st initial_node
      | None ->
	  ()
  end
;;


let strip_ws_re = Str.regexp "^[ \r\n\t]*\\(.*\\)[ \r\n\t]*$";;

let strip_ws s =
  if Str.string_match strip_ws_re s 0 then
    Str.matched_group 1 s
  else
    assert false
;;


let file_open st () =
  let rec enter_path path node =
    match path with
	n :: path' ->
	  begin try
	    let child = List.find (fun ch -> ch.node.name = n) node.children in
	                (* or Not_found *)
	    if path' <> [] then begin
	      child.node.is_dir <- true;
	    end;
	    enter_path path' child
	  with
	      Not_found ->
		let child =
		  { node = { name = n;
			     path = node.node.path @ [n];
			     is_dir = (path' <> []);
			     realtype = Regular;  (* Will be updated later *)
			     selected = true;
			   };
		    children = [];
		    show = false;
		    scanned = path' = [];
		    interactive = true;
		  } in
		node.children <- child :: node.children;
		enter_path path' child
	  end
      | [] ->
	  ()
  in

  let rec read_file file initial_node =
    let line = strip_ws (input_line file) in
    if line = "" then read_file file initial_node else begin
      let path = Filesys.split_path line in
      enter_path path initial_node;
      read_file file initial_node
    end
  in

  let enable_ok filename =
    Sys.file_exists filename
  in

  if q_save st then begin
    match Filetree_dlg.open_dialog ~title:"Open tree" ~enable_ok st.topwdg with
	Some (dir, name) ->
	  drop st;
	  st.current_directory <- dir;
	  st.list_file <- name;
	  let basename = Filename.basename dir in
	  let initial_node =
	    { node = { name = basename;
		       path = [ dir ];
		       is_dir = true;
		       realtype = Directory;
		       selected = false;
		     };
	      children = [];
	      show = false;
	      scanned = false;
	      interactive = true;
	    } in
	  let filename = Filename.concat dir name in
	  let file = open_in filename in
	  (try read_file file initial_node with End_of_file -> ());
	  close_in file;
	  create_work_tree st initial_node
      | None ->
	  ()
  end
;;


let file_save st () =
  save st;
  ignore(Dialog.create ~parent:st.topwdg ~title:"Saved" ~message:"Tree saved." 
	 ~buttons:[ "OK" ] ());
;;


let file_quit st () =
  if q_save st then destroy st.topwdg
;;


let view_expand ?all st () =
  match st.tree_widget with
      Some tw ->
	expand ?all tw (Lx_tree.tree tw);
	Lx_tree.update tw
    | None ->
	()
;;


let view_collapse st () =
  match st.tree_widget with
      Some tw ->
	collapse tw (Lx_tree.tree tw);
	Lx_tree.update tw
    | None ->
	()
;;


let init_application top =
  let directory_xpm =
    Imagebitmap.create ~file:"directory.xpm" ~background:`White () in
  let selected_xpm =
    Imagebitmap.create ~file:"selected.xpm" ~background:`White () in
  let unselected_xpm =
    Imagebitmap.create ~file:"unselected.xpm" ~background:`White () in

  (* Create the work frame, 400 x 600 pixels *)

  let workframe = Frame.create ~width:400 ~height:600 top in

  (* Create the state record: *)

  let st =
    { current_directory = Sys.getcwd();
      list_file = "";
      tree_widget = None;
      info_widget = None;
      dir_context_menu = None;
      dir_context_suffix_menu = None;
      dir_context_unsuffix_menu = None;
      file_context_menu = None;
      context_menu_tree = dummy_tree;
      modified = false;
      workframe = workframe;
      topwdg = top;
      directory_xpm = directory_xpm;
      selected_xpm = selected_xpm;
      unselected_xpm = unselected_xpm;
      display = (fun _ _ -> Lx_tree.display_text "");
    } in
  st.display <- display_node st;
  
  (* Because there are currently no toplevel menubars in labltk (they appeared
   * first in Tk 8.0), we simulate them using a frame.
   *)

  let menuframe =
    Frame.create ~relief:`Groove ~borderwidth:2 top in
  
  let file_mb =
    Menubutton.create ~text:"File" menuframe in

  let file_m =
    Menu.create file_mb in
  Menu.add_command ~label:"New Tree"     ~command:(file_new  st) file_m;
  Menu.add_command ~label:"Open"         ~command:(file_open st) file_m;
  Menu.add_command ~label:"Check" file_m;
  Menu.add_command ~label:"Save"         ~command:(file_save st) file_m;
  Menu.add_command ~label:"Quit"         ~command:(file_quit st) file_m;
  Menubutton.configure ~menu:file_m file_mb;
  Wm.protocol_set top ~name:"WM_DELETE_WINDOW" ~command:(file_quit st);

(*
  let edit_mb =
    Menubutton.create ~text:"Edit" menuframe in

  let edit_m =
    Menu.create edit_mb in
  (* Undo? *)
  Menu.add_command ~label:"Select by suffix" edit_m;
  Menu.add_command ~label:"Select by regexp" edit_m;
  Menu.add_command ~label:"Select all" edit_m;
  Menubutton.configure ~menu:edit_m edit_mb;
*)

  let view_mb =
    Menubutton.create ~text:"View" menuframe in

  let view_m =
    Menu.create view_mb in
  Menu.add_command ~label:"Expand all" ~command:(view_expand ~all:true st) view_m;
  Menu.add_command ~label:"Expand as needed" ~command:(view_expand st) view_m;
  Menu.add_command ~label:"Collapse all"     ~command:(view_collapse st) view_m;
  Menubutton.configure ~menu:view_m view_mb;

  pack ~side:`Left [file_mb; (* edit_mb; *) view_mb];

  pack ~anchor:`W ~fill:`X [menuframe];
  pack ~anchor:`W [workframe]
;;


let top_window = openTk() in
init_application top_window;
Sys.catch_break true;
try
  mainLoop()
with
    Sys.Break -> 
      prerr_endline "EXIT";
      ()
;;

(* ======================================================================
 * History:
 * 
 * $Log: tree_editor.ml,v $
 * Revision 1.1  2002/06/08 19:39:53  gerd
 * 	Initial revision.
 *
 * 
 *)
