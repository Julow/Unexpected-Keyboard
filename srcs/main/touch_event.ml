(** Handle touch events *)
open Android_view

(** Pointers store its initial position, id and key *)
type pointer = {
	ptrid	: int;
	down_x	: float;
	down_y	: float;
	key		: Key.t;
	value	: Key.value
}

let pointer ptrid down_x down_y key =
	{ ptrid; down_x; down_y; key; value = key.v }

type state = pointer list

(** Event returned by [on_touch]
	The [Key.t] values can be compared physically to track pointers *)
type event =
	| Key_down of Key.t * state
	| Key_up of Key.t * Key.value * state
	| Pointer_changed of Key.t * Key.value * state
	| Cancelled of Key.t * state
	| Ignore

let empty_state = []

let get_pointer ptrid state = List.find (fun p -> p.ptrid = ptrid) state
let remove_pointer pointers ptrid = List.filter (fun p -> p.ptrid <> ptrid) pointers

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

(** Returns the activated value of a key,
	Raises [Not_found] if the key is not activated at all *)
let key_activated state key =
	let p = List.find (fun p -> p.key == key) state in
	p.value

let down ~layout ptrid x y state =
	match KeyboardLayout.pick layout x y with
	| Some (_, key)	-> Key_down (key, pointer ptrid x y key :: state)
	| None			-> Ignore

let up ptrid x y state =
	match get_pointer ptrid state with
	| exception Not_found	-> Ignore
	| p						->
		let dx, dy = p.down_x -. x, p.down_y -. y in
		let v = pointed_value dx dy p.key in
		Key_up (p.key, v, remove_pointer state ptrid)

let cancel ptrid x y state =
	match get_pointer ptrid state with
	| exception Not_found	-> Ignore
	| p						-> Cancelled (p.key, remove_pointer state ptrid)

let move ptrid x y state =
	match get_pointer ptrid state with
	| exception Not_found	-> Ignore
	| p						->
		let dx, dy = p.down_x -. x, p.down_y -. y in
		let value = pointed_value dx dy p.key in
		if value = p.value
		then Ignore
		else
			let state = { p with value } :: remove_pointer state ptrid in
			Pointer_changed (p.key, value, state)

(** Process touch events *)
let on_touch view layout state ev =
	let go f index =
		let x = Motion_event.get_x ev index /. float (View.get_width view)
		and y = Motion_event.get_y ev index /. float (View.get_height view)
		and ptrid = Motion_event.get_pointer_id ev index in
		f ptrid x y state
	in
	let go' f = go f (Motion_event.get_action_index ev) in
	let rec go_move i =
		match go move i with
		| Ignore when i > 0	-> go_move (i - 1)
		| r					-> r
	in
	let action = Motion_event.get_action_masked ev in
	if action = motion_event_ACTION_MOVE
	then go_move (Motion_event.get_pointer_count ev - 1)
	else if action = motion_event_ACTION_UP
		|| action = motion_event_ACTION_POINTER_UP
	then go' up
	else if action = motion_event_ACTION_DOWN
		|| action = motion_event_ACTION_POINTER_DOWN
	then go' (down ~layout)
	else if action = motion_event_ACTION_CANCEL
	then go' cancel
	else Ignore
