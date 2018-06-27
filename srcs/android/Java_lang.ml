class%java character "java.lang.Character" =
object
	method [@static] to_upper_case : int -> int = "toUpperCase"
end

class%java char_sequence "java.lang.CharSequence" = object
end

class%java string "java.lang.String" =
object
	inherit char_sequence
	initializer (create : string -> _)
	initializer (create_cps : int array -> int -> int -> _)
end
