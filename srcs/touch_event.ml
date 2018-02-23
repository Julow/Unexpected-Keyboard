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

(** Handle touch events
	Use an internal Pointer_table to track pointer positions
	Send back the value of the keys to `send_key` *)
let on_touch send_key layout view =
	let pointers = Pointer_table.create () in
	let handle action id x y =
		let x = x /. float (View.get_width view)
		and y = y /. float (View.get_height view) in
		match action with
		| `Move		-> Pointer_table.move pointers id x y
		| `Down		->
			begin match KeyboardLayout.pick layout x y with
				| Some (_, key)	-> Pointer_table.add pointers id x y key
				| None			-> ()
			end
		| `Up		->
			begin match Pointer_table.find pointers id with
				| exception Not_found	-> ()
				| Pointer_table.{ down_x; down_y; x; y }, key ->
					let dx = down_x -. x and dy = down_y -. y in
					send_key (pointed_value dx dy key);
					Pointer_table.remove pointers id
			end
		| `Cancel	-> Pointer_table.remove pointers id
	in
	handle_motion_event handle
