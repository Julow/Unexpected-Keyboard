(** Pointers in a mutable list
	Pointers store its initial position, id and key *)

type t = {
	id		: int;
	down_x	: float;
	down_y	: float;
	key		: Key_value.t Key.t
}

let make id down_x down_y key = { id; down_x; down_y; key }

let corner_dist = 0.1

(** Returns the value of the key when the pointer moved (dx, dy)
	Corner values are only considered if (dx, dy)
		has a size of `corner_dist` or more *)
let pointed_value dx dy key =
	let open Key in
	if dx *. dx +. dy *. dy < corner_dist *. corner_dist
	then key.v
	else match dx > 0., dy > 0., key with
		| true, true, { a = Some v }
		| false, true, { b = Some v }
		| false, false, { c = Some v }
		| true, false, { d = Some v }
		| _, _, { v }		-> v

module Table =
struct

	type pointer = t
	type t = pointer list

	let empty = []

	let is_empty =
		function
		| []	-> true
		| _		-> false

	let add t ptr = ptr :: t
	let remove t id = List.filter (fun p -> p.id <> id) t

	let find t id = List.find (fun p -> p.id = id) t
	let exists t id = List.exists (fun p -> p.id = id) t
	let key_exists t key = List.exists (fun p -> p.key == key) t

end
