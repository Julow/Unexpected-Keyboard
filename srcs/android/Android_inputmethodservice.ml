class%java input_connection "android.view.inputmethod.InputConnection" =
object
	method send_key_event : Android_view.key_event -> bool = "sendKeyEvent"
end

class%java input_method_service "android.inputmethodservice.InputMethodService" =
object
	inherit Android_content.context
	method get_current_input_connection : input_connection = "getCurrentInputConnection"
	method send_down_up_key_events : int -> unit = "sendDownUpKeyEvents"
	method send_key_char : char -> unit = "sendKeyChar"
end
