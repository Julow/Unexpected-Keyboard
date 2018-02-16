(** Keyboard layout
	Store the absolute position for each keys
	Position are scaled as if the keyboard had a size of (1 * 1) *)

type pos = {
	x		: float;
	y		: float;
	width	: float;
	height	: float
}

type 'a t = (pos * 'a) array

let iter = Array.iter

module Desc =
struct

	(** Conveniant functions for easily describe keyboard layouts *)

	type 'a key = {
		width : float;
		value : 'a
	}

	let key ?(width=1.) value = { width; value }

	type 'a row = {
		height : float;
		margin : float;
		keys : 'a key list
	}

	let row ?(height=1.) ?(margin=0.) keys = { height; margin; keys }

	(** Build the layout
		Key size/position are scaled down until the total
		width and height are 1 *)
	let build rows =
		let size_x, size_y = List.fold_left (fun (max_x, y) row ->
			let x = List.fold_left (fun x key -> x +. key.width)
				row.margin row.keys in
			max max_x x, y +. row.height
		) (0., 0.) rows in
		let fold_key y height key (acc, x) =
			let width = key.width /. size_x in
			({ x; y; width; height }, key.value) :: acc,
			x +. width
		in
		let fold_row row (acc, y) =
			let margin = row.margin /. size_x
			and height = row.height /. size_y in
			let acc, x = List.fold_right (fold_key y height)
				row.keys (acc, margin) in
			acc, y +. height
		in
		let keys, _ = List.fold_right fold_row rows ([], 0.) in
		Array.of_list keys

end
