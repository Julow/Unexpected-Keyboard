type state = Hold_waiting | Hold | Waiting | Locked

type modifier = Key.modifier * state

(** List of modifiers *)
type t = modifier list

let empty = []

let active_modifiers t =
  List.map (fun (m, _) -> m) t

let rec extract_modifier acc m = function
	| (m'', _ as m') :: tl when m = m'' ->
		Some (m', List.rev_append acc tl)
	| m' :: tl	-> extract_modifier (m' :: acc) m tl
	| []		-> None

(** Down event on a modifier
	[Shift] is the only modifier that can be locked
	Waiting (Shift) -> Locked
	Locked -> Hold
	_ -> Hold_waiting *)
let on_down m t =
	match extract_modifier [] m t with
	| Some ((Key.Shift, Waiting), t)	-> (m, Locked) :: t
	| Some ((_, Locked), t)				-> (m, Hold) :: t
	| Some (_, t)						-> (m, Hold_waiting) :: t
	| None								-> (m, Hold_waiting) :: t

(** Up event on a modifier
	Hold_waiting -> Waiting
	Hold -> remove *)
let on_up m t =
	match extract_modifier [] m t with
	| Some ((_, Hold_waiting), t)	-> (m, Waiting) :: t
	| Some ((_, Hold), t)			-> t
	| _								-> t

(** Cancel event on a modifier
	Hold_waiting | Hold -> remove *)
let on_cancel m t =
	match extract_modifier [] m t with
	| Some ((_, (Hold_waiting | Hold)), t)	-> t
	| _										-> t

(** Update the modifiers after a key have been handled
	Hold_waiting -> Hold
	Waiting -> remove *)
let rec on_key_press =
	function
	| (_, Waiting) :: tl		-> on_key_press tl
	| (m, Hold_waiting) :: tl	-> (m, Hold) :: on_key_press tl
	| hd :: tl					-> hd :: on_key_press tl
	| []						-> []
