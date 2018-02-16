class%java measure_spec "android.view.View$MeasureSpec" =
object
	method [@static] get_size : int -> int = "getSize"
end

class%java motion_event "android.view.MotionEvent" =
object
end

class%java view "android.view.View" =
object
	method get_context : Android_content.context = "getContext"
	method set_measured_dimension : int -> int -> unit = "setMeasuredDimension"
end
