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
	(* [@@deriving eq] *)

type accent = [ `Acute | `Grave | `Circumflex | `Tilde | `Cedilla | `Trema ]
	(* [@@deriving eq] *)

type pad = [ `Default | `Numeric ]
	(* [@@deriving eq] *)

type t =
	| Char of int
	| Event of event * int
	| Shift
	| Accent of accent
	| Nothing
	| Change_pad of pad
	(* [@@deriving eq] *)

(** Implement equality for the type [t]
	Should switch to using `ppx_deriving` when it is available for android *)
let equal = (=)
