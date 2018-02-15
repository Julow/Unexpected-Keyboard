class%java paint "android.graphics.Paint" =
object

	initializer (create_default : _)
	initializer (create : int -> _)
	initializer (copy : paint -> _)

end

class%java canvas "android.graphics.Canvas" =
object

	method draw_text : string -> float -> float -> paint -> unit = "drawText"

end
