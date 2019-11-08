open Android_api.Graphics
open Key
open Android_utils

(** Initialize the drawer
	`dp` should convert from the `dp` unit to `px`
	`render_key` should return the key string symbol
	See the actual drawing function below (`keyboard`) *)
let keyboard dp render_key state =

  let is_activated key =
    Keyboard.State.key_activated state key
  and render_key k =
    let k = match k with
      | Typing tv -> Typing (Keyboard.Modifiers.apply tv state.modifiers)
      | k -> k
    in
    render_key k
  in

	(* Styles *)

	let key_bg_radius = dp 4.
	and key_shadow_offset = Rect.offset (dp 0.5) (dp 0.5) in

	let keyboard_bg = Color.rgb 60 60 50 in

	let p_key_bg = Paint_builder.(default
		|> argb 255 27 27 27) ()

	and p_key_shadow = Paint_builder.(default
		|> stroke (dp 1.)
		|> argb 255 70 70 70) ()

	and p_key_bg_activated = Paint_builder.(default
		|> argb 255 15 15 15) ()

	and p_key_label = Paint_builder.(default
		|> anti_alias
		|> text_align `Center
		|> text_size (dp 16.)
		|> argb 255 255 255 255) ()

	and p_key_corner_label_l, p_key_corner_label_r =
		let open Paint_builder in
		let p = default
			|> anti_alias
			|> text_size (dp 9.)
			|> argb 255 180 180 180
		in
		(p |> text_align `Left) (),
		(p |> text_align `Right) ()
	in

	let keyboard_padding = Rect.padding_rect
		(Rect.create (dp 3.) (dp 3.) (dp 2.) (dp 6.))
	and key_bg_padding = Rect.padding (dp 1.3) (dp 1.3)
	and corner_padding = Rect.padding (dp 3.5) (dp 7.5) in

	(* Adjust the y coordinate for drawing text centered *)
	let text_center_y paint y =
		y -. (Paint.ascent paint +. Paint.descent paint) /. 2.
	in

	(* Draw the value of a key (label) *)
	let key_value paint x y value canvas =
		let y = text_center_y paint y in
		Canvas.draw_text canvas (render_key value) x y paint
	in

	(* Draw corners values *)
	let key_corners rect key canvas =
		let corner paint x y =
			function
			| Some v	-> key_value paint x y v canvas
			| None		-> ()
		in
		let { Rect.l; r; t; b } = corner_padding rect in
		corner p_key_corner_label_l l t key.a;
		corner p_key_corner_label_r r t key.b;
		corner p_key_corner_label_r r b key.c;
		corner p_key_corner_label_l l b key.d
	in

	let draw_rect canvas { Rect.l; t; r; b } radius paint =
		Canvas.draw_round_rect canvas l t r b radius radius paint
	in

	let key_background rect activated canvas =
		let rect = key_bg_padding rect in
		let bg = draw_rect canvas rect key_bg_radius in
		let shadow = draw_rect canvas (key_shadow_offset rect) key_bg_radius in
		if activated
		then bg p_key_bg_activated
		else (shadow p_key_shadow; bg p_key_bg)
	in

	(* Draw a key inside a rectangle *)
	let key rect key canvas =
		key_background rect (is_activated key) canvas;
		key_corners rect key canvas;
		let mid_x, mid_y = Rect.middle rect in
		key_value p_key_label mid_x mid_y key.v canvas
	in

	let rect_of_layout_pos { Keyboard.Layout.x; y; width; height } =
		Rect.create x (x +. width) y (y +. height)
	in

	(* Draw the keyboard on `canvas`, scaled to the canvs size
		The layout is expected to have a size of (1, 1) *)
	let keyboard canvas =
		Canvas.draw_color canvas keyboard_bg;
		let rect =
			let w = float (Canvas.get_width canvas)
			and h = float (Canvas.get_height canvas) in
			keyboard_padding (Rect.create 0. w 0. h)
		in
		Keyboard.Layout.iter (fun (pos, k) ->
			let key_rect = Rect.transform rect (rect_of_layout_pos pos) in
			key key_rect k canvas
		) state.layout
	in
	keyboard
