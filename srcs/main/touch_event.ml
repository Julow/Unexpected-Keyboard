open Android_view

let corner_dist = 0.1

(** Returns the value of the key when the pointer moved (dx, dy)
	Corner values are only considered if (dx, dy)
		has a size of `corner_dist` or more *)
let pointed_value dx dy key =
	let open Key in
	if dx *. dx +. dy *. dy <. corner_dist *. corner_dist
	then key.v
	else match dx >. 0., dy >. 0., key with
		| true, true, { a = Some v }
		| false, true, { b = Some v }
		| false, false, { c = Some v }
		| true, false, { d = Some v }
		| _, _, { v }		-> v

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
	then `Move (List.init (Motion_event.get_pointer_count ev) get)
	else if action = motion_event_ACTION_UP
		|| action = motion_event_ACTION_POINTER_UP
	then `Up (get (index ev))
	else if action = motion_event_ACTION_DOWN
		|| action = motion_event_ACTION_POINTER_DOWN
	then `Down (get (index ev))
	else if action = motion_event_ACTION_CANCEL
	then `Cancel (Motion_event.get_pointer_id ev (index ev))
	else `Ignore

(** Handle touch events *)
let on_touch view layout pointers ev =
	let pos x y =
		x /. float (View.get_width view),
		y /. float (View.get_height view)
	in
	match read_motion_event ev with
	| `Down (id, x, y)	->
		let x, y = pos x y in
		begin match KeyboardLayout.pick layout x y with
			| Some (_, key)	->
				let p = Pointers.Table.add pointers (Pointers.make id x y key) in
				`Pointers_changed p
			| None			-> `No_change
		end
	| `Up (id, x, y)	->
		begin match Pointers.Table.find pointers id with
			| exception Not_found	-> `No_change
			| (p : Pointers.t)		->
				let x, y = pos x y in
				let dx, dy = p.down_x -. x, p.down_y -. y in
				let v = pointed_value dx dy p.key
				and p = Pointers.Table.remove pointers id in
				`Key_pressed (v, p)
		end
	| `Cancel id		-> `Pointers_changed (Pointers.Table.remove pointers id)
	| `Move _
	| `Ignore			-> `No_change
