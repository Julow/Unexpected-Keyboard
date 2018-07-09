class%java color "android.graphics.Color" =
object
	method [@static] rgb : int -> int -> int -> int32 = "rgb"
	method [@static] argb : int -> int -> int -> int -> int32 = "argb"
end

class%java paint_align "android.graphics.Paint$Align" =
object
	val [@static] center : paint_align = "CENTER"
	val [@static] left : paint_align = "LEFT"
	val [@static] right : paint_align = "RIGHT"
end

class%java paint_style "android.graphics.Paint$Style" =
object
	val [@static] fill : paint_style = "FILL"
	val [@static] fill_and_stroke : paint_style = "FILL_AND_STROKE"
	val [@static] stroke : paint_style = "STROKE"
end

let paint_ANTI_ALIAS_FLAG = 0x00000001

class%java paint "android.graphics.Paint" =
object
	initializer (create_default : _)
	initializer (create : int -> _)
	initializer (copy : paint -> _)

	method ascent : float = "ascent"
	method descent : float = "descent"

	method set_argb : int -> int -> int -> int -> unit = "setARGB"
	method set_alpha : int -> unit = "setAlpha"
	method set_anti_alias : bool -> unit = "setAntiAlias"
	method set_color : int32 -> unit = "setColor"
	method set_style : paint_style -> unit = "setStyle"
	method set_stroke_width : float -> unit = "setStrokeWidth"
	method set_text_align : paint_align -> unit = "setTextAlign"
	method set_text_size : float -> unit = "setTextSize"
end

class%java canvas "android.graphics.Canvas" =
object
	method get_width : int = "getWidth"
	method get_height : int = "getHeight"

	method draw_color : int32 -> unit = "drawColor"
	method draw_rect : float -> float -> float -> float -> paint -> unit =
		"drawRect"
	method draw_round_rect : float -> float -> float -> float -> float ->
		float -> paint -> unit = "drawRoundRect"
	method draw_text : string -> float -> float -> paint -> unit = "drawText"
end
