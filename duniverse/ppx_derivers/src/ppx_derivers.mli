(** Ppx derivers

    This module holds the various derivers registered by either ppx_deriving or
    ppx_type_conv.
*)

(** Type of a deriver. The concrete constructors are added by
    ppx_type_conv/ppx_deriving. *)
type deriver = ..

(** [register name deriver] registers a new deriver. Raises if [name] is already
    registered. *)
val register : string -> deriver -> unit

(** Lookup a previously registered deriver *)
val lookup : string -> deriver option

(** [derivers ()] returns all currently registered derivers. *)
val derivers : unit -> (string * deriver) list
