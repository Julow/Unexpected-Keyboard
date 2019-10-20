(** Handle touch events *)
type touch_event_kind = Up | Down | Cancel | Move

type touch_event = {
  kind : touch_event_kind;
  ptr_id : int;
  x : float;
  y : float;
}

(** Pointers store its initial position, id and key *)
type pointer = {
	ptr_id	: int;
	down_x	: float;
	down_y	: float;
	key		: Key.t;
	value	: Key.value
}

let pointer { ptr_id; x; y; _ } key =
	{ ptr_id; down_x = x; down_y = y; key; value = key.v }

type state = pointer list

(** Event returned by [on_touch]
	The [Key.t] values can be compared physically to track pointers *)
type event =
	| Key_down of Key.t * state
	| Key_up of Key.t * Key.value * state
	| Pointer_changed of Key.t * Key.value * Key.value * state
	| Cancelled of Key.t * Key.value * state
	| Ignore

let empty_state = []

let get_pointer ptr_id state = List.find (fun p -> p.ptr_id = ptr_id) state
let remove_pointer pointers ptr_id = List.filter (fun p -> p.ptr_id <> ptr_id) pointers

let corner_dist = 0.1

(** Returns the value of the key when the pointer moved (dx, dy)
	Corner values are only considered if (dx, dy)
		has a size of `corner_dist` or more *)
let pointed_value dx dy key =
	let open Key in
	if dx *. dx +. dy *. dy < corner_dist *. corner_dist
	then key.v
	else match dx > 0., dy > 0., key with
    | true, true, { a = Some v; _ }
    | false, true, { b = Some v; _ }
    | false, false, { c = Some v; _ }
    | true, false, { d = Some v; _ }
    | _, _, { v; _ }		-> v

(** Returns the activated value of a key,
	Raises [Not_found] if the key is not activated at all *)
let key_activated state key =
	let p = List.find (fun p -> p.key == key) state in
	p.value

let down ~layout state ev =
	match KeyboardLayout.pick layout ev.x ev.y with
	| Some (_, key)	-> Key_down (key, pointer ev key :: state)
	| None			-> Ignore

let up state (ev : touch_event) =
	match get_pointer ev.ptr_id state with
	| exception Not_found	-> Ignore
	| p						->
		let dx, dy = p.down_x -. ev.x, p.down_y -. ev.y in
		let v = pointed_value dx dy p.key in
		Key_up (p.key, v, remove_pointer state ev.ptr_id)

let cancel state (ev : touch_event) =
	match get_pointer ev.ptr_id state with
	| exception Not_found	-> Ignore
	| p						->
		Cancelled (p.key, p.value, remove_pointer state ev.ptr_id)

let move state (ev : touch_event) =
	match get_pointer ev.ptr_id state with
	| exception Not_found	-> Ignore
	| p						->
		let dx, dy = p.down_x -. ev.x, p.down_y -. ev.y in
		let value = pointed_value dx dy p.key in
		if value = p.value
		then Ignore
		else
			let state = { p with value } :: remove_pointer state ev.ptr_id in
			Pointer_changed (p.key, p.value, value, state)

(** Process touch events *)
let on_touch layout state ev =
  match ev.kind with
  | Up -> up state ev
  | Down -> down ~layout state ev
  | Move -> move state ev
  | Cancel -> cancel state ev
