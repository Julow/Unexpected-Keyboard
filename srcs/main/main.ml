open Android_utils

external _hack : unit -> unit = "Java_juloo_javacaml_Caml_startup"

let keyboard_service ims =
  let view = lazy (CustomView.create ims (Keyboard_view.create ~ims)) in
	object
		method onInitializeInterface = ()
		method onBindInput = ()
		method onCreateInputView = Lazy.force view
		method onCreateCandidatesView = Java.null
		method onStartInput _ _ = ()
	end

let () =
	Printexc.record_backtrace true;
	Android_enable_logging.enable "UNEXPECTED KEYBOARD";
	UnexpectedKeyboardService.register keyboard_service
