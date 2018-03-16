open Android_view

(** Easier handling of motion events
	`f` is called with the action, pointer ID, x/y coordinates
	Events other than move/up/down/cancel are ignored
	The move event calls `f` for each pointers *)
let handle_motion_event f ev =
	let handle action index =
		let x = Motion_event.get_x ev index
		and y = Motion_event.get_y ev index
		and id = Motion_event.get_pointer_id ev index in
		f action id x y
	in
	let action = Motion_event.get_action_masked ev
	and index = Motion_event.get_action_index ev in
	if action == motion_event_ACTION_MOVE then
		for idx = 0 to Motion_event.get_pointer_count ev - 1 do
			handle `Move idx
		done
	else if action == motion_event_ACTION_UP
		|| action == motion_event_ACTION_POINTER_UP then
		handle `Up index
	else if action == motion_event_ACTION_DOWN
		|| action == motion_event_ACTION_POINTER_DOWN then
		handle `Down index
	else if action == motion_event_ACTION_CANCEL then
		handle `Cancel index
	else
		()

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

module Pointers =
struct

	(** Pointers in a mutable list
		Pointers store its initial position, id and key *)

	type pointer = {
		id		: int;
		down_x	: float;
		down_y	: float;
		key		: Key_value.t Key.t
	}

	type t = pointer list ref

	let create () = ref []

	let pointer id down_x down_y key = { id; down_x; down_y; key }

	let add t ptr = t := ptr :: !t
	let find t id = List.find (fun p -> p.id = id) !t
	let exists t id = List.exists (fun p -> p.id = id) !t
	let key_exists t key = List.exists (fun p -> p.key == key) !t
	let remove t id = t := List.filter (fun p -> p.id <> id) !t

end

(** Handle touch events
	Send back the value of the keys to `send_key` *)
let on_touch invalidate send_key view =
	let pointers = Pointers.create () in
	let handle layout action id x y =
		let x = x /. float (View.get_width view)
		and y = y /. float (View.get_height view) in
		match action with
		| `Move		-> ()
		| `Down		->
			begin match KeyboardLayout.pick layout x y with
				| Some (_, key)	->
					Pointers.add pointers (Pointers.pointer id x y key);
					invalidate ()
				| None			-> ()
			end
		| `Up		->
			begin match Pointers.find pointers id with
				| exception Not_found				-> ()
				| Pointers.{ down_x; down_y; key }	->
					let dx = down_x -. x and dy = down_y -. y in
					send_key (pointed_value dx dy key);
					Pointers.remove pointers id
			end;
			invalidate ();
		| `Cancel	->
			Pointers.remove pointers id;
			invalidate ()
	in
	let handle layout ev = handle_motion_event (handle layout) ev
	and is_activated key = Pointers.key_exists pointers key in
	handle, is_activated
