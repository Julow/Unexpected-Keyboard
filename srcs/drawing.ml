open Android_graphics

(** Initialize the drawer
	`dp` should convert from the `dp` unit to `px`
	`render_key` should return the key string symbol
	See the actual drawing function below (`keyboard`) *)
let keyboard dp render_key =

	(* Styles *)

	let keyboard_bg = Color.rgb 30 30 30 in

	let p_key_bg = Paint_builder.(default
		|> argb 255 60 60 60) ()

	and p_key_label = Paint_builder.(default
		|> anti_alias
		|> text_align `Center
		|> text_size (dp 16.)
		|> argb 255 255 255 255) ()

	and p_key_corner_label = Paint_builder.(default
		|> anti_alias
		|> text_align `Center
		|> text_size (dp 10.)
		|> argb 255 160 160 160) ()
	in

	let keyboard_padding = Rect.padding_rect
		(Rect.create (dp 3.) (dp 3.) (dp 2.) (dp 6.))
	and key_bg_padding = Rect.padding (dp 1.3) (dp 1.3)
	and corner_padding = Rect.padding (dp 6.) (dp 7.5) in

	let key_bg_radius = dp 4. in

	(** Adjust the y coordinate for drawing text centered *)
	let text_center_y paint y =
		y -. (Paint.ascent paint +. Paint.descent paint) /. 2.
	in

	(** Draw the value of a key (label) *)
	let key_value paint x y value canvas =
		let y = text_center_y paint y in
		Canvas.draw_text canvas (render_key value) x y paint
	in

	(** Draw corners values *)
	let key_corners rect key canvas =
		let corner x y =
			function
			| Some v	-> key_value p_key_corner_label x y v canvas
			| None		-> ()
		in
		let Rect.{ l; r; t; b } = corner_padding rect in
		corner l t key.Key.a;
		corner r t key.Key.b;
		corner r b key.Key.c;
		corner l b key.Key.d
	in

	(** Draw a key inside a rectangle *)
	let key rect key canvas =
		begin
			let Rect.{ l; r; t; b } = key_bg_padding rect
			and rd = key_bg_radius in
			Canvas.draw_round_rect canvas l t r b rd rd p_key_bg
		end;
		key_corners rect key canvas;
		let mid_x, mid_y = Rect.middle rect in
		key_value p_key_label mid_x mid_y key.Key.v canvas
	in

	let rect_of_layout_pos KeyboardLayout.{ x; y; width; height } =
		Rect.create x (x +. width) y (y +. height)
	in

	(** Draw the keyboard on `canvas`, scaled to the canvs size
		The layout is expected to have a size of (1, 1) *)
	let keyboard layout canvas =
		Canvas.draw_color canvas keyboard_bg;
		let rect =
			let w = float (Canvas.get_width canvas)
			and h = float (Canvas.get_height canvas) in
			keyboard_padding (Rect.create 0. w 0. h)
		in
		KeyboardLayout.iter (fun (pos, k) ->
			let key_rect = Rect.transform rect (rect_of_layout_pos pos) in
			key key_rect k canvas
		) layout
	in
	keyboard
