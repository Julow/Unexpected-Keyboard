class%java input_connection "android.view.inputmethod.InputConnection" =
object
	method send_key_event : View.key_event -> bool = "sendKeyEvent"
	method commit_text : Java_api.Lang.char_sequence -> int -> bool = "commitText"
end

class%java input_method_service "android.inputmethodservice.InputMethodService" =
object
	inherit Content.context
	method get_current_input_connection : input_connection = "getCurrentInputConnection"
	method send_down_up_key_events : int -> unit = "sendDownUpKeyEvents"
	method send_key_char : char -> unit = "sendKeyChar"
end
