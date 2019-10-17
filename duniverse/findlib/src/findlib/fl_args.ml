(* $Id$ *)

(* Rewrite a list of arguments args (from Sys.args) so that contracted
   options like -L<arg> are transformed to -L <arg>, and become parseable
   by Arg.
 *)

let make_ht (l:string list) =
  let ht = Hashtbl.create 10 in
  List.iter (fun x -> Hashtbl.add ht x ()) l;
  ht

let is_prefix s1 s2 =
  let l1 = String.length s1 in
  let l2 = String.length s2 in
  l2 >= l1 && String.sub s2 0 l1 = s1

let rewrite_contracted_args spec contracted_opts args =
  let args = Array.to_list args in
  let switches =
    List.map
      (fun (name,kind,text) -> name)
      (List.filter
         (fun (name,kind,text) ->
            match kind with
              | Arg.Unit _ 
              | Arg.Set _
              | Arg.Clear _ -> true
              | Arg.Tuple _ ->
                  failwith
                    "Fl_args.rewrite_for_contracted_args: Arg.Tuple unsupported"
              | _ -> false
         )
         spec
      ) in
  let unary_opts =
    List.map
      (fun (name,kind,text) -> name)
      (List.filter
         (fun (name,kind,text) -> 
            match kind with
              | Arg.String _
              | Arg.Set_string _
              | Arg.Int _
              | Arg.Set_int _
              | Arg.Float _
              | Arg.Set_float _ -> true
              | _ -> false
         )
         spec
      ) in
  let rest_opts =
    List.map
      (fun (name,kind,text) -> name)
      (List.filter
         (fun (name,kind,text) -> 
            match kind with
              | Arg.Rest _ -> true
              | _ -> false
         )
         spec
      ) in
      
  let sw_ht = make_ht switches in
  let unary_ht = make_ht unary_opts in
  let rest_ht = make_ht rest_opts in

  let rec rewrite (args:string list) =
    match args with
      | arg :: args_rest when Hashtbl.mem sw_ht arg ->
          arg :: rewrite args_rest
      | arg :: args_rest when Hashtbl.mem rest_ht arg ->
          args
      | arg1 :: arg2 :: args_rest when Hashtbl.mem unary_ht arg1 ->
          arg1 :: arg2 :: rewrite args_rest
      | arg :: args_rest ->
          ( try
              let args1 = expand arg contracted_opts in
              let args2 = rewrite args_rest in
	      args1 @ args2
            with
              | Not_found ->
                  arg :: rewrite args_rest
          )
      | [] ->
          []

  and expand arg olo =
    match olo with
      | olo1 :: olo_rest ->
          if is_prefix olo1 arg then
            let p = String.length olo1 in
            let l = String.length arg in
            [ olo1;
              String.sub arg p (l-p)
            ]
          else
            expand arg olo_rest
      | [] ->
          raise Not_found

  in

  Array.of_list (rewrite args)
