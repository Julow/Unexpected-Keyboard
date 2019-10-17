(* $Id$ -*- tuareg -*-
 * ----------------------------------------------------------------------
 *
 *)
open Fl_metatoken
  
open Printf
  
type formal_pred = [ | `Pred of string | `NegPred of string ]

type flavour = [ | `BaseDef | `Appendix ]

type pkg_definition =
  { def_var : string; def_flav : flavour; def_preds : formal_pred list;
    def_value : string
  }

type pkg_expr =
  { pkg_defs : pkg_definition list; pkg_children : (string * pkg_expr) list
  }

let string_of_preds pl =
  let print = function | `Pred n -> n | `NegPred n -> "-" ^ n
  in
    if pl = []
    then ""
    else "(" ^ ((String.concat "," (List.map print pl)) ^ ")")
  
let scan_lexing buf =
  (* transform a Lexing.lexbuf into a token stream; 'Space' tokens are left
   * out.
   *)
  let rec next line pos0 =
    let t = Fl_meta.token buf
    in
      match t with
      | Space -> next line pos0
      | Newline -> next (line + 1) (Lexing.lexeme_end buf)
      | Eof ->
          let pos = (Lexing.lexeme_start buf) - pos0
          in Stream.lsing (fun _ -> (line, pos, Eof))
      | _ ->
          let pos = (Lexing.lexeme_start buf) - pos0
          in
            Stream.lcons (fun _ -> (line, pos, t))
              (Stream.slazy (fun _ -> next line pos0))
  in next 1 0
  
let scan ch = scan_lexing (Lexing.from_channel ch)
  
let parse_lexing lexbuf =
  let rec mk_set l =
    match l with
    | x :: l' -> if List.mem x l' then mk_set l' else x :: (mk_set l')
    | [] -> [] in
  let error_msg m line col =
    m ^
      (" at line " ^
         ((string_of_int line) ^ (" position " ^ (string_of_int col)))) in
  let rec parse_all need_rparen stream =
    let (__strm : _ Stream.t) = stream
    in
      match Stream.peek __strm with
      | Some ((line, col, Name "package")) ->
          (Stream.junk __strm;
           (match Stream.peek __strm with
            | Some ((l, c, String n)) ->
                (Stream.junk __strm;
                 (match Stream.peek __strm with
                  | Some ((l, c, LParen)) ->
                      (Stream.junk __strm;
                       let subpkg =
                         (try parse_all true __strm
                          with
                          | Stream.Failure ->
                              raise
                                (Stream.Error
                                   (error_msg
                                      "Error in subpackage definition" line
                                      col))) in
                       let rest =
                         (try parse_all need_rparen __strm
                          with | Stream.Failure -> raise (Stream.Error ""))
                       in
                         {
                           pkg_defs = rest.pkg_defs;
                           pkg_children = (n, subpkg) :: rest.pkg_children;
                         })
                  | _ ->
                      raise
                        (Stream.Error
                           (error_msg "'(' expected after string" l c))))
            | _ ->
                raise
                  (Stream.Error
                     (error_msg "String literal expected after 'package'"
                        line col))))
      | Some ((line, col, Name n)) ->
          (Stream.junk __strm;
           let props =
             (try parse_properties __strm
              with
              | Stream.Failure ->
                  raise
                    (Stream.Error
                       (error_msg "Error in 'name = value' clause" line col))) in
           let rest =
             (try parse_all need_rparen __strm
              with | Stream.Failure -> raise (Stream.Error "")) in
           let (args, flav, value) = props in (* TODO: Check args *)
           let args' = List.sort compare (mk_set args) in
           let def =
             {
               def_var = n;
               def_flav = flav;
               def_preds = args';
               def_value = value;
             }
           in
             {
               pkg_defs = def :: rest.pkg_defs;
               pkg_children = rest.pkg_children;
             })
      | Some ((line, col, Eof)) ->
          (Stream.junk __strm;
           if need_rparen
           then
             raise
               (Stream.Error
                  ("Unexpected end of file in line " ^
                     ((string_of_int line) ^
                        (" position " ^ (string_of_int col)))))
           else ();
           { pkg_defs = []; pkg_children = []; })
      | Some ((line, col, RParen)) ->
          (Stream.junk __strm;
           if not need_rparen
           then
             raise
               (Stream.Error
                  ("Unexpected ')' in line " ^
                     ((string_of_int line) ^
                        (" position " ^ (string_of_int col)))))
           else ();
           { pkg_defs = []; pkg_children = []; })
      | Some ((line, col, _)) ->
          (Stream.junk __strm;
           raise
             (Stream.Error
                (error_msg "Expected 'name = value' clause" line col)))
      | _ -> raise Stream.Failure
  and parse_properties stream =
    let (__strm : _ Stream.t) = stream
    in
      match Stream.peek __strm with
      | Some ((line, col, LParen)) ->
          (Stream.junk __strm;
           let arg1 =
             (try parse_argument __strm
              with | Stream.Failure -> raise (Stream.Error "")) in
           let args =
             (try parse_arguments __strm
              with | Stream.Failure -> raise (Stream.Error "")) in
           let flav =
             (try parse_flavour __strm
              with | Stream.Failure -> raise (Stream.Error ""))
           in
             (match Stream.peek __strm with
              | Some ((line3, col3, String s)) ->
                  (Stream.junk __strm; ((arg1 :: args), flav, s))
              | _ ->
                  raise
                    (Stream.Error
                       (error_msg "Expected string constant after '='" line
                          col))))
      | Some ((line, col, Equal)) ->
          (Stream.junk __strm;
           (match Stream.peek __strm with
            | Some ((_, _, String s)) ->
                (Stream.junk __strm; ([], `BaseDef, s))
            | _ ->
                raise
                  (Stream.Error
                     (error_msg
                        "'=' must be followed by a string constant in line "
                        line col))))
      | Some ((line, col, PlusEqual)) ->
          (Stream.junk __strm;
           (match Stream.peek __strm with
            | Some ((_, _, String s)) ->
                (Stream.junk __strm; ([], `Appendix, s))
            | _ ->
                raise
                  (Stream.Error
                     (error_msg
                        "'+=' must be followed by a string constant in line "
                        line col))))
      | Some ((line, col, _)) ->
          (Stream.junk __strm;
           raise
             (Stream.Error
                (error_msg "Expected a '=' or a '(arguments,...)=' clause"
                   line col)))
      | _ -> raise Stream.Failure
  and parse_arguments stream =
    let (__strm : _ Stream.t) = stream
    in
      match Stream.peek __strm with
      | Some ((line, col, Comma)) ->
          (Stream.junk __strm;
           let arg =
             (try parse_argument __strm
              with | Stream.Failure -> raise (Stream.Error "")) in
           let args =
             (try parse_arguments __strm
              with | Stream.Failure -> raise (Stream.Error ""))
           in arg :: args)
      | Some ((_, _, RParen)) -> (Stream.junk __strm; [])
      | Some ((line, col, _)) ->
          (Stream.junk __strm;
           raise
             (Stream.Error
                (error_msg "Another predicate or a ')' expected" line col)))
      | _ -> raise Stream.Failure
  and parse_argument stream =
    let (__strm : _ Stream.t) = stream
    in
      match Stream.peek __strm with
      | Some ((line, col, Name n)) -> (Stream.junk __strm; `Pred n)
      | Some ((line, col, Minus)) ->
          (Stream.junk __strm;
           (match Stream.peek __strm with
            | Some ((l, c, Name n)) -> (Stream.junk __strm; `NegPred n)
            | _ ->
                raise
                  (Stream.Error
                     (error_msg "Name expected after '-'" line col))))
      | Some ((line, col, _)) ->
          (Stream.junk __strm;
           raise (Stream.Error (error_msg "Name or -Name expected" line col)))
      | _ -> raise Stream.Failure
  and parse_flavour stream =
    let (__strm : _ Stream.t) = stream
    in
      match Stream.peek __strm with
      | Some ((line, col, Equal)) -> (Stream.junk __strm; `BaseDef)
      | Some ((line, col, PlusEqual)) -> (Stream.junk __strm; `Appendix)
      | Some ((line, col, _)) ->
          (Stream.junk __strm;
           raise (Stream.Error (error_msg "'+' or '+=' expected" line col)))
      | _ -> raise Stream.Failure in
  let rec check_defs p l =
    match l with
    | [] -> ()
    | def :: l' ->
        (List.iter
           (fun def' ->
              if
                (def.def_var = def'.def_var) &&
                  ((def.def_preds = def'.def_preds) &&
                     ((def.def_flav = `BaseDef) && (def'.def_flav = `BaseDef)))
              then
                (let prefix =
                   if p = "" then "" else "In subpackage " ^ (p ^ ": ") in
                 let args = string_of_preds def.def_preds
                 in
                   raise
                     (Stream.Error
                        (prefix ^
                           ("Double definition of '" ^
                              (def.def_var ^ (args ^ "'"))))))
              else ())
           l';
         check_defs p l') in
  let rec check_pkg p pkg =
    (check_defs p pkg.pkg_defs;
     let l = ref []
     in
       List.iter
         (fun (n, subpkg) ->
            let p' = if p = "" then n else p ^ ("." ^ n)
            in
              (if List.mem n !l
               then
                 raise
                   (Stream.Error ("Double definition for subpackage " ^ p'))
               else ();
               if String.contains n '.'
               then
                 raise
                   (Stream.Error
                      ("Subpackage name must not contain '.': \"" ^
                         (n ^ "\"")))
               else ();
               check_pkg p' subpkg;
               l := n :: !l))
         pkg.pkg_children)
  in
    try
      let pkg = parse_all false (scan_lexing lexbuf)
      in (check_pkg "" pkg; pkg)
    with | Stream.Error "" -> raise (Stream.Error "Syntax Error")
  
let parse ch = parse_lexing (Lexing.from_channel ch)
  
let scan2_lexing buf =
  (* transform an in_channel to a token stream; 'Space' tokens are left
   * out.
   *)
  let (line_ref, pos0_ref, eof_found) = ((ref 1), (ref 0), (ref false))
  in
    fun () ->
      let rec next line pos0 =
        let t = Fl_meta.token buf
        in
          match t with
          | Space -> next line pos0
          | Newline -> next (line + 1) (Lexing.lexeme_end buf)
          | Eof -> (eof_found := true; produce line pos0 Eof)
          | _ -> produce line pos0 t
      and produce line pos0 t =
        (line_ref := line;
         pos0_ref := pos0;
         let pos = (Lexing.lexeme_start buf) - pos0 in (line, pos, t))
      in
        if !eof_found
        then produce !line_ref !pos0_ref Eof
        else next !line_ref !pos0_ref
  
let scan2 ch = scan2_lexing (Lexing.from_channel ch)
  
let parse2_lexing lexbuf =
  let rec mk_set l =
    match l with
    | x :: l' -> if List.mem x l' then mk_set l' else x :: (mk_set l')
    | [] -> [] in
  let error_msg msg line col =
    Printf.sprintf "%s at line %d position %d" msg line col in
  let next_token = scan2_lexing lexbuf in
  let raise_err error_fun line col =
    raise (Stream.Error (error_fun line col)) in
  let get_tok test error_fun =
    let (line, col, tok) = next_token ()
    in
      match test tok with
      | None -> raise_err error_fun line col
      | Some result -> result in
  let get_rule rule arg error_fmt line col =
    try rule arg with | Stream.Error _ -> raise_err error_fmt line col in
  let rec parse_all need_rparen =
    match next_token () with
    | (line, col, Name "package") ->
        let n =
          get_tok string_tok
            (error_msg "String literal expected after 'package'") in
        let () =
          get_tok (const_tok LParen) (error_msg "'(' expected after string") in
        let subpkg =
          get_rule parse_all true
            (error_msg "Error in subpackage definition") line col in
        let rest = parse_all need_rparen
        in
          {
            pkg_defs = rest.pkg_defs;
            pkg_children = (n, subpkg) :: rest.pkg_children;
          }
    | (line, col, Name n) ->
        let (args, flav, value) =
          get_rule parse_properties ()
            (error_msg "Error in 'name = value' clause") line col in
        let rest = parse_all need_rparen in (* TODO: Check args *)
        let args' = List.sort compare (mk_set args) in
        let def =
          {
            def_var = n;
            def_flav = flav;
            def_preds = args';
            def_value = value;
          }
        in
          {
            pkg_defs = def :: rest.pkg_defs;
            pkg_children = rest.pkg_children;
          }
    | (line, col, Eof) ->
        (if need_rparen
         then
           raise_err
             (Printf.sprintf "Unexpected end of file in line %d position %d")
             line col
         else ();
         { pkg_defs = []; pkg_children = []; })
    | (line, col, RParen) ->
        (if not need_rparen
         then
           raise_err
             (Printf.sprintf "Unexpected end of file in line %d position %d")
             line col
         else ();
         { pkg_defs = []; pkg_children = []; })
    | (line, col, _) ->
        raise_err (error_msg "Expected 'name = value' clause") line col
  and parse_properties () =
    match next_token () with
    | (line, col, LParen) ->
        let arg1 = parse_argument () in
        let args = parse_arguments () in
        let flav = parse_flavour () in
        let s =
          get_tok string_tok (error_msg "Expected string constant after '='")
        in ((arg1 :: args), flav, s)
    | (line, col, Equal) ->
        let s =
          get_tok string_tok
            (error_msg "'=' must be followed by a string constant")
        in ([], `BaseDef, s)
    | (line, col, PlusEqual) ->
        let s =
          get_tok string_tok
            (error_msg "'+=' must be followed by a string constant")
        in ([], `Appendix, s)
    | (line, col, _) ->
        raise_err (error_msg "Expected a '=' or a '(arguments,...)=' clause")
          line col
  and parse_arguments () =
    match next_token () with
    | (line, col, Comma) ->
        let arg = parse_argument () in
        let args = parse_arguments () in arg :: args
    | (_, _, RParen) -> []
    | (line, col, _) ->
        raise_err (error_msg "Another predicate or a ')' expected") line col
  and parse_argument () =
    match next_token () with
    | (line, col, Name n) -> `Pred n
    | (line, col, Minus) ->
        let n = get_tok name_tok (error_msg "Name expected after '-'")
        in `NegPred n
    | (line, col, _) ->
        raise_err (error_msg "Name or -Name expected") line col
  and parse_flavour () =
    match next_token () with
    | (line, col, Equal) -> `BaseDef
    | (line, col, PlusEqual) -> `Appendix
    | (line, col, _) -> raise_err (error_msg "'+' or '+=' expected") line col in
  let rec check_defs p l =
    match l with
    | [] -> ()
    | def :: l' ->
        (List.iter
           (fun def' ->
              if
                (def.def_var = def'.def_var) &&
                  ((def.def_preds = def'.def_preds) &&
                     ((def.def_flav = `BaseDef) && (def'.def_flav = `BaseDef)))
              then
                (let prefix =
                   if p = "" then "" else "In subpackage " ^ (p ^ ": ") in
                 let args = string_of_preds def.def_preds
                 in
                   raise
                     (Stream.Error
                        (prefix ^
                           ("Double definition of '" ^
                              (def.def_var ^ (args ^ "'"))))))
              else ())
           l';
         check_defs p l') in
  let rec check_pkg p pkg =
    (check_defs p pkg.pkg_defs;
     let l = ref []
     in
       List.iter
         (fun (n, subpkg) ->
            let p' = if p = "" then n else p ^ ("." ^ n)
            in
              (if List.mem n !l
               then
                 raise
                   (Stream.Error ("Double definition for subpackage " ^ p'))
               else ();
               if String.contains n '.'
               then
                 raise
                   (Stream.Error
                      ("Subpackage name must not contain '.': \"" ^
                         (n ^ "\"")))
               else ();
               check_pkg p' subpkg;
               l := n :: !l))
         pkg.pkg_children)
  in
    try let pkg = parse_all false in (check_pkg "" pkg; pkg)
    with | Stream.Error "" -> raise (Stream.Error "Syntax Error")
  
let parse2 ch = parse2_lexing (Lexing.from_channel ch)

let escape s = (* no Str available :-( *)
  let b = Buffer.create (String.length s)
  in
  (for k = 0 to (String.length s) - 1 do
     (match s.[k] with
      | '\\' -> Buffer.add_string b "\\\\"
      | '"' -> Buffer.add_string b "\\\""
      | c -> Buffer.add_char b c)
   done;
   Buffer.contents b)

let print_def f def =
  let format_pred = function | `Pred s -> s | `NegPred s -> "-" ^ s in
  fprintf f "%s%s %s \"%s\"\n" def.def_var
    (match def.def_preds with
     | [] -> ""
     | l -> "(" ^ ((String.concat "," (List.map format_pred l)) ^ ")"))
    (match def.def_flav with | `BaseDef -> "=" | `Appendix -> "+=")
    (escape def.def_value)


let rec print f pkg =
    (List.iter (print_def f) pkg.pkg_defs;
     List.iter
       (fun (name, child) ->
          (fprintf f "\npackage \"%s\" (\n" (escape name);
           print f child;
           fprintf f ")\n"))
       pkg.pkg_children)
  
let rec remove_dups l =
  (* FIXME: O(n^2) *)
  match l with
    x :: l' ->
      if List.mem x l' then remove_dups l' else x::remove_dups l'
  | [] -> []

let lookup_2 name predicate_list def =
  let fulfills actual_preds formal_preds =
    List.for_all
      (function
       | `Pred n -> List.mem n predicate_list
       | `NegPred n -> not (List.mem n predicate_list))
      formal_preds in
  let rec search_base best_n best_value l =
    match l with
    | [] -> if best_n >= 0 then best_value else raise Not_found
    | def :: l' ->
        if
          (name = def.def_var) &&
            ((def.def_flav = `BaseDef) &&
               ((fulfills predicate_list def.def_preds) &&
                  ((List.length def.def_preds) > best_n)))
        then search_base
               (List.length def.def_preds)
               (def.def_value, def.def_preds)
               l'
        else search_base best_n best_value l' in
  let rec search_appdx l =
    match l with
    | [] -> []
    | def :: l' ->
        if
          (name = def.def_var) &&
            ((def.def_flav = `Appendix) &&
               (fulfills predicate_list def.def_preds))
        then (def.def_value, def.def_preds) :: (search_appdx l')
        else search_appdx l' in
  let value_a, preds_a  = search_base (-1) ("",[]) def in
  let additions = search_appdx def in
  let values_b = List.map fst additions in
  let preds_b = List.flatten (List.map snd additions) in
  let value = String.concat " " (value_a :: values_b) in
  let preds = remove_dups (preds_a @ preds_b) in
  (value, preds)
  
let lookup name predicate_list def =
  fst(lookup_2 name predicate_list def)

let predicate_exists p defs =
  List.exists
    (fun def ->
       List.exists (function | `Pred n -> n = p | `NegPred n -> n = p)
         def.def_preds)
    defs
  

