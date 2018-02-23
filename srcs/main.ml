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

	row ~height:1.2 ~margin:1. [
		key ~width:1.5 '.';
		key ~width:6.0 ' ';
		key ~width:1.5 '^';
	]
])

let keyboard_view context send_key =
	CustomView.create context (fun view ->
		let res = Context.get_resources context in
		let dm = Resources.get_display_metrics res in
		let text_paint = Paint.create paint_ANTI_ALIAS_FLAG in
		Paint.set_text_align text_paint (Paint_align.get'center ());
		Paint.set_text_size text_paint
			(Display_metrics.get'scaled_density dm *. 16.);
		let on_touch = Touch_event.on_touch send_key layout view in
	object

		method onTouchEvent ev =
			on_touch ev;
			true

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
				let text = String.make 1 key.Key.v in
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
