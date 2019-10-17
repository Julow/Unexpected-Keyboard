(* $Id$
 * ----------------------------------------------------------------------
 *
 *)

(* TODO:
 * - Use a hashtable in 'find'
 * - implement le_than with an 'iter' like method
 *)


(**********************************************************************)


module type IdentifiedType = 
  sig
    type t
    type id_t
    val id : t -> id_t
  end

exception Inconsistent_ordering

module type S =
  sig
    type key
    type el_t
    type t
    val create : unit -> t
    val add : t -> el_t -> unit
    val let_le : t -> key -> key -> unit
    val find : t -> key -> el_t
    val le_than : t -> key -> key -> bool
    val key : el_t -> key
    val iter_up : (el_t -> unit) -> t -> unit
    val iter_down : (el_t -> unit) -> t -> unit
    val iter_up_at : (el_t -> unit) -> t -> key list -> unit
    val iter_down_at : (el_t -> unit) -> t -> key list -> unit

    val clear : t -> unit
    val replace : t -> key -> el_t -> unit
    val delete : t -> key -> unit

    val copy : t -> t
  end


(**********************************************************************)

module Make(H: IdentifiedType) =
  struct

    type key = H.id_t

    type el_t = H.t

    type 'a node =
	{ mutable content : 'a;
	  mutable smaller : 'a node list;
	  mutable bigger  : 'a node list;
	  mutable mark    : bool;    (* used in 'iter' *)
	  (* mutable ppmark  : bool *)    (* used in 'private_property' *)
	} 

    type t = 
	{ mutable cnt : el_t node list;
	  mutable lock : bool
	} 

    let copy ord =
      (* This operation is quite expensive when the graph has already
       * relations. In findlib, this case is avoided, and is here only
       * implemented for completeness.
       *)
      let ord' = 
	{ cnt = List.map (fun n -> { n with 
				       smaller = [];
				       bigger = [];
				       mark = false }) ord.cnt;
	  lock = false
	} in
      let combined_list = List.combine ord.cnt ord'.cnt in
      let lookup_node n =
	(* Find the new node corresponding to old node n *)
	try List.assq n combined_list 
	with Not_found -> assert false
      in
      List.iter2
	(fun n n' ->
	   (* n: old node, n': new node *)
	   let smaller = List.map lookup_node n.smaller in
	   let bigger  = List.map lookup_node n.bigger in
	   n'.smaller <- smaller;
	   n'.bigger <- bigger;
	)
	ord.cnt
	ord'.cnt;
      ord'


    let rec delete_all p l =
      match l with
	x::l' ->
	  if p x then delete_all p l' else x :: delete_all p l'
      |	[] -> []


    (******************************************************************)

    let create () = { cnt = []; lock = false }

    let clear ordering =
      ordering.cnt <- [];
      ordering.lock <- false

    (******************************************************************)

    let add ordering x =
      (* Is there already a node with the same key? *)
      let k = H.id x in
      if List.exists (fun y -> H.id y.content = k) ordering.cnt then
	raise Inconsistent_ordering;

      (* Ok, add the node to the list *)
      let nx = { content = x;
		 smaller = [];
		 bigger = [];
		 mark = false
	       } in
      ordering.cnt <- nx :: ordering.cnt


    (******************************************************************)

    let find_node ordering kx = 
      let rec search l =
	match l with
	  []      -> raise Not_found
	| y :: l' -> if H.id y.content = kx then y else search l'
      in
      search ordering.cnt


    let find ordering kx = (find_node ordering kx).content

    (******************************************************************)

    let replace ordering kx x' =
      let x = find_node ordering kx in
      x.content <- x'

    (******************************************************************)

    let delete ordering kx =
      let x = find_node ordering kx in
      ordering.cnt <- delete_all (fun a -> a == x) ordering.cnt;
      List.iter
	(fun x ->
	  x.smaller <- delete_all (fun a -> a == x) x.smaller;
	  x.bigger  <- delete_all (fun a -> a == x) x.bigger)
	ordering.cnt
	   

    (******************************************************************)

    let le_than ordering kx ky =
      (* Find x, y: *)
      let x = find_node ordering kx in
      let y = find_node ordering ky in

      let rec search x1 =
	if x1 == y then
	  true
	else
	  List.exists search x1.bigger

      in
      search x

    (******************************************************************)

    let let_le ordering kx ky =
      (* Find x, y: *)
      let x = find_node ordering kx in
      let y = find_node ordering ky in

      (* If already done just return (this is an idempotent function) *)
      if not (List.memq x y.smaller) then begin

        (* let x <= y. This is only allowed if not (y <= x) *)
      	if le_than ordering ky kx then
	  raise Inconsistent_ordering;

        (* Ok, add the relation *)
      	x.bigger  <- y :: x.bigger;
      	y.smaller <- x :: y.smaller
      end

    (******************************************************************)

    let key x = H.id x

    (******************************************************************)

    let iter upwards f ordering =

      let in_direction n = 
	if upwards then n.bigger else n.smaller in

      let against_direction n =
	if upwards then n.smaller else n.bigger in

      (* the following is written as if for iter_up. *)
      
      let rec find_biggest ordlist =
	(* find biggest, non-marked node *)
	match ordlist with
	  []             -> raise Not_found
	| nx :: ordlist' -> if not nx.mark && in_direction nx = [] 
	                    then nx
	                    else find_biggest ordlist'
      in

      let rec run_up n =
	(* iterate over all nodes <= x and return their number *)

	let rec run_up_list l =
	  match l with
	    []      -> 0
	  | x :: l' -> run_up x + run_up_list l'
	in

	if n.mark then
	  (* have already visited this node *)
	  0
	else
	  let u = run_up_list (against_direction n) in
	  n.mark <- true;
	  f n.content;
	  u + 1
      in
	  
      (* Lock *)
      if ordering.lock then
	failwith "iter_up: recursive application not allowed";
      ordering.lock <- true;

      (* clear all marks *)
      List.iter (fun nx -> nx.mark <- false) ordering.cnt;

      (* Catch exceptions *)

      try
        (* while there is a biggest node... *)
      	let c = ref 0 in             (* counter *)
      	while !c < List.length ordering.cnt do
	
	  (* Find a biggest node *)
	  let n_biggest = find_biggest ordering.cnt in

	  (* run through the graph *)
	  c := !c + run_up n_biggest

      	done;
      
        (* unlock *)
        ordering.lock <- false

      with
	any -> (* unlock, too *)
	  ordering.lock <- false;
	  raise any


    let iter_up = iter true
    let iter_down = iter false


    (******************************************************************)

    let iter_at upwards f ordering startpoints =

(*
      let in_direction n = 
	if upwards then n.bigger else n.smaller in
 *)

      let against_direction n =
	if upwards then n.smaller else n.bigger in

      (* the following is written as if for iter_up. *)
      
      let rec run_up n =
	(* iterate over all nodes <= x and return their number *)

	let rec run_up_list l =
	  match l with
	    []      -> 0
	  | x :: l' -> run_up x + run_up_list l'
	in

	if n.mark then
	  (* have already visited this node *)
	  0
	else
	  let u = run_up_list (against_direction n) in
	  n.mark <- true;
	  f n.content;
	  u + 1
      in
	  
      (* Lock *)
      if ordering.lock then
	failwith "iter_up: recursive application not allowed";
      ordering.lock <- true;

      (* clear all marks *)
      List.iter (fun nx -> nx.mark <- false) ordering.cnt;

      (* Catch exceptions *)

      try

	List.iter
	  (fun start ->
	    let _ = run_up (find_node ordering start) in ())
	  startpoints;
      
        (* unlock *)
        ordering.lock <- false

      with
	any -> (* unlock, too *)
	  ordering.lock <- false;
	  raise any


    let iter_up_at = iter_at true
    let iter_down_at = iter_at false


    (******************************************************************)

  end
