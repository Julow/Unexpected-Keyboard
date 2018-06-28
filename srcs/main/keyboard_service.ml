open Android_content
open Android_util
open Android_inputmethodservice
open Android_view

(** Sends a single character *)
let send_char ims cp =
	let ic = Input_method_service.get_current_input_connection ims in
	let txt = Utils.java_string_of_code_point cp in
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

(** InputMethodService related stuff
	defines the main loop of the app
	The logic is in the [Main] module *)
let keyboard_service create update draw ims =
	let dp =
		let density = Context.get_resources ims
			|> Resources.get_display_metrics
			|> Display_metrics.get'scaled_density in
		fun x -> x *. density
	in

	let keyboard_view view =
		let acc = ref (create ~view ~dp ()) in

		let update ev =
			let acc', actions = update !acc ev in
			acc := acc';
			List.iter (function
				| `Send_char c				-> send_char ims c
				| `Send_event (ev, meta)	-> send_event ims ev meta
				| `Invalidate				-> View.invalidate view)
				actions
		in

		object
			method onTouchEvent ev =
				update (`Touch_event ev);
				true

			method onMeasure w_spec _ =
				let w = Measure_spec.get_size w_spec
				and h = int_of_float (dp 200.) in
				View.set_measured_dimension view w h

			method onDraw canvas = draw !acc canvas
			method onDetachedFromWindow = ()
		end
	in

	object
		val view = lazy (CustomView.create ims keyboard_view)
		method onInitializeInterface = ()
		method onBindInput = ()
		method onCreateInputView = Lazy.force view
		method onCreateCandidatesView = Java.null
		method onStartInput _ _ = ()
	end
