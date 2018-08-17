open Android_content
open Android_inputmethodservice
open Android_os
open Android_util
open Android_view

(** Send a character through the current input connection *)
let send_char ims cp =
	let ic = Input_method_service.get_current_input_connection ims in
	let txt = Utils.java_string_of_code_point cp in
	ignore (Input_connection.commit_text ic txt 1)

let key_char_map = lazy Key_character_map.(load (get'virtual_keyboard ()))

(** Send a character as 2 key events (down and up)
	Allows setting the meta field *)
let send_char_meta ims cp meta =
	match Char.chr cp with
	| exception _	-> ()
	| c				->
		let events =
			let chars = Jarray.create_char 1 in
			Jarray.set_char chars 0 c;
			Key_character_map.get_events (Lazy.force key_char_map) chars
		in
		let ic = Input_method_service.get_current_input_connection ims in
		for i = 0 to Jarray.length events - 1 do
			let ev = Jarray.get_object events i in
			let action = Key_event.get_action ev
			and code = Key_event.get_key_code ev in
			let ev = Key_event.create' 1L 1L action code 1 meta in
			ignore (Input_connection.send_key_event ic ev)
		done

(** Likes `Input_method_service.send_down_up_key_events`
		with an extra `meta` parameter *)
let send_event ims evt meta =
	let code =
		let open Key_event in
		match evt with
		| Key.Escape	-> get'keycode_escape ()
		| Tab			-> get'keycode_tab ()
		| Backspace		-> get'keycode_del ()
		| Delete		-> get'keycode_forward_del ()
		| Enter			-> get'keycode_enter ()
		| Left			-> get'keycode_dpad_left ()
		| Right			-> get'keycode_dpad_right ()
		| Up			-> get'keycode_dpad_up ()
		| Down			-> get'keycode_dpad_down ()
		| Page_up		-> get'keycode_page_up ()
		| Page_down		-> get'keycode_page_down ()
		| Home			-> get'keycode_home ()
		| End			-> get'keycode_move_end ()
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

type ('a, 'b) task = Task of (unit -> ('a -> ('a, 'b) loop) Lwt.t)
and ('a, 'b) loop = 'b * ('a, 'b) task list

let loop data =
	let pull, push =
		let stream, push = Lwt_stream.create () in
		let pull () = Lwt_stream.next stream in
		let push e = push (Some e) in
		pull, push
	in
	let pull_task (Task task) = Lwt.on_success (task ()) push in
	let rec loop t =
		let%lwt f = pull () in
		let t, tasks = f t in
		List.iter pull_task tasks;
		loop t
	in
	Lwt.async (fun () -> loop data);
	push

let handler = lazy (Handler.create ())

let timeout msec =
	let t, u = Lwt.task () in
	let callback = Jrunnable.create (Lwt.wakeup u) in
	ignore (Handler.post_delayed (Lazy.force handler) callback msec);
	t

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
		let push = loop (create ~ims ~view ~dp ()) in

		object
			method onTouchEvent ev =
				push (update (`Touch_event ev));
				true

			method onMeasure w_spec _ =
				let w = Measure_spec.get_size w_spec
				and h = int_of_float (dp 200.) in
				View.set_measured_dimension view w h

			method onDraw canvas = push (fun t -> draw t canvas; t, [])
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
