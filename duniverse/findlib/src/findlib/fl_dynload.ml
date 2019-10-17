(* $Id$ *)

(* Utilities for loading dynamically packages *)

open Printf

let load_pkg ~debug pkg =
  if not (Findlib.is_recorded_package pkg) then (
     if debug then
       eprintf "[DEBUG] Fl_dynload: about to load: %s\n%!" pkg;
     (* Determine the package directory: *)
     let d = Findlib.package_directory pkg in
     (* First try the new "plugin" variable: *)
     let preds = Findlib.recorded_predicates() in
     let archive =
       try
         Findlib.package_property preds pkg "plugin"
       with
         | Not_found ->
              (* Legacy: use "archive" but require that the predicate
                 "plugin" is mentioned in the definition
               *)
              try
                let v, fpreds =
                  Findlib.package_property_2 ("plugin"::preds) pkg "archive" in
                let need_plugin =
                  List.mem "native" preds in
                if need_plugin && not (List.mem (`Pred "plugin") fpreds) then
                  ""
                else
                  v
              with Not_found -> "" in
     (* Split the plugin/archive property and resolve the files: *)
     let files = Fl_split.in_words archive in
     if debug then
       eprintf "[DEBUG] Fl_dynload: files=%S\n%!" archive;
     List.iter 
       (fun file ->
          if debug then
            eprintf "[DEBUG] Fl_dynload: loading %S\n%!" file;
          let file = Findlib.resolve_path ~base:d file in
          Dynlink.loadfile file
       ) files;
     Findlib.record_package Findlib.Record_load pkg
  )
  else
    if debug then
      eprintf "[DEBUG] Fl_dynload: not loading: %s\n%!" pkg


let load_packages ?(debug=false) pkgs =
  let preds = Findlib.recorded_predicates() in
  let eff_pkglist =
    Findlib.package_deep_ancestors preds pkgs in
  List.iter (load_pkg ~debug) eff_pkglist
