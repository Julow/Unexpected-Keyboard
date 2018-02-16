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

(* Search the key at the position (x, y) *)
let pick t x y =
	let cmp (p, _) = Utils.Cmp.(between p.y (p.y +. p.height) y
			&&& between p.x (p.x +. p.width) x) in
	match Utils.Array.binary_search t cmp with
	| `Should_be_at _	-> None
	| `Found_at i		-> Some t.(i)

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
		let fold_key y height (acc, x) key =
			let width = key.width /. size_x in
			({ x; y; width; height }, key.value) :: acc,
			x +. width
		in
		let fold_row (acc, y) row =
			let margin = row.margin /. size_x
			and height = row.height /. size_y in
			let acc, x = List.fold_left (fold_key y height)
				(acc, margin) row.keys in
			acc, y +. height
		in
		let keys, _ = List.fold_left fold_row ([], 0.) rows in
		Utils.Array.rev (Array.of_list keys)

end
