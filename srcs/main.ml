open Android_graphics
open Android_view

let keyboard_view context =
	CustomView.create context (fun view ->
	object

		method onTouchEvent _ = false
		method onMeasure _ _ =
			View.set_measured_dimension view 200 200
		method onDraw canvas =
			let p = Paint.create_default () in
			Canvas.draw_text canvas "abc" 50. 50. p

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
