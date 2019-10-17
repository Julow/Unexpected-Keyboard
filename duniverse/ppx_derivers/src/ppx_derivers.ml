type deriver = ..

let all = Hashtbl.create 42

let register name deriver =
  if Hashtbl.mem all name then
    Printf.ksprintf failwith
      "Ppx_deriviers.register: %S is already registered" name;
  Hashtbl.add all name deriver

let lookup name =
  match Hashtbl.find all name with
  | drv -> Some drv
  | exception Not_found -> None

let derivers () =
  Hashtbl.fold (fun name drv acc -> (name, drv) :: acc) all []
