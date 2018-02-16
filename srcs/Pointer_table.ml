
(* `down_x` and `down_y` the initial position
	and `x` and `y` are the current position *)
type p = {
	down_x : float;
	down_y : float;
	mutable x : float;
	mutable y : float
}

(** Pointer table, where the key is the pointer id *)
module Tbl = Hashtbl.Make (struct
	type t = int
	let equal : int -> int -> bool = (=)
	let hash id = id
end)

(** Pointer table
	Implemented as a mutable Hashtbl *)
type 'a t = (p * 'a) Tbl.t

let create () = Tbl.create 5

(** Checks if a pointer is in the map *)
let mem t id = Tbl.mem t id

(** Find a pointer and its value
	Raises `Not_found` if the pointer is not in the map *)
let find t id = Tbl.find t id

(** Adds a pointer *)
let add_pointer t id down_x down_y value =
	Tbl.replace t id
		({ down_x; down_y; x = down_x; y = down_y }, value)

(** Removes a pointer by its ID *)
let rem_pointer t id =
	Tbl.remove t id

(** Updates the position of a pointer *)
let pointer_move t id x y =
	let p, _ = find t id in
	p.x <- x;
	p.y <- y
