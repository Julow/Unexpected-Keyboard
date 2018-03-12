(** Value of a key *)

type event = [ `Escape ]
type accent = [ `Acute ]

type key =
	| Char of int
	| Event of event * int
	| Shift
	| Accent of accent
	| Nothing
