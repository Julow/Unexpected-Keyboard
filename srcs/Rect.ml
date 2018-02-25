(** A rectangle
	l, r, t and b are its edges: left, right, top and bottom *)
type t = {
	l : float;
	r : float;
	t : float;
	b : float
}

let create l r t b = { l; r; t; b }

let width { l; r } = r -. l
let height { t; b } = b -. t

(** Scale every edges *)
let scale x y { l; r; t; b } =
	{ l = l *. x; r = r *. x; t = t *. y; b = b *. y }

(** Shrink the rect toward its center
	A negative value grow it *)
let padding x y { l; r; t; b } =
	{ l = l +. x; r = r -. x; t = t +. y; b = b -. y }

let padding_rect a { l; r; t; b } =
	{ l = l +. a.l; r = r -. a.r; t = t +. a.t; b = b -. a.b }

(** Offset by the origin (l, t) and scale by the length *)
let transform a t =
	let w = width a and h = height a in
	{	l = a.l +. t.l *. w;
		r = a.l +. t.r *. w;
		t = a.t +. t.t *. h;
		b = a.t +. t.b *. h }

(** (x, y) coordinates of the center of the rectangle *)
let middle { l; r; t; b } =
	l +. ((r -. l) /. 2.),
	t +. ((b -. t) /. 2.)
