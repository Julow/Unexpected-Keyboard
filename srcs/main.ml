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

type event_type = [ `Escape ]

type key =
	| Char of char
	| Event of event_type * int
	| Shift
	| Nothing

let layout = KeyboardLayout.Desc.(
	let key ?(width=1.) ?a ?b ?c ?d v = key ~width Key.{ a; b; c; d; v } in
	let c c = Char c
	and e evt = Event (evt, 0) in
build [
	row [
		key (c 'q') ~b:(c '1') ~d:(c '~') ~a:(e `Escape) ~c:(c '!');
		key (c 'w') ~b:(c '2') ~d:(c '@');
		key (c 'e') ~b:(c '3') ~d:(c '#');
		key (c 'r') ~b:(c '4') ~d:(c '$');
		key (c 't') ~b:(c '5') ~d:(c '%');
		key (c 'y') ~b:(c '6') ~d:(c '^');
		key (c 'u') ~b:(c '7') ~d:(c '&');
		key (c 'i') ~b:(c '8') ~d:(c '*');
		key (c 'o') ~b:(c '9') ~d:(c '(') ~c:(c ')');
		key (c 'p') ~b:(c '0')
	];

	row ~margin:0.5 [
		key (c 'a');
		key (c 's');
		key (c 'd');
		key (c 'f');
		key (c 'g');
		key (c 'h');
		key (c 'j');
		key (c 'k');
		key (c 'l')
	];

	row [
		key ~width:1.5 Shift;
		key (c 'z');
		key (c 'x');
		key (c 'c');
		key (c 'v');
		key (c 'b');
		key (c 'n');
		key (c 'm');
		key ~width:1.5 Nothing
	];

	row ~height:0.9 ~margin:1. [
		key ~width:1.5 (c '.');
		key ~width:6.0 (c ' ');
		key ~width:1.5 Nothing
	]
])

let keyboard_view context render_key send_key =
	CustomView.create context (fun view ->
		let dp =
			let density = Context.get_resources context
				|> Resources.get_display_metrics
				|> Display_metrics.get'scaled_density in
			fun x -> x *. density
		in
		let on_touch = Touch_event.on_touch send_key layout view
		and on_draw = Drawing.keyboard dp render_key layout in
	object

		method onTouchEvent ev =
			on_touch ev;
			true

		method onMeasure w_spec _ =
			let w = Measure_spec.get_size w_spec
			and h = int_of_float (dp 200.) in
			View.set_measured_dimension view w h

		method onDraw canvas = on_draw canvas

		method onDetachedFromWindow = ()

	end)

let () = Printexc.record_backtrace true

module Modifier =
struct

	let default k = k

	let shift =
		let meta_shift = Key_event.get'meta_shift_left_on in
		function
		| Char c			-> Char (Char.uppercase_ascii c)
		| Event (evt, meta)	-> Event (evt, meta lor meta_shift ())
		| Shift
		| Nothing			-> Nothing

end

let event_code =
	let open Key_event in
	function
	| `Escape	-> get'keycode_escape ()

let event_label =
	function
	| `Escape	-> "esc"

let send_char = Input_method_service.send_key_char

(** Likes `Input_method_service.send_down_up_key_events`
		with an extra `meta` parameter *)
let send_event ims evt meta =
	let code = event_code evt
	and time = Android_os.System_clock.uptime_millis () in
	let mk_event action =
		let flags = Key_event.(get'flag_keep_touch_mode ()
				lor get'flag_soft_keyboard ()) in
		Key_event.create time time action code 0 meta ~-1 0 flags
	in
	let ic = Input_method_service.get_current_input_connection ims in
	let send = Input_connection.send_key_event ic in
	ignore (send (mk_event (Key_event.get'action_down ()))
		&& send (mk_event (Key_event.get'action_up ())))

let () =
	UnexpectedKeyboardService.register (fun ims ->
		let modifier = ref Modifier.default in

		let set_modifier_once m =
			modifier := (fun k ->
				let k = m k in
				modifier := Modifier.default;
				k)
		in

		let send_key k =
			match !modifier k with
			| Char c			-> send_char ims c
			| Event (evt, meta)	-> send_event ims evt meta
			| Shift				-> set_modifier_once Modifier.shift
			| Nothing			-> ()

		and render_key k =
			match !modifier k with
			| Char c			-> String.make 1 c
			| Event (evt, _)	-> event_label evt
			| Shift				-> "|^|"
			| Nothing			-> ""
		in

	object

		method onInitializeInterface = ()
		method onBindInput = ()
		method onCreateInputView = keyboard_view ims render_key send_key
		method onCreateCandidatesView = Java.null
		method onStartInput _ _ = ()

	end)
