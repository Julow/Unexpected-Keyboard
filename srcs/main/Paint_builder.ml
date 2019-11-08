(** Util to create Paint objects (from Android_graphics)
	Example:
		Paint_builder.(default
			|> anti_alias
			|> stroke
			|> text_align `Center
			|> text_size 20.)
			() *)

open Android_api.Graphics

type t = unit -> Paint.t

let default () = Paint.create_default ()
let with_flags flags () = Paint.create flags
let copy paint () = Paint.copy paint

let _set set value (t : t) () : Paint.t =
	let p = t () in
	set p value;
	p

open Paint

let argb a r g b t () =
	let p = t () in
	set_argb p a r g b;
	p

let alpha = _set set_alpha
let anti_alias = _set set_anti_alias true
let color = _set set_color
let fill t () = _set set_style (Paint_style.get'fill ()) t ()

let fill_and_stroke w t () =
	let p = t () in
	set_style p (Paint_style.get'fill_and_stroke ());
	set_stroke_width p w;
	p

let stroke w t () =
	let p = t () in
	set_style p (Paint_style.get'stroke ());
	set_stroke_width p w;
	p

let text_align a t () =
	match a with
	| `Center	-> _set set_text_align (Paint_align.get'center ()) t ()
	| `Left		-> _set set_text_align (Paint_align.get'left ()) t ()
	| `Right	-> _set set_text_align (Paint_align.get'right ()) t ()

let text_size = _set set_text_size
