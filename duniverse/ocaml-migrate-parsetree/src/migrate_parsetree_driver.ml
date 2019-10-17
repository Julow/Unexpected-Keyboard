open Migrate_parsetree_versions
module Ast_io = Migrate_parsetree_ast_io

(** {1 State a rewriter can access} *)

type extra = ..

type config = {
  tool_name: string;
  include_dirs : string list;
  load_path : string list;
  debug : bool;
  for_package : string option;
  extras : extra list;
}

let make_config ~tool_name ?(include_dirs=[]) ?(load_path=[]) ?(debug=false)
      ?for_package ?(extras=[]) () =
  { tool_name
  ; include_dirs
  ; load_path
  ; debug
  ; for_package
  ; extras
  }

type cookie = Cookie : 'types ocaml_version * 'types get_expression -> cookie

type cookies = (string, cookie) Hashtbl.t

let create_cookies () = Hashtbl.create 3

let global_cookie_table = create_cookies ()

let get_cookie table name version =
  match
    match Hashtbl.find table name with
    | result -> Some result
    | exception Not_found ->
        match Ast_mapper.get_cookie name with
        | Some expr -> Some (Cookie ((module OCaml_current), expr))
        | None ->
            match Hashtbl.find global_cookie_table name with
            | result -> Some result
            | exception Not_found -> None
  with
  | None -> None
  | Some (Cookie (version', expr)) ->
    Some ((migrate version' version).copy_expression expr)

let set_cookie table name version expr =
  Hashtbl.replace table name (Cookie (version, expr))

let set_global_cookie name version expr =
  set_cookie global_cookie_table name version expr

let apply_cookies table =
  Hashtbl.iter (fun name (Cookie (version, expr)) ->
      Ast_mapper.set_cookie name
        ((migrate version (module OCaml_current)).copy_expression expr)
    ) table

let initial_state () =
  {
    tool_name = Ast_mapper.tool_name ();
    include_dirs = !Clflags.include_dirs;
    load_path = Migrate_parsetree_compiler_functions.get_load_paths ();
    debug = !Clflags.debug;
    for_package = !Clflags.for_package;
    extras = [];
  }

(** {1 Registering rewriters} *)

type 'types rewriter = config -> cookies -> 'types get_mapper

type rewriter_group =
    Rewriters : 'types ocaml_version * (string * 'types rewriter) list -> rewriter_group

let rewriter_group_names (Rewriters (_, l)) = List.map fst l

let uniq_rewriter = Hashtbl.create 7
module Pos_map = Map.Make(struct
    type t = int
    let compare : int -> int -> t = compare
  end)
let registered_rewriters = ref Pos_map.empty

let all_rewriters () =
  Pos_map.bindings !registered_rewriters
  |> List.map (fun (_, r) -> !r)
  |> List.concat

let uniq_arg = Hashtbl.create 7
let registered_args_reset = ref []
let registered_args = ref []

let () =
  let set_cookie s =
    match String.index s '=' with
    | exception _ ->
      raise (Arg.Bad "invalid cookie, must be of the form \"<name>=<expr>\"")
    | i ->
      let name = String.sub s 0 i in
      let value = String.sub s (i + 1) (String.length s - i - 1) in
      let input_name = "<command-line>" in
      Location.input_name := input_name;
      let lexbuf = Lexing.from_string value in
      lexbuf.Lexing.lex_curr_p <-
        { Lexing.
          pos_fname = input_name
        ; pos_lnum  = 1
        ; pos_bol   = 0
        ; pos_cnum  = 0
        };
      let expr = Parse.expression lexbuf in
      set_global_cookie name (module OCaml_current) expr
  in
  registered_args :=
    ("--cookie", Arg.String set_cookie,
     "NAME=EXPR Set the cookie NAME to EXPR") :: !registered_args

type ('types, 'version, 'rewriter) is_rewriter =
  | Is_rewriter : ('types, 'types ocaml_version, 'types rewriter) is_rewriter

let add_rewriter
    (type types) (type version) (type rewriter)
    (Is_rewriter : (types, version, rewriter) is_rewriter)
    (version : version) name (rewriter : rewriter) =
  let rec add_rewriter = function
  | [] -> [Rewriters (version, [name, rewriter])]
  | (Rewriters (version', rewriters) as x) :: xs ->
      match compare_ocaml_version version version' with
      | Eq -> Rewriters (version', (name, rewriter) :: rewriters) :: xs
      | Lt -> Rewriters (version, [name, rewriter]) :: x :: xs
      | Gt -> x :: add_rewriter xs
  in
  add_rewriter

let register ~name ?reset_args ?(args=[]) ?(position=0) version rewriter =
  (* Validate name *)
  if name = "" then
    invalid_arg "Migrate_parsetree_driver.register: name is empty";
  if Hashtbl.mem uniq_rewriter name then
    invalid_arg ("Migrate_parsetree_driver.register: rewriter " ^ name ^ " has already been registered")
  else Hashtbl.add uniq_rewriter name ();
  (* Validate arguments *)
  List.iter (fun (arg_name, _, _) ->
      match Hashtbl.find uniq_arg arg_name with
      | other_rewriter ->
          invalid_arg (Printf.sprintf
                         "Migrate_parsetree_driver.register: argument %s is used by %s and %s" arg_name name other_rewriter)
      | exception Not_found ->
          Hashtbl.add uniq_arg arg_name name
    ) args;
  (* Register *)
  begin match reset_args with
  | None -> ()
  | Some f -> registered_args_reset := f :: !registered_args_reset
  end;
  registered_args := List.rev_append args !registered_args;
  let r =
    try
      Pos_map.find position !registered_rewriters
    with Not_found ->
      let r = ref [] in
      registered_rewriters := Pos_map.add position r !registered_rewriters;
      r
  in
  r := add_rewriter Is_rewriter version name rewriter !r

let registered_args () = List.rev !registered_args
let reset_args () = List.iter (fun f -> f ()) !registered_args_reset

(** {1 Accessing or running registered rewriters} *)

type ('types, 'version, 'tree) is_signature =
    Signature : ('types, 'types ocaml_version, 'types get_signature) is_signature

type ('types, 'version, 'tree) is_structure =
    Structure : ('types, 'types ocaml_version, 'types get_structure) is_structure

type some_structure =
  | Str : (module Migrate_parsetree_versions.OCaml_version with
            type Ast.Parsetree.structure = 'concrete) * 'concrete -> some_structure

type some_signature =
  | Sig : (module Migrate_parsetree_versions.OCaml_version with
            type Ast.Parsetree.signature = 'concrete) * 'concrete -> some_signature

let migrate_some_structure dst (Str ((module Version), st)) =
  (migrate (module Version) dst).copy_structure st

let migrate_some_signature dst (Sig ((module Version), sg)) =
  (migrate (module Version) dst).copy_signature sg

let rec rewrite_signature
  : type types version tree.
    config -> cookies ->
    (types, version, tree) is_signature -> version -> tree ->
    rewriter_group list -> some_signature
  = fun (type types) (type version) (type tree)
    config cookies
    (Signature : (types, version, tree) is_signature)
    (version : version)
    (tree : tree)
    -> function
      | [] ->
        let (module Version) = version in
        Sig ((module Version), tree)
      | Rewriters (version', rewriters) :: rest ->
          let rewrite (_name, rewriter) tree =
            let (module Version) = version' in
            Version.Ast.map_signature (rewriter config cookies) tree
          in
          let tree = (migrate version version').copy_signature tree in
          let tree = List.fold_right rewrite rewriters tree in
          rewrite_signature config cookies Signature version' tree rest

let rewrite_signature config version sg =
  let cookies = create_cookies () in
  let sg =
    rewrite_signature config cookies Signature version sg
      (all_rewriters ())
  in
  apply_cookies cookies;
  sg

let rec rewrite_structure
  : type types version tree.
    config -> cookies ->
    (types, version, tree) is_structure -> version -> tree ->
    rewriter_group list -> some_structure
  = fun (type types) (type version) (type tree)
    config cookies
    (Structure : (types, version, tree) is_structure)
    (version : version)
    (tree : tree)
    -> function
      | [] ->
        let (module Version) = version in
        Str ((module Version), tree)
      | Rewriters (version', rewriters) :: rest ->
          let rewriter (_name, rewriter) tree =
            let (module Version) = version' in
            Version.Ast.map_structure (rewriter config cookies) tree
          in
          let tree = (migrate version version').copy_structure tree in
          let tree = List.fold_right rewriter rewriters tree in
          rewrite_structure config cookies Structure version' tree rest

let rewrite_structure config version st =
  let cookies = create_cookies () in
  let st =
    rewrite_structure config cookies Structure version st
      (all_rewriters ())
  in
  apply_cookies cookies;
  st

let run_as_ast_mapper args =
  let spec = registered_args () in
  let args, usage =
    let me = Filename.basename Sys.executable_name in
    let args = match args with "--as-ppx" :: args -> args | args -> args in
    (Array.of_list (me :: args),
     Printf.sprintf "%s [options] <input ast file> <output ast file>" me)
  in
  reset_args ();
  match
    Arg.parse_argv args spec
      (fun arg -> raise (Arg.Bad (Printf.sprintf "invalid argument %S" arg)))
      usage
  with
  | exception (Arg.Help msg) ->
      prerr_endline msg;
      exit 1
  | () ->
      OCaml_current.Ast.make_top_mapper
        ~signature:(fun sg ->
            let config = initial_state () in
            rewrite_signature config (module OCaml_current) sg
            |> migrate_some_signature (module OCaml_current)
          )
        ~structure:(fun str ->
            let config = initial_state () in
            rewrite_structure config (module OCaml_current) str
            |> migrate_some_structure (module OCaml_current)
          )

let protectx x ~finally ~f =
  match f x with
  | y -> finally x; y
  | exception e -> finally x; raise e

let with_file_in fn ~f =
  protectx (open_in_bin fn) ~finally:close_in ~f

let with_file_out fn ~f =
  protectx (open_out_bin fn) ~finally:close_out ~f

type ('a, 'b) intf_or_impl =
  | Intf of 'a
  | Impl of 'b

let guess_file_kind fn =
  if Filename.check_suffix fn ".ml" then
    Impl fn
  else if Filename.check_suffix fn ".mli" then
    Intf fn
  else
    Location.raise_errorf ~loc:(Location.in_file fn)
      "I can't decide whether %s is an implementation or interface file"
      fn

let check_kind fn ~expected ~got =
  let describe = function
    | Intf _ -> "interface"
    | Impl _ -> "implementation"
  in
  match expected, got with
  | Impl _, Impl _
  | Intf _, Intf _ -> ()
  | _ ->
    Location.raise_errorf ~loc:(Location.in_file fn)
      "Expected an %s got an %s instead"
      (describe expected)
      (describe got)

let load_file file =
  let fn =
    match file with
    | Intf fn -> fn
    | Impl fn -> fn
  in
  with_file_in fn ~f:(fun ic ->
    match Ast_io.from_channel ic with
    | Ok (fn, Ast_io.Intf ((module V), sg)) ->
      check_kind fn ~expected:file ~got:(Intf ());
      Location.input_name := fn;
      (* We need to convert to the current version in order to interpret the cookies using
         [Ast_mapper.drop_ppx_context_*] from the compiler *)
      (fn, Intf ((migrate (module V) (module OCaml_current)).copy_signature sg))
    | Ok (fn, Ast_io.Impl ((module V), st)) ->
      check_kind fn ~expected:file ~got:(Impl ());
      Location.input_name := fn;
      (fn, Impl ((migrate (module V) (module OCaml_current)).copy_structure st))
    | Error (Ast_io.Unknown_version _) ->
      Location.raise_errorf ~loc:(Location.in_file fn)
        "File is a binary ast for an unknown version of OCaml"
    | Error (Ast_io.Not_a_binary_ast prefix_read_from_file) ->
      (* To test if a file is a binary AST file, we have to read the first few bytes of
         the file.

         If it is not a binary AST, we have to parse these bytes and the rest of the file
         as source code. To do that, we prefill the lexbuf buffer with what we read from
         the file to do the test. *)
      let lexbuf = Lexing.from_channel ic in
      let len = String.length prefix_read_from_file in
      String.blit prefix_read_from_file 0 lexbuf.Lexing.lex_buffer 0 len;
      lexbuf.Lexing.lex_buffer_len <- len;
      lexbuf.Lexing.lex_curr_p <-
        { Lexing.
          pos_fname = fn
        ; pos_lnum  = 1
        ; pos_bol   = 0
        ; pos_cnum  = 0
        };
      Location.input_name := fn;
      match file with
      | Impl fn ->
        (fn, Impl (Parse.implementation lexbuf))
      | Intf fn ->
        (fn, Intf (Parse.interface lexbuf)))

let with_output ?bin output ~f =
  match output with
  | None ->
      begin match bin with
      | Some bin -> set_binary_mode_out stdout bin
      | None -> ()
      end;
      f stdout
  | Some fn -> with_file_out fn ~f

type output_mode =
  | Pretty_print
  | Dump_ast
  | Null

let process_file ~config ~output ~output_mode ~embed_errors file =
  let fn, ast = load_file file in
  let ast =
    match ast with
    | Intf sg ->
      let sg = Ast_mapper.drop_ppx_context_sig ~restore:true sg in
      let sg =
        try
          rewrite_signature config (module OCaml_current) sg
          |> migrate_some_signature (module OCaml_current)
        with exn when embed_errors ->
        match Migrate_parsetree_compiler_functions.error_of_exn exn with
        | None -> raise exn
        | Some error ->
          [ Ast_helper.Sig.extension ~loc:Location.none
              (Ast_mapper.extension_of_error error) ]
      in
      Intf (sg, Ast_mapper.add_ppx_context_sig ~tool_name:config.tool_name sg)
    | Impl st ->
      let st = Ast_mapper.drop_ppx_context_str ~restore:true st in
      let st =
        try
          rewrite_structure config (module OCaml_current) st
          |> migrate_some_structure (module OCaml_current)
        with exn when embed_errors ->
        match Migrate_parsetree_compiler_functions.error_of_exn exn with
        | None -> raise exn
        | Some error ->
          [ Ast_helper.Str.extension ~loc:Location.none
              (Ast_mapper.extension_of_error error) ]
      in
      Impl (st, Ast_mapper.add_ppx_context_str ~tool_name:config.tool_name st)
  in
  match output_mode with
  | Dump_ast ->
    with_output ~bin:true output ~f:(fun oc ->
      let ast =
        match ast with
        | Intf (_, sg) -> Ast_io.Intf ((module OCaml_current), sg)
        | Impl (_, st) -> Ast_io.Impl ((module OCaml_current), st)
      in
      Ast_io.to_channel oc fn ast)
  | Pretty_print ->
    with_output output ~f:(fun oc ->
      let ppf = Format.formatter_of_out_channel oc in
      (match ast with
       | Intf (sg, _) -> Pprintast.signature ppf sg
       | Impl (st, _) -> Pprintast.structure ppf st);
      Format.pp_print_newline ppf ())
  | Null ->
    ()

let print_transformations () =
  let print_group name = function
    | [] -> ()
    | names ->
        Printf.printf "%s:\n" name;
        List.iter (Printf.printf "%s\n") names
  in
  all_rewriters ()
  |> List.map rewriter_group_names
  |> List.concat
  |> print_group "Registered Transformations";
  Ppx_derivers.derivers ()
  |> List.map (fun (x, _) -> x)
  |> print_group "Registered Derivers"


let run_as_standalone_driver () =
  let request_print_transformations = ref false in
  let output = ref None in
  let output_mode = ref Pretty_print in
  let output_mode_arg = ref "" in
  let files = ref [] in
  let embed_errors = ref false in
  let embed_errors_arg = ref "" in
  let spec =
    let fail fmt = Printf.ksprintf (fun s -> raise (Arg.Bad s)) fmt in
    let incompatible a b = fail "%s and %s are incompatible" a b in
    let as_ppx () = fail "--as-ppx must be passed as first argument" in
    let set_embed_errors arg =
      if !output_mode = Null then incompatible !output_mode_arg arg;
      embed_errors := true;
      embed_errors_arg := arg
    in
    let set_output_mode arg mode =
      match !output_mode, mode with
      | Pretty_print, _ ->
        if mode = Null && !embed_errors then
          incompatible !embed_errors_arg arg;
        if mode = Null && !output <> None then
          incompatible "-o" arg;
        output_mode := mode;
        output_mode_arg := arg
      | _, Pretty_print -> assert false
      | Dump_ast, Dump_ast | Null, Null -> ()
      | _ -> incompatible !output_mode_arg arg
    in
    let set_output fn =
      if !output_mode = Null then incompatible !output_mode_arg "-o";
      output := Some fn
    in
    let as_pp () =
      let arg = "--as-pp" in
      set_output_mode arg Dump_ast;
      set_embed_errors arg
    in
    [ "--as-ppx", Arg.Unit as_ppx,
      " Act as a -ppx rewriter"
    ; "--as-pp", Arg.Unit as_pp,
      " Shorthand for: --dump-ast --embed-errors"
    ; "--dump-ast", Arg.Unit (fun () -> set_output_mode "--dump-ast" Dump_ast),
      " Output a binary AST instead of source code"
    ; "--null", Arg.Unit (fun () -> set_output_mode "--null" Null),
      " Output nothing, just report errors"
    ; "-o", Arg.String set_output,
      "FILE Output to this file instead of the standard output"
    ; "--intf", Arg.String (fun fn -> files := Intf fn :: !files),
      "FILE Treat FILE as a .mli file"
    ; "--impl", Arg.String (fun fn -> files := Impl fn :: !files),
      "FILE Treat FILE as a .ml file"
    ; "--embed-errors", Arg.Unit (fun () -> set_embed_errors "--embed-errors"),
      " Embed error reported by rewriters into the AST"
    ; "--print-transformations", Arg.Set request_print_transformations,
      " Print registered transformations in their order of executions"
    ]
  in
  let spec = Arg.align (spec @ registered_args ()) in
  let me = Filename.basename Sys.executable_name in
  let usage = Printf.sprintf "%s [options] [<files>]" me in
  try
    reset_args ();
    Arg.parse spec (fun anon -> files := guess_file_kind anon :: !files) usage;
    if !request_print_transformations then begin
      print_transformations ();
      exit 0
    end;
    let output = !output in
    let output_mode = !output_mode in
    let embed_errors = !embed_errors in
    let config =
      (* TODO: we could add -I, -L and -g options to populate these fields. *)
      { tool_name    = "migrate_driver"
      ; include_dirs = []
      ; load_path    = []
      ; debug        = false
      ; for_package  = None
      ; extras       = []
      }
    in
    List.iter (process_file ~config ~output ~output_mode ~embed_errors)
      (List.rev !files)
  with exn ->
    Location.report_exception Format.err_formatter exn;
    exit 1

let run_as_ppx_rewriter () =
  let a = Sys.argv in
  let n = Array.length a in
  if n <= 2 then begin
    let me = Filename.basename Sys.executable_name in
    Arg.usage (registered_args ())
      (Printf.sprintf "%s [options] <input ast file> <output ast file>" me);
    exit 2
  end;
  match
    Ast_mapper.apply ~source:a.(n - 2) ~target:a.(n - 1)
      (run_as_ast_mapper (Array.to_list (Array.sub a 1 (n - 3))))
  with
  | () -> exit 0
  | exception (Arg.Bad help) ->
      prerr_endline help;
      exit 1
  | exception exn ->
      Location.report_exception Format.err_formatter exn;
      exit 1

let run_main () =
  if Array.length Sys.argv >= 2 && Sys.argv.(1) = "--as-ppx" then
    run_as_ppx_rewriter ()
  else
    run_as_standalone_driver ();
  exit 0
