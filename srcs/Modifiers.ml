(** Functions that transform Key_value.t *)

(** Do nothing *)
let default k = k

(** Transforms chars to upper case, adds meta_shift flag on events *)
let shift =
	let meta_shift = Key_event.get'meta_shift_left_on in
	function
	| Char c			-> Char (Java_lang.Character.to_upper_case c)
	| Event (kv, meta)	-> Event (kv, meta lor meta_shift ())
	| kv				-> kv

(** Adds accents to chars that support it *)
let accent acc =
	let dead_char =
		match acc with
		| `Acute		-> 0x00B4
	in
	function
	| Char c as kv		->
		begin match Key_character_map.get_dead_char dead_char c with
			| 0		-> kv
			| c		-> Char c
		end
	| kv				-> kv
