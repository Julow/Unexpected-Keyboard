module Cmp =
struct

	(* Comparaisons *)

	let (&&&) a b = if a <> 0 then a else b

	let between a b x =
		if x < a then ~-1 else if x >= b then 1 else 0

	(** Perform binary search
		`lo` and `hi` are begin/end indexes (inclusive)
		`dir` is the function that decide which way to go:
			0 means found, a negative value means go to the left
				and positive, to the right *)
	let rec binary_search dir lo hi =
		if lo > hi
		then `Should_be_at lo
		else
			let mid = (lo + hi) / 2 in
			match dir mid with
			| 0				-> `Found_at mid
			| d when d < 0	-> binary_search dir lo (mid - 1)
			| _				-> binary_search dir (mid + 1) hi

end

module Array =
struct

	let binary_search a cmp =
		Cmp.binary_search (fun i -> cmp a.(i)) 0 (Array.length a - 1)

	let rev a =
		let len = Array.length a in
		Array.init len (fun i -> a.(len - i - 1))

end

(** Create a String object representing a single character *)
let java_string_of_code_point =
	let code_points = Jarray.create_int 1 in
	fun cp ->
		Jarray.set_int code_points 0 cp;
		Java_lang.String.create_cps code_points 0 1
