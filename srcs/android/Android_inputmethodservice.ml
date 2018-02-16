class%java input_method_service "android.inputmethodservice.InputMethodService" =
object
	inherit Android_content.context
	method send_key_char : char -> unit = "sendKeyChar"
end
