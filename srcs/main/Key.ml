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

type value =
	| Char of int
	| Event of event * int
	| Shift
	| Accent of accent
	| Nothing
	| Change_pad of pad

(** Represent a key with its 5 values
	The 4 corner values are optional *)
type t = {
	a : value option;
	b : value option;
	c : value option;
	d : value option;
	v : value
}
