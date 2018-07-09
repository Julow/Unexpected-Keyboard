(** Keyboard layouts *)

open KeyboardLayout.Desc
open Key

let key ?(width=1.) ?a ?b ?c ?d v = key ~width Key.{ a; b; c; d; v }
let c c = Char (Char.code c)
let event evt = Event (evt, 0)

let qwerty = build [
	row [
		key (c 'q') ~b:(c '1') ~d:(c '~') ~a:(event Escape) ~c:(c '!');
		key (c 'w') ~b:(c '2') ~d:(c '@');
		key (c 'e') ~b:(c '3') ~c:(c '#') ~d:(Accent Acute);
		key (c 'r') ~b:(c '4') ~d:(c '$');
		key (c 't') ~b:(c '5') ~d:(c '%');
		key (c 'y') ~b:(c '6') ~d:(c '^');
		key (c 'u') ~b:(c '7') ~d:(c '&');
		key (c 'i') ~b:(c '8') ~d:(c '*') ~a:(Accent Trema);
		key (c 'o') ~b:(c '9') ~d:(c '(') ~c:(c ')');
		key (c 'p') ~b:(c '0')
	];

	row ~margin:0.5 [
		key (c 'a') ~a:(event Tab) ~b:(c '`') ~c:(Accent Grave);
		key (c 's');
		key (c 'd');
		key (c 'f');
		key (c 'g') ~b:(c '-') ~d:(c '_');
		key (c 'h') ~b:(c '=') ~d:(c '+') ~a:(event Left);
		key (c 'j') ~d:(c '{') ~b:(c '}') ~c:(event Down);
		key (c 'k') ~d:(c '[') ~b:(c ']') ~a:(event Up);
		key (c 'l') ~b:(c '|') ~d:(c '\\') ~c:(event Right)
	];

	row [
		key ~width:1.5 Shift;
		key (c 'z') ~a:(Accent Circumflex);
		key (c 'x');
		key (c 'c') ~a:(c '<') ~d:(c '.') ~c:(Accent Cedilla);
		key (c 'v') ~b:(c '>') ~d:(c ',');
		key (c 'b') ~b:(c '?') ~d:(c '/');
		key (c 'n') ~a:(Accent Tilde) ~b:(c ':') ~d:(c ';');
		key (c 'm') ~b:(c '"') ~d:(c '\'');
		key ~width:1.5 (event Backspace) ~b:(event Delete)
	];

	row ~height:0.9 ~margin:0.5 [
		key ~width:1.5 Nothing ~d:(Change_pad Numeric);
		key ~width:6.0 (c ' ');
		key ~width:1.5 (event Enter)
	]
]

let numeric = build [
	row ~margin:0. [
		key ~width:0.75 (c '(') ~a:(c '<');
		key ~width:0.75 (c ')') ~b:(c '>');
		key (c '7');
		key (c '8') ~b:(event Up);
		key (c '9');
		key ~width:0.75 (event Backspace) ~b:(event Delete);
	];
	row ~margin:0.25 [
		key ~width:0.75 (event Tab);
		key ~width:0.75 (c '/') ~b:(c '%');
		key (c '4') ~d:(event Left);
		key (c '5');
		key (c '6') ~b:(event Right);
		key ~width:0.75 (c '-');
	];
	row ~margin:0.5 [
		key ~width:0.75 Shift;
		key ~width:0.75 (c '*') ~b:(c '^');
		key (c '1');
		key (c '2') ~d:(event Down);
		key (c '3');
		key ~width:0.75 (c '+');
	];
	row ~margin:0.375 [
		key ~width:1.25 (Change_pad Default);
		key ~width:2. (c '0');
		key (c '.') ~d:(c ',');
		key ~width:0.75 (event Enter);
	]
]
