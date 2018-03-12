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

type event = [ `Escape ]
type accent = [ `Acute ]

type key =
	| Char of int
	| Event of event * int
	| Shift
	| Accent of accent
	| Nothing

let layout = KeyboardLayout.Desc.(
	let key ?(width=1.) ?a ?b ?c ?d v = key ~width Key.{ a; b; c; d; v } in
	let c c = Char (Char.code c)
	and event evt = Event (evt, 0) in
build [
	row [
		key (c 'q') ~b:(c '1') ~d:(c '~') ~a:(event `Escape) ~c:(c '!');
		key (c 'w') ~b:(c '2') ~d:(c '@');
		key (c 'e') ~b:(c '3') ~d:(c '#') ~a:(Accent `Acute);
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
		| Char c			-> Char (Java_lang.Character.to_upper_case c)
		| Event (evt, meta)	-> Event (evt, meta lor meta_shift ())
		| Shift				-> Nothing
		| evt				-> evt

	let accent acc =
		let dead_char =
			match acc with
			| `Acute		-> 0x00B4
		in
		function
		| Char c as evt		->
			begin match Key_character_map.get_dead_char dead_char c with
				| 0		-> evt
				| c		-> Char c
			end
		| Accent acc' when acc' = acc	-> Nothing
		| evt				-> evt

end

let event_code =
	let open Key_event in
	function
	| `Escape	-> get'keycode_escape ()

let event_label =
	function
	| `Escape	-> "esc"

let accent_label =
	function
	| `Acute		-> "\xCC\x81"

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
			| Accent acc		-> set_modifier_once (Modifier.accent acc)
			| Nothing			-> ()

		and render_key k =
			match !modifier k with
			| Char c			->
				(* TODO: OCaml and Java are useless at unicode *)
				Java.to_string (java_string_of_code_point c)
			| Event (evt, _)	-> event_label evt
			| Shift				-> "|^|"
			| Accent acc		-> accent_label acc
			| Nothing			-> ""
		in

	object

		method onInitializeInterface = ()
		method onBindInput = ()
		method onCreateInputView = keyboard_view ims render_key send_key
		method onCreateCandidatesView = Java.null
		method onStartInput _ _ = ()

	end)
