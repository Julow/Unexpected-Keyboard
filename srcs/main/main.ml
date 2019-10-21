open Android_inputmethodservice
open Android_view

external _hack : unit -> unit = "Java_juloo_javacaml_Caml_startup"

(** Key value to string *)
let render_key =
	let open Key in
	let render_event =
		function
		| Escape		-> "Esc"
		| Tab			-> "\xE2\x87\xA5"
		| Backspace		-> "\xE2\x8C\xAB"
		| Delete		-> "\xE2\x8C\xA6"
		| Enter			-> "\xE2\x8F\x8E"
		| Left			-> "\xE2\x86\x90"
		| Right			-> "\xE2\x86\x92"
		| Up			-> "\xE2\x86\x91"
		| Down			-> "\xE2\x86\x93"
		| Page_up		-> "\xE2\x87\x9E"
		| Page_down		-> "\xE2\x87\x9F"
		| Home			-> "\xE2\x86\x96"
		| End			-> "\xE2\x86\x98"
	and render_modifier =
		function
		| Shift				-> "\xE2\x87\xA7"
		| Ctrl				-> "ctrl"
		| Alt				-> "alt"
		| Accent Acute		-> "\xCC\x81"
		| Accent Grave		-> "\xCC\x80"
		| Accent Circumflex	-> "\xCC\x82"
		| Accent Tilde		-> "\xCC\x83"
		| Accent Cedilla	-> "\xCC\xA7"
		| Accent Trema		-> "\xCC\x88"
	in
	function
	| Typing (Char (c, _))		->
		(* TODO: OCaml and Java are useless at unicode *)
		Java.to_string (Utils.java_string_of_code_point c)
	| Typing (Event (ev, _))	-> render_event ev
	| Modifier m				-> render_modifier m
	| Nothing					-> ""
	| Change_pad Default		-> "ABC"
	| Change_pad Numeric		-> "123"

type t = {
	touch_state		: Touch_event.state;
	layout			: Key.t KeyboardLayout.t;
	modifiers		: Modifiers.t;
	ims				: Input_method_service.t;
	view			: View.t;
  key_repeat : Key_repeat.t;
	dp				: float -> float;
}

let create ~ims ~view ~dp () = {
	touch_state = Touch_event.empty_state;
	layout = Layouts.qwerty;
	modifiers = Modifiers.empty;
	key_repeat = Key_repeat.empty;
	ims; view; dp
}

let key_repeat_timeout = function
  | Some t ->
      let t = Int64.of_int t in
      [ `Timeout (t, `Key_repeat_timeout) ]
  | None -> []

let handle_key_repeat_timeout t =
  let key_repeat, repeats, timeout =
    Key_repeat.on_timeout t.key_repeat
  in
  let sends = List.map (fun tv -> `Send tv) repeats in
  { t with key_repeat }, sends @ key_repeat_timeout timeout

let handle_down t = function
	| Key.Modifier m	->
		{ t with modifiers = Modifiers.on_down m t.modifiers }, []
	| Typing tv			->
      let key_repeat, timeout = Key_repeat.on_down t.key_repeat tv in
      { t with key_repeat }, key_repeat_timeout timeout
	| Change_pad pad	->
		let layout = match pad with
			| Default	-> Layouts.qwerty
			| Numeric	-> Layouts.numeric
		in
		{ t with layout;
			modifiers = Modifiers.empty;
			touch_state = Touch_event.empty_state }, []
	| _					-> t, []

let handle_cancel t = function
	| Key.Modifier m	->
		{ t with modifiers = Modifiers.on_cancel m t.modifiers }, []
	| Typing kv			->
      let key_repeat = Key_repeat.on_up t.key_repeat kv in
      { t with key_repeat }, []
	| _					-> t, []

let handle_up t = function
	| Key.Typing tv			->
		let tv = Modifiers.apply tv t.modifiers
		and modifiers = Modifiers.on_key_press t.modifiers
    and key_repeat = Key_repeat.on_up t.key_repeat tv in
    { t with modifiers; key_repeat }, [ `Send tv ]
	| Modifier m			->
		{ t with modifiers = Modifiers.on_up m t.modifiers }, []
	| _						-> t, []

let handle_touch_event ev t =
	let invalidate = `Invalidate t.view in
  match Touch_event.on_touch t.layout t.touch_state ev with
	| Key_down (key, ts)				->
		let t, tasks = handle_down t key.v in
		{ t with touch_state = ts }, invalidate :: tasks
	| Key_up (_, v, ts)				->
		let t, tasks = handle_up t v in
		{ t with touch_state = ts }, invalidate :: tasks
	| Pointer_changed (_, v', v, ts)	->
		let t, tasks = handle_cancel t v' in
		let t, tasks' = handle_down t v in
		{ t with touch_state = ts }, invalidate :: (tasks @ tasks')
	| Cancelled (_, v, ts)			->
		let t, tasks = handle_cancel t v in
		{ t with touch_state = ts }, invalidate :: tasks
	| Ignore							-> t, []

let handle_touch_event ev t =
  let make_event kind ptr_index =
    let x = Motion_event.get_x ev ptr_index /. float (View.get_width t.view)
    and y = Motion_event.get_y ev ptr_index /. float (View.get_height t.view)
    and ptr_id = Motion_event.get_pointer_id ev ptr_index in
    Touch_event.{ kind; ptr_id; x; y }
  in
  let rec move_event t tasks i =
    let t, tasks' = handle_touch_event (make_event Move i) t in
    let tasks = tasks' @ tasks in
    if i < 0 then t, tasks else move_event t tasks (i - 1)
  in
  let get_ptr_idx = Motion_event.get_action_index in
  let action = Motion_event.get_action_masked ev in
  if action = motion_event_ACTION_MOVE
  then move_event t [] (Motion_event.get_pointer_count ev - 1)
  else if action = motion_event_ACTION_UP
    || action = motion_event_ACTION_POINTER_UP
  then handle_touch_event (make_event Up (get_ptr_idx ev)) t
  else if action = motion_event_ACTION_DOWN
    || action = motion_event_ACTION_POINTER_DOWN
  then handle_touch_event (make_event Down (get_ptr_idx ev)) t
  else if action = motion_event_ACTION_CANCEL
  then handle_touch_event (make_event Cancel (get_ptr_idx ev)) t
  else t, []

let do_update ev t =
  match ev with
  | `Touch_event ev -> handle_touch_event ev t
  | `Key_repeat_timeout -> handle_key_repeat_timeout t

let task t = Keyboard_service.Task t

let view_invalidate view =
	task (fun () -> View.invalidate view; Lwt.return (fun t -> t, []))

let send tv =
	let send t =
		begin match tv with
			| Key.Char (c, 0)	-> Keyboard_service.send_char t.ims c
			| Char (c, meta)	-> Keyboard_service.send_char_meta t.ims c meta
			| Event (ev, meta)	-> Keyboard_service.send_event t.ims ev meta
		end;
		t, []
	in
	task (fun () -> Lwt.return send)

let rec timeout t ev =
  task (fun () ->
    Keyboard_service.timeout t
    |> Lwt.map (fun () -> update ev)
  )

and update ev t =
  let t, tasks = do_update ev t in
  let tasks =
    List.map (function
      | `Send tv -> send tv
      | `Invalidate view -> view_invalidate view
      | `Timeout (t, ev) -> timeout t ev)
      tasks
  in
  t, tasks

let draw t canvas =
	let is_activated key =
		match Touch_event.key_activated t.touch_state key with
		| exception Not_found	-> false
		| _						-> true
	and render_key k =
		let k = match k with
			| Key.Typing tv	-> Key.Typing (Modifiers.apply tv t.modifiers)
			| k				-> k
		in
		render_key k
	in
	Drawing.keyboard is_activated t.dp render_key t.layout canvas

let () =
	Printexc.record_backtrace true;
	Android_enable_logging.enable "UNEXPECTED KEYBOARD";
	let service = Keyboard_service.keyboard_service create update draw in
	UnexpectedKeyboardService.register service
