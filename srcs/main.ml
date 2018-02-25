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

let layout = KeyboardLayout.Desc.(
	let key ?(width=1.) ?a ?b ?c ?d v = key ~width Key.{ a; b; c; d; v } in
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

	row ~height:0.9 ~margin:1. [
		key ~width:1.5 '.';
		key ~width:6.0 ' ';
		key ~width:1.5 '^';
	]
])

let keyboard_view context send_key =
	CustomView.create context (fun view ->
		let dp =
			let density = Context.get_resources context
				|> Resources.get_display_metrics
				|> Display_metrics.get'scaled_density in
			fun x -> x *. density
		in
		let on_touch = Touch_event.on_touch send_key layout view in
		let on_draw = Drawing.keyboard dp layout in
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
