open Android_content
open Android_graphics
open Android_inputmethodservice
open Android_util
open Android_view

external _hack : unit -> unit = "Java_juloo_javacaml_Caml_startup"

(** Key value to string *)
let render_key =
	let open Key in
	function
	| Char c				->
		(* TODO: OCaml and Java are useless at unicode *)
		Java.to_string (Utils.java_string_of_code_point c)
	| Event (Escape, _)		-> "Esc"
	| Event (Tab, _)		-> "\xE2\x87\xA5"
	| Event (Backspace, _)	-> "\xE2\x8C\xAB"
	| Event (Delete, _)		-> "\xE2\x8C\xA6"
	| Event (Enter, _)		-> "\xE2\x8F\x8E"
	| Event (Left, _)		-> "\xE2\x86\x90"
	| Event (Right, _)		-> "\xE2\x86\x92"
	| Event (Up, _)			-> "\xE2\x86\x91"
	| Event (Down, _)		-> "\xE2\x86\x93"
	| Event (Page_up, _)	-> "\xE2\x87\x9E"
	| Event (Page_down, _)	-> "\xE2\x87\x9F"
	| Event (Home, _)		-> "\xE2\x86\x96"
	| Event (End, _)		-> "\xE2\x86\x98"
	| Shift					-> "\xE2\x87\xA7"
	| Accent Acute			-> "\xCC\x81"
	| Accent Grave			-> "\xCC\x80"
	| Accent Circumflex		-> "\xCC\x82"
	| Accent Tilde			-> "\xCC\x83"
	| Accent Cedilla		-> "\xCC\xA7"
	| Accent Trema			-> "\xCC\x88"
	| Nothing				-> ""
	| Change_pad Default	-> "ABC"
	| Change_pad Numeric	-> "123"

type t = {
	touch_state		: Touch_event.state;
	layout			: Key.t KeyboardLayout.t;
	modifiers		: Modifiers.Stack.t;
	ims				: Input_method_service.t;
	view			: View.t;
	dp				: float -> float;
	cancel_repeat	: (unit -> unit) option
}

let task t = Keyboard_service.Task t

let view_invalidate view =
	task (fun () -> View.invalidate view; Lwt.return (fun t -> t, []))

let create ~ims ~view ~dp () = {
	touch_state = Touch_event.empty_state;
	layout = Layouts.qwerty;
	modifiers = Modifiers.Stack.empty;
	cancel_repeat = None;
	ims; view; dp
}

let handle_key t key =
	let clear_mods t = { t with modifiers = Modifiers.Stack.empty }
	and with_mods t m kv =
		let modifiers = Modifiers.Stack.add kv m t.modifiers in
		{ t with modifiers }, []
	and with_layout t layout =
		let touch_state = Touch_event.empty_state
		and modifiers = Modifiers.Stack.empty in
		{ t with touch_state; modifiers; layout }, []
	in
	match Modifiers.Stack.apply t.modifiers key with
	| Key.Char c			->
		clear_mods t, [ Keyboard_service.send_char t.ims c ]
	| Event (ev, meta)		->
		clear_mods t, [ Keyboard_service.send_event t.ims ev meta ]
	| Shift as kv			-> with_mods t Modifiers.shift kv
	| Accent acc as kv		-> with_mods t (Modifiers.accent acc) kv
	| Nothing				-> t, []
	| Change_pad Default	-> with_layout t Layouts.qwerty
	| Change_pad Numeric	-> with_layout t Layouts.numeric

let rec key_repeat ?(timeout=1000L) key =
	let later () t =
		match Touch_event.activated t.touch_state key with
		| None			-> t, []
		| Some value	->
			let task, cancel = key_repeat ~timeout:200L key in
			let t, tasks = handle_key t value in
			{ t with cancel_repeat = Some cancel }, task :: tasks
	in
	let timeout	= Keyboard_service.timeout timeout in
	let t () = Lwt.map later timeout
	and cancel () = Lwt.cancel timeout in
	task t, cancel

let update (`Touch_event ev) t =
	let invalidate = view_invalidate t.view in
	let key_repeat t touch_state key =
		match t.cancel_repeat with
		| None		->
			let task, cancel = key_repeat key in
			{ t with touch_state; cancel_repeat = Some cancel }, [ task ]
		| Some _	-> { t with touch_state }, []
	in
	let cancel_repeat t touch_state =
		match t.cancel_repeat with
		| Some cancel	-> cancel (); { t with touch_state; cancel_repeat = None }
		| None			-> { t with touch_state }
	in
	match Touch_event.on_touch t.view t.layout t.touch_state ev with
	| `Key_up (key, ts)		->
		let t, tasks = handle_key (cancel_repeat t ts) key in
		t, invalidate :: tasks
	| `Key_down (key, ts)	->
		let t, rt = key_repeat t ts key in
		t, invalidate :: rt
	| `Key_cancelled ts		-> cancel_repeat t ts, [ invalidate ]
	| `Moved touch_state	-> { t with touch_state }, []
	| `No_change			-> t, []

let draw t canvas =
	let is_activated = Touch_event.is_activated t.touch_state
	and render_key k = render_key (Modifiers.Stack.apply t.modifiers k) in
	Drawing.keyboard is_activated t.dp render_key t.layout canvas

let () =
	Printexc.record_backtrace true;
	Android_enable_logging.enable "UNEXPECTED KEYBOARD";
	let service = Keyboard_service.keyboard_service create update draw in
	UnexpectedKeyboardService.register service
