class%java paint_align "android.graphics.Paint$Align" =
object
	val [@static] center : paint_align = "CENTER"
	val [@static] left : paint_align = "LEFT"
	val [@static] right : paint_align = "RIGHT"
end

let paint_ANTI_ALIAS_FLAG = 0x00000001

class%java paint "android.graphics.Paint" =
object
	initializer (create_default : _)
	initializer (create : int -> _)
	initializer (copy : paint -> _)

	method set_text_align : paint_align -> unit = "setTextAlign"
	method set_text_size : float -> unit = "setTextSize"
end

class%java canvas "android.graphics.Canvas" =
object
	method get_width : int = "getWidth"
	method get_height : int = "getHeight"

	method draw_text : string -> float -> float -> paint -> unit = "drawText"
end
