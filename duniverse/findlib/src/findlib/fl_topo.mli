(* $Id$
 * ----------------------------------------------------------------------
 *
 *)

(* The type topo.t is a partially ordered relation. You can add an element
 * by giving all descendents ...
 *)

module type IdentifiedType = 
  sig
    type t
    type id_t
    val id : t -> id_t
  end

exception Inconsistent_ordering

module type S =
  sig
    type key
    type el_t
    type t
    val create : unit -> t
    val add : t -> el_t -> unit
    val let_le : t -> key -> key -> unit
    val find : t -> key -> el_t
    val le_than : t -> key -> key -> bool
    val key : el_t -> key
    val iter_up : (el_t -> unit) -> t -> unit
    val iter_down : (el_t -> unit) -> t -> unit
    val iter_up_at : (el_t -> unit) -> t -> key list -> unit
    val iter_down_at : (el_t -> unit) -> t -> key list -> unit
    val clear : t -> unit
    val replace : t -> key -> el_t -> unit
    val delete : t -> key -> unit
    val copy : t -> t
  end

module Make(H: IdentifiedType): 
    (S with type el_t = H.t
        and type key = H.id_t)
