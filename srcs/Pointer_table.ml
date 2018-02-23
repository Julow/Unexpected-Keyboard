(** Pointer table
	Implemented as a mutable Hashtbl
	Pointers can be added, moved and removed
	A pointer store its initial position, current position
		and attached value *)

(** A pointer *)
type p = {
	down_x : float;
	down_y : float;
	mutable x : float;
	mutable y : float
}

(** Change the position of a pointer *)
let p_move p x y =
	p.x <- x;
	p.y <- y

module Tbl = Hashtbl.Make (struct
	type t = int
	let equal : int -> int -> bool = (=)
	let hash id = id
end)

(** Pointer table *)
type 'a t = (p * 'a) Tbl.t

(** New empty table *)
let create () = Tbl.create 5

(** Find a pointer and its value
	Raises `Not_found` if the pointer is not in the map *)
let find = Tbl.find

(** Adds a pointer
	With initial position (`x`, `y`) and value `value` *)
let add t id x y value =
	let p = { down_x = x; down_y = y; x; y } in
	Tbl.replace t id (p, value)

(** Removes a pointer by its ID *)
let remove = Tbl.remove

(** Updates the position of a pointer
	Raises `Not_found` if the pointer is not in the map *)
let move t id x y =
	let p, _ = find t id in
	p_move p x y
