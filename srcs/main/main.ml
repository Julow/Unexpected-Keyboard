open Android_content
open Android_graphics
open Android_inputmethodservice
open Android_util
open Android_view

(** Create a String object representing a single character *)
let java_string_of_code_point =
	let code_points = Jarray.create_int 1 in
	fun cp ->
		Jarray.set_int code_points 0 cp;
		Java_lang.String.create_cps code_points 0 1

(** Key_value to string *)
let render_key =
	let open Key_value in
	function
	| Char c				->
		(* TODO: OCaml and Java are useless at unicode *)
		Java.to_string (java_string_of_code_point c)
	| Event (`Escape, _)	-> "Esc"
	| Event (`Tab, _)		-> "\xE2\x87\xA5"
	| Event (`Backspace, _)	-> "\xE2\x8C\xAB"
	| Event (`Delete, _)	-> "\xE2\x8C\xA6"
	| Event (`Enter, _)		-> "\xE2\x8F\x8E"
	| Event (`Left, _)		-> "\xE2\x86\x90"
	| Event (`Right, _)		-> "\xE2\x86\x92"
	| Event (`Up, _)		-> "\xE2\x86\x91"
	| Event (`Down, _)		-> "\xE2\x86\x93"
	| Event (`Page_up, _)	-> "\xE2\x87\x9E"
	| Event (`Page_down, _)	-> "\xE2\x87\x9F"
	| Event (`Home, _)		-> "\xE2\x86\x96"
	| Event (`End, _)		-> "\xE2\x86\x98"
	| Shift					-> "\xE2\x87\xA7"
	| Accent `Acute			-> "\xCC\x81"
	| Accent `Grave			-> "\xCC\x80"
	| Accent `Circumflex	-> "\xCC\x82"
	| Accent `Tilde			-> "\xCC\x83"
	| Accent `Cedilla		-> "\xCC\xA7"
	| Accent `Trema			-> "\xCC\x88"
	| Nothing				-> ""
	| Change_pad `Default	-> "ABC"
	| Change_pad `Numeric	-> "123"

type t = {
	pointers	: Pointers.Table.t;
	layout		: Key_value.t Key.t KeyboardLayout.t;
	modifiers	: Modifiers.Stack.t;
	view		: View.t;
	dp			: float -> float
}

let create ~view ~dp () = {
	pointers = Pointers.Table.empty;
	layout = Layouts.qwerty;
	modifiers = Modifiers.Stack.empty;
	view; dp
}

let handle_key pointers t key =
	let send ev = { t with pointers }, [ ev; `Invalidate ]
	and with_mods modifiers = { t with modifiers; pointers }, [ `Invalidate ]
	and with_layout layout =
		{ t with pointers = Pointers.Table.empty;
			modifiers = Modifiers.Stack.empty;
			layout }, [ `Invalidate ]
	in
	match Modifiers.Stack.apply t.modifiers key with
	| Key_value.Char c		-> send (`Send_char c)
	| Event (ev, meta)		-> send (`Send_event (ev, meta))
	| Shift as kv			-> with_mods Modifiers.(Stack.add kv shift t.modifiers)
	| Accent acc as kv		-> with_mods Modifiers.(Stack.add kv (accent acc) t.modifiers)
	| Nothing				-> with_mods t.modifiers
	| Change_pad `Default	-> with_layout Layouts.qwerty
	| Change_pad `Numeric	-> with_layout Layouts.numeric

let update t = function
	| `Touch_event ev	->
		begin match Touch_event.on_touch t.view t.layout t.pointers ev with
			| `Key_pressed (key, pointers)	-> handle_key pointers t key
			| `Pointers_changed pointers	-> { t with pointers }, [ `Invalidate ]
			| `No_change					-> t, []
		end

let draw t canvas =
	let is_activated = Pointers.Table.key_exists t.pointers
	and render_key k = render_key (Modifiers.Stack.apply t.modifiers k) in
	Drawing.keyboard is_activated t.dp render_key
