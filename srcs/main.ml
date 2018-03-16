open Android_content
open Android_graphics
open Android_inputmethodservice
open Android_util
open Android_view

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
		let open Key_event in
		match evt with
		| `Escape		-> get'keycode_escape ()
		| `Tab			-> get'keycode_tab ()
		| `Backspace	-> get'keycode_del ()
		| `Delete		-> get'keycode_forward_del ()
		| `Enter		-> get'keycode_enter ()
		| `Left			-> get'keycode_dpad_left ()
		| `Right		-> get'keycode_dpad_right ()
		| `Up			-> get'keycode_dpad_up ()
		| `Down			-> get'keycode_dpad_down ()
		| `Page_up		-> get'keycode_page_up ()
		| `Page_down	-> get'keycode_page_down ()
		| `Home			-> get'keycode_home ()
		| `End			-> get'keycode_move_end ()
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

(** Key_value to string *)
let render_key =
	let open Key_value in
	function
	| Char c				->
		(* TODO: OCaml and Java are useless at unicode *)
		Java.to_string (java_string_of_code_point c)
	| Event (`Escape, _)	-> "Esc"
	| Event (`Tab, _)		-> "\xE2\x87\xA5"
	| Event (`Backspace, _)	-> "\xE2\x8C\xAB"
	| Event (`Delete, _)	-> "\xE2\x8C\xA6"
	| Event (`Enter, _)		-> "\xE2\x8F\x8E"
	| Event (`Left, _)		-> "\xE2\x86\x90"
	| Event (`Right, _)		-> "\xE2\x86\x92"
	| Event (`Up, _)		-> "\xE2\x86\x91"
	| Event (`Down, _)		-> "\xE2\x86\x93"
	| Event (`Page_up, _)	-> "\xE2\x87\x9E"
	| Event (`Page_down, _)	-> "\xE2\x87\x9F"
	| Event (`Home, _)		-> "\xE2\x86\x96"
	| Event (`End, _)		-> "\xE2\x86\x98"
	| Shift					-> "\xE2\x87\xA7"
	| Accent `Acute			-> "\xCC\x81"
	| Accent `Grave			-> "\xCC\x80"
	| Accent `Circumflex	-> "\xCC\x82"
	| Accent `Tilde			-> "\xCC\x83"
	| Accent `Cedilla		-> "\xCC\xA7"
	| Accent `Trema			-> "\xCC\x88"
	| Nothing				-> ""
	| Change_pad `Default	-> "ABC"
	| Change_pad `Numeric	-> "123"

let keyboard_service ims =
	let open Key_value in

	let layout = ref Layouts.qwerty
	and modifiers = ref Modifiers.Stack.empty in

	let set_mods mods key modifier =
		modifiers := Modifiers.Stack.add_or_cancel key modifier mods
	in

	let send_key k =
		let open Key_value in
		let mods = !modifiers in
		modifiers := Modifiers.Stack.empty;
		match Modifiers.Stack.apply mods k with
		| Char c				-> send_char ims c
		| Event (evt, meta)		-> send_event ims evt meta
		| Shift as kv			-> set_mods mods kv Modifiers.shift
		| Accent acc as kv		-> set_mods mods kv (Modifiers.accent acc)
		| Nothing				-> ()
		| Change_pad `Default	-> layout := Layouts.qwerty
		| Change_pad `Numeric	-> layout := Layouts.numeric
	in

	let render_key k = render_key (Modifiers.Stack.apply !modifiers k) in

	let keyboard_view view =
		let dp =
			let density = Context.get_resources ims
				|> Resources.get_display_metrics
				|> Display_metrics.get'scaled_density in
			fun x -> x *. density
		in
		let invalidate () = View.invalidate view in
		let on_touch, is_activated =
			Touch_event.on_touch invalidate send_key view in
		let on_draw = Drawing.keyboard is_activated dp render_key in

		object
			method onTouchEvent ev =
				on_touch !layout ev;
				true

			method onMeasure w_spec _ =
				let w = Measure_spec.get_size w_spec
				and h = int_of_float (dp 200.) in
				View.set_measured_dimension view w h

			method onDraw canvas = on_draw !layout canvas
			method onDetachedFromWindow = ()
		end
	in

	object

		method onInitializeInterface = ()
		method onBindInput = ()
		method onCreateInputView = CustomView.create ims keyboard_view
		method onCreateCandidatesView = Java.null
		method onStartInput _ _ = ()

	end

let () = UnexpectedKeyboardService.register keyboard_service
