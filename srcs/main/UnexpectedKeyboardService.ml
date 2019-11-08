open Android_api.Inputmethodservice
open Android_api.View_inputmethod
open Android_api.View

type t = <
	onInitializeInterface : unit;
	onBindInput : unit;
	onCreateInputView : View.t;
	onCreateCandidatesView : View.t;
	onStartInput : Editor_info.t -> bool -> unit
>

let register (f : Input_method_service.t -> t) =
	Callback.register "unexpectedKeyboardService" f
