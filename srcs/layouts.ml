(** Keyboard layouts *)

open KeyboardLayout.Desc

let key ?(width=1.) ?a ?b ?c ?d v = key ~width Key.{ a; b; c; d; v }
let c c = Char (Char.code c)
let event evt = Event (evt, 0)

let qwerty = build [
	row [
		key (c 'q') ~b:(c '1') ~d:(c '~') ~a:(event `Escape) ~c:(c '!');
		key (c 'w') ~b:(c '2') ~d:(c '@');
		key (c 'e') ~b:(c '3') ~d:(c '#') ~a:(Accent `Acute);
		key (c 'r') ~b:(c '4') ~d:(c '$');
		key (c 't') ~b:(c '5') ~d:(c '%');
		key (c 'y') ~b:(c '6') ~d:(c '^');
		key (c 'u') ~b:(c '7') ~d:(c '&');
		key (c 'i') ~b:(c '8') ~d:(c '*');
		key (c 'o') ~b:(c '9') ~d:(c '(') ~c:(c ')');
		key (c 'p') ~b:(c '0')
	];

	row ~margin:0.5 [
		key (c 'a');
		key (c 's');
		key (c 'd');
		key (c 'f');
		key (c 'g');
		key (c 'h');
		key (c 'j');
		key (c 'k');
		key (c 'l')
	];

	row [
		key ~width:1.5 Shift;
		key (c 'z');
		key (c 'x');
		key (c 'c');
		key (c 'v');
		key (c 'b');
		key (c 'n');
		key (c 'm');
		key ~width:1.5 Nothing
	];

	row ~height:0.9 ~margin:1. [
		key ~width:1.5 (c '.');
		key ~width:6.0 (c ' ');
		key ~width:1.5 Nothing
	]
]
