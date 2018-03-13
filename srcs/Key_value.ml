(** Value of a key *)

type event = [
	| `Escape
	| `Tab
	| `Backspace
	| `Delete
	| `Enter
	| `Left
	| `Right
	| `Up
	| `Down
	| `Page_up
	| `Page_down
	| `Home
	| `End
]

type accent = [ `Acute | `Grave | `Circumflex | `Tilde | `Cedilla | `Trema ]

type t =
	| Char of int
	| Event of event * int
	| Shift
	| Accent of accent
	| Nothing
