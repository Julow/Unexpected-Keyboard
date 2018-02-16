open Android_content
open Android_graphics
open Android_inputmethodservice
open Android_util
open Android_view

class%java log "android.util.Log" =
object
	method [@static] d : string -> string -> int = "d"
end

let log msg = ignore (Log.d "KEYBOARD" msg)

type key = {
	a : char option;
	b : char option;
	c : char option;
	d : char option;
	v : char
}

let layout = KeyboardLayout.Desc.(
	let key ?(width=1.) ?a ?b ?c ?d v = key ~width { a; b; c; d; v } in
build [
	row [
		key 'q' ~b:'1' ~d:'~' ~c:'!';
		key 'w' ~b:'2' ~d:'@';
		key 'e' ~b:'3' ~d:'#';
		key 'r' ~b:'4' ~d:'$';
		key 't' ~b:'5' ~d:'%';
		key 'y' ~b:'6' ~d:'^';
		key 'u' ~b:'7' ~d:'&';
		key 'i' ~b:'8' ~d:'*';
		key 'o' ~b:'9' ~d:'(' ~c:')';
		key 'p' ~b:'0';
	];

	row ~margin:0.5 [
		key 'a';
		key 's';
		key 'd';
		key 'f';
		key 'g';
		key 'h';
		key 'j';
		key 'k';
		key 'l';
	];

	row [
		key ~width:1.5 '%';
		key 'z';
		key 'x';
		key 'c';
		key 'v';
		key 'b';
		key 'n';
		key 'm';
		key ~width:1.5 '<';
	];

	row ~height:1.2 ~margin:1. [
		key ~width:1.5 '.';
		key ~width:6.0 ' ';
		key ~width:1.5 '^';
	]
])

let corner_dist = 0.1

(** Returns the corner a pointer (Pointer_table) is in *)
let corner p =
	let open Pointer_table in
	let sq x = x *. x in
	if sq (p.down_x -. p.x) +. sq (p.down_y -. p.y) < sq corner_dist
	then `Center
	else match p.x < p.down_x, p.y < p.down_y with
		| true, true	-> `Top_left
		| false, true	-> `Top_right
		| false, false	-> `Bottom_right
		| true, false	-> `Bottom_left

let keyboard_view context send_key =
	CustomView.create context (fun view ->
		let res = Context.get_resources context in
		let dm = Resources.get_display_metrics res in
		let text_paint = Paint.create paint_ANTI_ALIAS_FLAG in
		Paint.set_text_align text_paint (Paint_align.get'center ());
		Paint.set_text_size text_paint
			(Display_metrics.get'scaled_density dm *. 16.);
		let pointers = Pointer_table.create () in
	object

		method onTouchEvent ev =
			let width = View.get_width view
			and height = View.get_height view in
			let action = Motion_event.get_action_masked ev in
			if action = motion_event_ACTION_MOVE then begin
				for i = 0 to Motion_event.get_pointer_count ev - 1 do
					let x = Motion_event.get_x ev i /. float width
					and y = Motion_event.get_y ev i /. float height in
					let ptr_id = Motion_event.get_pointer_id ev i in
					Pointer_table.pointer_move pointers ptr_id x y
				done;
				true
			end
			else
				let ptr_index = Motion_event.get_action_index ev in
				let x = Motion_event.get_x ev ptr_index /. float width
				and y = Motion_event.get_y ev ptr_index /. float height in
				let ptr_id = Motion_event.get_pointer_id ev ptr_index in
				if action = motion_event_ACTION_DOWN
					|| action = motion_event_ACTION_POINTER_DOWN then begin
					match KeyboardLayout.pick layout x y with
					| Some (_, key)	->
						Pointer_table.add_pointer pointers ptr_id x y key;
						true
					| None			-> false
				end
				else if action = motion_event_ACTION_UP
					|| action = motion_event_ACTION_POINTER_UP then begin
					if Pointer_table.mem pointers ptr_id then begin
						let ptr, key = Pointer_table.find pointers ptr_id in
						let (|||) opt def =
							match opt with Some v -> v | None -> def in
						let key_val = match corner ptr with
							| `Center		-> key.v
							| `Top_left		-> key.a ||| key.v
							| `Top_right	-> key.b ||| key.v
							| `Bottom_right	-> key.c ||| key.v
							| `Bottom_left	-> key.d ||| key.v
						in
						send_key key_val;
						Pointer_table.rem_pointer pointers ptr_id;
						true
					end
					else false
				end
				else false

		method onMeasure w_spec _ =
			let w = Measure_spec.get_size w_spec in
			let h = 400 in
			View.set_measured_dimension view w h

		method onDraw canvas =
			let width = float (Canvas.get_width canvas)
			and height = float (Canvas.get_height canvas) in
			KeyboardLayout.iter (fun (pos, key) ->
				let x, y = KeyboardLayout.(
					(pos.x +. pos.width /. 2.) *. width,
					(pos.y +. pos.height /. 2.) *. height
				) in
				let text = String.make 1 key.v in
				Canvas.draw_text canvas text x y text_paint
			) layout

		method onDetachedFromWindow = ()

	end)

let () = Printexc.record_backtrace true

let () =
	UnexpectedKeyboardService.register (fun ims ->
		let send_key k =
			Input_method_service.send_key_char ims k
		in
	object

		method onInitializeInterface = ()
		method onBindInput = ()
		method onCreateInputView = keyboard_view ims send_key
		method onCreateCandidatesView = Java.null
		method onStartInput _ _ = ()

	end)
