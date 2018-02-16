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

	method get_x : int -> float = "getX"
	method get_y : int -> float = "getY"
	method get_x_first : float = "getX"
	method get_y_first : float = "getY"
end

class%java view "android.view.View" =
object
	method get_context : Android_content.context = "getContext"
	method get_width : int = "getWidth"
	method get_height : int = "getHeight"
	method set_measured_dimension : int -> int -> unit = "setMeasuredDimension"
end
