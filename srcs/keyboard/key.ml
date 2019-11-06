open Android_view

type event =
	| Escape
	| Tab
	| Backspace
	| Delete
	| Enter
	| Left
	| Right
	| Up
	| Down
	| Page_up
	| Page_down
	| Home
	| End

type accent = Acute | Grave | Circumflex | Tilde | Cedilla | Trema

type pad = Default | Numeric

type modifier = Shift | Ctrl | Alt | Accent of accent

type typing_value =
	| Char of int * int
	| Event of event * int

type value =
	| Typing of typing_value
	| Modifier of modifier
	| Change_pad of pad
	| Nothing

(** Represent a key with its 5 values
	The 4 corner values are optional *)
type t = {
	a : value option;
	b : value option;
	c : value option;
	d : value option;
	v : value
}

(** Transforms chars to upper case, adds meta_shift flag on events *)
let apply_shift =
	let meta_shift = Key_event.(get'meta_shift_left_on () lor get'meta_shift_on ()) in
	function
	| Char (c, _)		-> Char (Java_lang.Character.to_upper_case c, 0)
	| Event (kv, meta)	-> Event (kv, meta lor meta_shift)

(** Adds meta flags *)
let m_meta meta =
	function
	| Char (c, m')	-> Char (c, m' lor meta)
	| Event (e, m')	-> Event (e, m' lor meta)

let apply_ctrl =
	m_meta Key_event.(get'meta_ctrl_left_on () lor get'meta_ctrl_on ())
let apply_alt =
	m_meta Key_event.(get'meta_alt_left_on () lor get'meta_alt_on ())

(** Adds accents to chars that support it *)
let apply_accent acc =
	let dead_char =
		match acc with
		| Acute			-> 0x00B4
		| Grave			-> 0x0060
		| Circumflex	-> 0x005E
		| Tilde			-> 0x007E
		| Cedilla		-> 0x00B8
		| Trema			-> 0x00A8
	in
	function
	| Char (c, meta) as kv	->
		begin match Key_character_map.get_dead_char dead_char c with
			| 0		-> kv
			| c		-> Char (c, meta)
		end
	| Event _ as kv			-> kv

let apply_modifier k = function
	| Shift		-> apply_shift k
	| Ctrl		-> apply_ctrl k
	| Alt		-> apply_alt k
	| Accent a	-> apply_accent a k
