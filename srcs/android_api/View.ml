class%java measure_spec "android.view.View$MeasureSpec" =
object
	method [@static] get_size : int -> int = "getSize"
end

let motion_event_ACTION_DOWN = 0x00000000
let motion_event_ACTION_UP = 0x00000001
let motion_event_ACTION_MOVE = 0x00000002
let motion_event_ACTION_CANCEL = 0x00000003
let motion_event_ACTION_POINTER_DOWN = 0x00000005
let motion_event_ACTION_POINTER_UP = 0x00000006

class%java motion_event "android.view.MotionEvent" =
object
	method get_action_masked : int = "getActionMasked"
	method get_action_index : int = "getActionIndex"

	method get_pointer_count : int = "getPointerCount"
	method get_pointer_id : int -> int = "getPointerId"

	method get_x : int -> float = "getX"
	method get_y : int -> float = "getY"
	method get_x_first : float = "getX"
	method get_y_first : float = "getY"
end

class%java view "android.view.View" =
object
	method get_context : Content.context = "getContext"
	method get_width : int = "getWidth"
	method get_height : int = "getHeight"
	method set_measured_dimension : int -> int -> unit = "setMeasuredDimension"
	method invalidate : unit = "invalidate"
end

class%java key_event "android.view.KeyEvent" =
object
	initializer (create' : long -> long -> int -> int -> int -> int -> _)
	initializer (create : long -> long -> int -> int -> int -> int -> int -> int -> int -> _)

	method get_action : int = "getAction"
	method get_key_code : int = "getKeyCode"

	val [@static] action_down : int = "ACTION_DOWN"
	val [@static] action_up : int = "ACTION_UP"
	val [@static] flag_keep_touch_mode : int = "FLAG_KEEP_TOUCH_MODE"
	val [@static] flag_soft_keyboard : int = "FLAG_SOFT_KEYBOARD"

	val [@static] meta_shift_left_on : int = "META_SHIFT_LEFT_ON"
	val [@static] meta_shift_on : int = "META_SHIFT_ON"
	val [@static] meta_ctrl_left_on : int = "META_CTRL_LEFT_ON"
	val [@static] meta_ctrl_on : int = "META_CTRL_ON"
	val [@static] meta_alt_left_on : int = "META_ALT_LEFT_ON"
	val [@static] meta_alt_on : int = "META_ALT_ON"

	method [@static] change_action : key_event -> int -> key_event = "changeAction"

	val [@static] keycode_escape : int = "KEYCODE_ESCAPE"
	val [@static] keycode_tab : int = "KEYCODE_TAB"
	val [@static] keycode_del : int = "KEYCODE_DEL"
	val [@static] keycode_forward_del : int = "KEYCODE_FORWARD_DEL"
	val [@static] keycode_enter : int = "KEYCODE_ENTER"
	val [@static] keycode_dpad_left : int = "KEYCODE_DPAD_LEFT"
	val [@static] keycode_dpad_right : int = "KEYCODE_DPAD_RIGHT"
	val [@static] keycode_dpad_up : int = "KEYCODE_DPAD_UP"
	val [@static] keycode_dpad_down : int = "KEYCODE_DPAD_DOWN"
	val [@static] keycode_page_up : int = "KEYCODE_PAGE_UP"
	val [@static] keycode_page_down : int = "KEYCODE_PAGE_DOWN"
	val [@static] keycode_home : int = "KEYCODE_HOME"
	val [@static] keycode_move_end : int = "KEYCODE_MOVE_END"
end

class%java key_character_map "android.view.KeyCharacterMap" =
object
	val [@static] virtual_keyboard : int = "VIRTUAL_KEYBOARD"
	method [@static] get_dead_char : int -> int -> int = "getDeadChar"
	method [@static] load : int -> key_character_map = "load"

	method get_events : char array -> key_event array = "getEvents"
end
