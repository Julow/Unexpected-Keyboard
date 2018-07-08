(** Handle touch events *)
open Android_view

(** Pointers store its initial position, id and key *)
type pointer = {
	id		: int;
	down_x	: float;
	down_y	: float;
	curr_x	: float;
	curr_y	: float;
	key		: Key.t
}

let pointer id down_x down_y key =
	{ id; down_x; down_y; curr_x = down_x; curr_y = down_y; key }

type state = pointer list

let empty_state = []

let corner_dist = 0.1

let remove_pointer pointers id = List.filter (fun p -> p.id <> id) pointers

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

let rec list_init f acc i =
	let i = i - 1 in
	if i < 0
	then acc
	else list_init f (f i :: acc) i

(** Convert a MotionEvent into a more digestible representation *)
let read_motion_event ev =
	let get index =
		let x = Motion_event.get_x ev index
		and y = Motion_event.get_y ev index
		and id = Motion_event.get_pointer_id ev index in
		id, x, y
	in
	let action = Motion_event.get_action_masked ev
	and index ev = Motion_event.get_action_index ev in
	if action = motion_event_ACTION_MOVE
	then `Move (list_init get [] (Motion_event.get_pointer_count ev))
	else if action = motion_event_ACTION_UP
		|| action = motion_event_ACTION_POINTER_UP
	then `Up (get (index ev))
	else if action = motion_event_ACTION_DOWN
		|| action = motion_event_ACTION_POINTER_DOWN
	then `Down (get (index ev))
	else if action = motion_event_ACTION_CANCEL
	then `Cancel (Motion_event.get_pointer_id ev (index ev))
	else `Ignore

(** Returns the activated value of a key,
	or None if the key is not activated at all *)
let activated state key =
	match List.find (fun p -> p.key == key) state with
	| exception Not_found	-> None
	| p						-> Some (pointed_value p.curr_x p.curr_y key)

let is_activated state key =
	List.exists (fun p -> p.key == key) state

(** Handle touch events *)
let on_touch view layout state ev =
	let pos x y =
		x /. float (View.get_width view),
		y /. float (View.get_height view)
	in
	match read_motion_event ev with
	| `Down (id, x, y)	->
		let x, y = pos x y in
		begin match KeyboardLayout.pick layout x y with
			| Some (_, key)	-> `Key_down (key, pointer id x y key :: state)
			| None			-> `No_change
		end
	| `Up (id, x, y)	->
		begin match List.find (fun p -> p.id = id) state with
			| exception Not_found	-> `No_change
			| p						->
				let x, y = pos x y in
				let dx, dy = p.down_x -. x, p.down_y -. y in
				let v = pointed_value dx dy p.key in
				`Key_up (v, remove_pointer state id)
		end
	| `Cancel id		-> `Key_cancelled (remove_pointer state id)
	| `Move moves		->
		let update_ptr p =
			match List.find (fun (id, _, _) -> id = p.id) moves with
			| exception Not_found	-> p
			| _, curr_x, curr_y		-> { p with curr_x; curr_y }
		in
		`Moved (List.map update_ptr state)
	| `Ignore			-> `No_change
