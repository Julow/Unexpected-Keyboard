open Migrate_parsetree
open Ast_405
open Ppx_tools_405

(*
 * This ppx rewrites expression extension of the form [%once expr].
 * The first time the expression is evaluated, its result is cached.
 * After that, the evaluation will be skipped and the result reused.
 *
 * The ppx introduces references at the top-level to cache the results.
 *)

let inject_once var code =
  [%expr match ![%e var] with
         | Some result -> result
         | None ->
             let result = [%e code] in
             [%e var] := Some result;
             result]
  [@metaloc {code.Parsetree.pexp_loc with Location.loc_ghost = true}]

let mapper _config _cookies =
  let open Ast_mapper in
  let toplevel = ref true in
  let uid = ref 0 in
  let insert = ref [] in
  let make_option name = [%str let [%p Ast_helper.Pat.var name] = ref None] in
  let toplevel_structure mapper str =
    let items = List.fold_left (fun acc x ->
      let x' = mapper.structure_item mapper x in
      let items = List.map make_option !insert in
      insert := [];
      x' :: List.concat items @ acc
    ) [] str
    in
    List.rev items
  in
  let expr mapper pexp =
    let open Parsetree in
    match pexp.pexp_desc with
    | Pexp_extension (
        {Location. txt = "once"; loc},
        payload
      ) ->
        begin match payload with
        | PStr [{pstr_desc = Pstr_eval (body, []) }] ->
          incr uid;
          let name = "__ppx_once_" ^ string_of_int !uid in
          insert := Location.mkloc name loc :: !insert;
          inject_once
            (Ast_helper.Exp.ident (Location.mkloc (Longident.Lident name) loc))
            (mapper.expr mapper body)
        | _ -> default_mapper.expr mapper pexp
        end;
    | _ -> default_mapper.expr mapper pexp
  in
  let structure mapper str =
    if !toplevel then (
      uid := 0;
      insert := [];
      toplevel := false;
      match toplevel_structure mapper str with
      | result -> toplevel := true; result
      | exception exn -> toplevel := true; raise exn
    ) else
      default_mapper.structure mapper str
  in
  {default_mapper with structure; expr}

let () = Driver.register ~name:"ppx_once" Versions.ocaml_405 mapper
