(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



val temp_name : unit -> string
(** Generates the name of a temporary file (or directory) in [_build/]. Note
    that a file at the path may already exist. *)

val temp_file : unit -> string
(** Creates a temporary file in [_build/] and evaluates to its path. *)

val temp_directory : unit -> string
(** Creates a temporary directory in [build/] and evaluates to its path. *)
