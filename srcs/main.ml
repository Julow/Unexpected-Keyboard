open Android_content
open Android_graphics
open Android_util
open Android_view


let layout = KeyboardLayout.Desc.(build [
	row [
		key 'q';
		key 'w';
		key 'e';
		key 'r';
		key 't';
		key 'y';
		key 'u';
		key 'i';
		key 'o';
		key 'p';
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

let keyboard_view context =
	CustomView.create context (fun view ->
		let context = View.get_context view in
		let res = Context.get_resources context in
		let dm = Resources.get_display_metrics res in
		let text_paint = Paint.create paint_ANTI_ALIAS_FLAG in
		Paint.set_text_align text_paint (Paint_align.get'center ());
		Paint.set_text_size text_paint
			(Display_metrics.get'scaled_density dm *. 16.);
	object

		method onTouchEvent _ = false
		method onMeasure w_spec _ =
			let w = Measure_spec.get_size w_spec in
			let h = 400 in
			View.set_measured_dimension view w h

		method onDraw canvas =
			let width = float (Canvas.get_width canvas)
			and height = float (Canvas.get_height canvas) in
			KeyboardLayout.iter (fun (pos, key) ->
				let x, y = KeyboardLayout.(
					width -. (pos.x +. pos.width /. 2.) *. width,
					height -. (pos.y +. pos.height /. 2.) *. height
				) in
				let text = String.make 1 key in
				Canvas.draw_text canvas text x y text_paint
			) layout

		method onDetachedFromWindow = ()

	end)

let () = Printexc.record_backtrace true

let () =
	UnexpectedKeyboardService.register (fun ims ->
	object

		method onInitializeInterface = ()
		method onBindInput = ()
		method onCreateInputView = keyboard_view ims
		method onCreateCandidatesView = Java.null
		method onStartInput _ _ = ()

	end)
