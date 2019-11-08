(** Create a String object representing a single character *)
let string_of_code_point =
	let code_points = Jarray.create_int 1 in
	fun cp ->
		Jarray.set_int code_points 0 cp;
		Java_api.Lang.String.create_cps code_points 0 1
