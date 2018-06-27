(** Represent a key with its 5 values
	The 4 corner values are optional *)
type 'a t = {
	a : 'a option;
	b : 'a option;
	c : 'a option;
	d : 'a option;
	v : 'a
}
