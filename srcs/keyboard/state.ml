(** Keyboard state *)
type t = {
	touch_state		: Touch_event.state;
	layout			: Key.t Layout.t;
	modifiers		: Modifiers.t;
  key_repeat : Key_repeat.t;
}

let create layout = {
	touch_state = Touch_event.empty_state;
  layout;
	modifiers = Modifiers.empty;
	key_repeat = Key_repeat.empty;
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
  let mods = Modifiers.active_modifiers t.modifiers in
  let sends = List.map (fun tv -> `Send (tv, mods)) repeats in
  { t with key_repeat }, sends @ key_repeat_timeout timeout

let handle_down t = function
	| Key.Modifier m	->
		{ t with modifiers = Modifiers.on_down m t.modifiers }, []
	| Typing tv			->
      let key_repeat, timeout = Key_repeat.on_down t.key_repeat tv in
      { t with key_repeat }, key_repeat_timeout timeout
	| Change_pad pad	->
    t, [ `Change_pad pad ]
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
    let modifiers = Modifiers.on_key_press t.modifiers
    and key_repeat = Key_repeat.on_up t.key_repeat tv in
    let mods = Modifiers.active_modifiers t.modifiers in
    { t with modifiers; key_repeat }, [ `Send (tv, mods) ]
	| Modifier m			->
		{ t with modifiers = Modifiers.on_up m t.modifiers }, []
	| _						-> t, []

let handle_touch_event ev t =
  match Touch_event.on_touch t.layout t.touch_state ev with
	| Key_down (key, ts)				->
		let t, tasks = handle_down t key.v in
		{ t with touch_state = ts }, `Invalidate :: tasks
	| Key_up (_, v, ts)				->
		let t, tasks = handle_up t v in
		{ t with touch_state = ts }, `Invalidate :: tasks
	| Pointer_changed (_, v', v, ts)	->
		let t, tasks = handle_cancel t v' in
		let t, tasks' = handle_down t v in
		{ t with touch_state = ts }, `Invalidate :: (tasks @ tasks')
	| Cancelled (_, v, ts)			->
		let t, tasks = handle_cancel t v in
		{ t with touch_state = ts }, `Invalidate :: tasks
	| Ignore							-> t, []

let key_activated t key =
  match Touch_event.key_activated t.touch_state key with
  | exception Not_found	-> false
  | _						-> true

let set_layout t layout =
  { t with layout;
    modifiers = Modifiers.empty;
    touch_state = Touch_event.empty_state }, []
