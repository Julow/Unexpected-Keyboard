open Android_content
open Android_os
open Android_util
open Android_view

external _hack : unit -> unit = "Java_juloo_javacaml_Caml_startup"

(** Key value to string *)
let render_key =
	let open Keyboard.Key in
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
		Java.to_string (Java_utils.string_of_code_point c)
	| Typing (Event (ev, _))	-> render_event ev
	| Modifier m				-> render_modifier m
	| Nothing					-> ""
	| Change_pad Default		-> "ABC"
	| Change_pad Numeric		-> "123"

let handler = lazy (Handler.create ())

let timeout f msec =
	let callback = Jrunnable.create f in
	ignore (Handler.post_delayed (Lazy.force handler) callback msec)

let handle_touch_event view ev t =
  let make_event kind ptr_index =
    let x = Motion_event.get_x ev ptr_index /. float (View.get_width view)
    and y = Motion_event.get_y ev ptr_index /. float (View.get_height view)
    and ptr_id = Motion_event.get_pointer_id ev ptr_index in
    Keyboard.Touch_event.{ kind; ptr_id; x; y }
  in
  let rec move_event t tasks i =
    let t, tasks' = Keyboard.State.handle_touch_event (make_event Move i) t in
    let tasks = tasks' @ tasks in
    if i <= 0 then t, tasks else move_event t tasks (i - 1)
  in
  let get_ptr_idx = Motion_event.get_action_index in
  let action = Motion_event.get_action_masked ev in
  if action = motion_event_ACTION_MOVE
  then move_event t [] (Motion_event.get_pointer_count ev - 1)
  else if action = motion_event_ACTION_UP
    || action = motion_event_ACTION_POINTER_UP
  then Keyboard.State.handle_touch_event (make_event Up (get_ptr_idx ev)) t
  else if action = motion_event_ACTION_DOWN
    || action = motion_event_ACTION_POINTER_DOWN
  then Keyboard.State.handle_touch_event (make_event Down (get_ptr_idx ev)) t
  else if action = motion_event_ACTION_CANCEL
  then Keyboard.State.handle_touch_event (make_event Cancel (get_ptr_idx ev)) t
  else t, []

let do_update ~view ev t =
  match ev with
  | `Touch_event ev -> handle_touch_event view ev t
  | `Key_repeat_timeout -> Keyboard.State.handle_key_repeat_timeout t

let send ims tv =
  let open Keyboard.Key in
  match tv with
  | Char (c, 0)	-> Keyboard_service.send_char ims c
  | Char (c, meta)	-> Keyboard_service.send_char_meta ims c meta
  | Event (ev, meta)	-> Keyboard_service.send_event ims ev meta

let handle_task ~ims ~view handle = function
  | `Send tv -> send ims tv
  | `Invalidate -> View.invalidate view
  | `Timeout (tm, ev) -> timeout (fun () -> handle ev) tm

let keyboard_view ~ims view =
	let dp =
		let density = Context.get_resources ims
			|> Resources.get_display_metrics
			|> Display_metrics.get'scaled_density in
		fun x -> x *. density
	in
  let t = ref Keyboard.(State.create Layouts.qwerty) in
  let rec handle ev =
    let t', tasks = do_update ~view ev !t in
    List.iter (handle_task ~ims ~view handle) tasks;
    t := t'
  in
  let draw canvas =
    Drawing.keyboard dp render_key !t canvas
  in
  object
    method onTouchEvent ev =
      handle (`Touch_event ev);
      true

    method onMeasure w_spec _ =
      let w = Measure_spec.get_size w_spec
      and h = int_of_float (dp 200.) in
      View.set_measured_dimension view w h

    method onDraw = draw
    method onDetachedFromWindow = ()
  end

let () =
	Printexc.record_backtrace true;
	Android_enable_logging.enable "UNEXPECTED KEYBOARD";
	UnexpectedKeyboardService.register @@
  Keyboard_service.create ~input_view:keyboard_view
