(* $Id$ -*- tuareg -*-
 * ----------------------------------------------------------------------
 *
 *)

open Fl_metascanner

module Have = struct
  module T = struct
    type mode = [`Byte | `Native | `Toploop | `Preprocessor | `Ppx_driver]
    type t = [
        `Mode of [ `TooMany | `None]
      (** problem in the number of mode (byte,native,syntax,...)
          in the variable  *)
      | `Archive of [`Plugin|`NoPlugin] * mode
        (** archive(plugin,...) or archive(...)) *)
      | `Plugin of [`Plugin|`NoPlugin] * mode
        (** plugin(...) *)
      | `Description
      | `Requires
      | `Version
    ]
    let compare = compare
  end
  include T
  module Set = Set.Make(T)
  module Map = Map.Make(T)
end

let scan_def acc def =
  let add have = Have.Map.add have def acc in
  let has_plugin_pred = List.mem (`Pred "plugin") def.def_preds in
  let plugin = if has_plugin_pred then `Plugin else `NoPlugin in
  let modes = [ "byte", `Byte;
                "native", `Native;
                "toploop", `Toploop;
                "preprocessor", `Preprocessor;
                "ppx_driver", `Ppx_driver
              ] in
  let modes =
    List.filter
      (fun (p,_) -> List.mem (`Pred p) def.def_preds)
      modes
  in
  let modes = List.map snd modes in
  match def.def_var, modes with
  (** For archive the modes are used in multiple ways, so we can't
      check exhaustiveness or presence.
  *)
  | "plugin", [] -> add (`Mode(`None))
  | "plugin", _::_::_ -> add (`Mode(`TooMany))

  | "archive", [mode] -> add (`Archive(plugin,mode))
  | "plugin", [mode]  -> add (`Plugin(plugin,mode))
  | "description", _ -> add `Description
  | "requires", _ -> add `Requires
  | "version", _ -> add `Version
  | _ -> acc


let warn_def ~warned pkg =
  let haves =
    List.fold_left scan_def Have.Map.empty pkg.pkg_defs
  in
  let mem x  = Have.Map.mem x haves in
  let find x = Have.Map.find x haves in
  let warning fmt = warned := true; Printf.printf fmt in
  let if_ ?has ?(has_not=[]) msg =
    match has, has_not with
    | Some has, [] when mem has ->
      warning "%a%s\n\n" print_def (find has) msg;
    | Some has, has_not when mem has && not (List.exists mem has_not) ->
      warning "%a%s\n\n" print_def (find has) msg;
    | None, has_not when not (List.exists mem has_not) ->
      warning "%s\n\n" msg;
    | _ -> ()
  in
  if_ ~has_not:[`Description]
    "You should add a description.";
  if_ ~has_not:[`Version]
    "You should add a version.";
  if_ ~has_not:[`Requires]
    "You should add the required libraries. You can silent this \
     warning by using the empty string.";
  if_ ~has:(`Mode(`TooMany))
    "This variable should have only one mode
     (\"byte\", \"native\").";
  if_ ~has:(`Mode(`None))
    "This variable should have at least the predicate \
     \"byte\" or \"native\".";
  let with_mode mode =
    if_ ~has:(`Plugin (`Plugin,mode))
      "You must not add the predicate \"plugin\" to the variable \
       \"plugin\".";
    if_ ~has:(`Archive (`Plugin,mode)) ~has_not:[`Plugin (`NoPlugin,mode)]
      "This specification of dynamic loading is deprecated, you should add a \
       \"plugin(...)\" variable.";
    if_ ~has:(`Archive (`NoPlugin,mode))
      ~has_not:[`Plugin (`NoPlugin,mode);`Archive (`Plugin,mode)]
      "This variable indicates how to link statically, you should add a \
       \"plugin(...)\" variable for linking dynamically.";
  in
  with_mode `Byte;
  with_mode `Native

let warn pkg =
  let warned = ref false in
  let rec aux pkg =
    warn_def ~warned pkg;
    List.iter (fun (_,pkg) -> aux pkg) pkg.pkg_children;
  in
  aux pkg;
  !warned
