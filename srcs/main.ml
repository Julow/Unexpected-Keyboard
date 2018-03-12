open Android_content
open Android_graphics
open Android_inputmethodservice
open Android_util
open Android_view

let keyboard_view context render_key send_key =
	CustomView.create context (fun view ->
		let dp =
			let density = Context.get_resources context
				|> Resources.get_display_metrics
				|> Display_metrics.get'scaled_density in
			fun x -> x *. density
		in
		let layout = Layouts.qwerty in
		let on_touch = Touch_event.on_touch send_key layout view
		and on_draw = Drawing.keyboard dp render_key layout in
	object

		method onTouchEvent ev =
			on_touch ev;
			View.invalidate view;
			true

		method onMeasure w_spec _ =
			let w = Measure_spec.get_size w_spec
			and h = int_of_float (dp 200.) in
			View.set_measured_dimension view w h

		method onDraw canvas = on_draw canvas

		method onDetachedFromWindow = ()

	end)

(** Create a String object representing a single character *)
let java_string_of_code_point =
	let code_points = Jarray.create_int 1 in
	fun cp ->
		Jarray.set_int code_points 0 cp;
	Java_lang.String.create_cps code_points 0 1

(** Sends a single character *)
let send_char =
	fun ims cp ->
		let ic = Input_method_service.get_current_input_connection ims in
		let txt = java_string_of_code_point cp in
		ignore (Input_connection.commit_text ic txt 1)

(** Likes `Input_method_service.send_down_up_key_events`
		with an extra `meta` parameter *)
let send_event ims evt meta =
	let code =
		match evt with
		| `Escape	-> Key_event.get'keycode_escape ()
	in
	let time = Android_os.System_clock.uptime_millis () in
	let mk_event action =
		let flags = Key_event.(get'flag_keep_touch_mode ()
				lor get'flag_soft_keyboard ()) in
		Key_event.create time time action code 0 meta ~-1 0 flags
	in
	let ic = Input_method_service.get_current_input_connection ims in
	let send = Input_connection.send_key_event ic in
	ignore (send (mk_event (Key_event.get'action_down ()))
		&& send (mk_event (Key_event.get'action_up ())))

let () = Printexc.record_backtrace true

module Modifier_stack =
struct

	(** Store activated modifiers
		Modifiers are associated to the key that activated them *)

	type modifier = Key_value.t * Modifiers.t
	type t = modifier list

	let empty = []

	(** Add a modifier on top of the stack *)
	let add key modifier t =
		(key, modifier) :: t

	(** Add a modifier like `add`
		Except if a modifier associated with the same key is already activated,
			it is simply removed, and no modifier is added *)
	let add_or_cancel key modifier t =
		if List.mem_assoc key t then
			List.remove_assoc key t
		else
			add key modifier t

	(** Apply the modifiers to the key
		Starting from the last added *)
	let apply t k =
		List.fold_left (fun k (_, m) -> m k) k t

end

let () =
	UnexpectedKeyboardService.register (fun ims ->
		let open Key_value in

		let modifiers = ref Modifier_stack.empty in
		let set_mods key modifier =
			modifiers := Modifier_stack.add_or_cancel key modifier mods
		in

		let send_key k =
			let mods = !modifiers in
			modifiers := Modifier_stack.empty;
			match Modifier_stack.apply mods k with
			| Char c			-> send_char ims c
			| Event (evt, meta)	-> send_event ims evt meta
			| Shift as kv		-> set_mods mods kv Modifiers.shift
			| Accent acc as kv	-> set_mods mods kv (Modifiers.accent acc)
			| Nothing			-> ()

		and render_key k =
			match Modifier_stack.apply !modifiers k with
			| Char c				->
				(* TODO: OCaml and Java are useless at unicode *)
				Java.to_string (java_string_of_code_point c)
			| Event (`Escape, _)	-> "esc"
			| Shift					-> "|^|"
			| Accent `Acute			-> "\xCC\x81"
			| Nothing				-> ""
		in

	object

		method onInitializeInterface = ()
		method onBindInput = ()
		method onCreateInputView = keyboard_view ims render_key send_key
		method onCreateCandidatesView = Java.null
		method onStartInput _ _ = ()

	end)
