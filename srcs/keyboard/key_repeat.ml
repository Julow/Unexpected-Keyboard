(** Assign timeout to pressed keys
    Keys are compared physically *)
type t = {
  waiting : int option;
  pressed_keys : (Key.typing_value * int) list;
}

let pressed_timeout = 1000
let repeated_timeout = 200

let empty = { waiting = None; pressed_keys = [] }

(** Returns the new state and optionally, a timeout to call [on_timout]. *)
let on_down t kv =
  let timeout = pressed_timeout in
  let pressed_keys = (kv, timeout) :: t.pressed_keys in
  match t.waiting with
  | Some _ -> { t with pressed_keys }, None
  | None -> { waiting = Some timeout; pressed_keys }, Some timeout

(** Removes [kv] from the state *)
let on_up t kv =
  let pressed_keys = List.remove_assq kv t.pressed_keys in
  { t with pressed_keys }

let next_timeout = function
  | [] -> None
  | (_, rt) :: tl ->
      Some (List.fold_left (fun acc (_, rt) -> min rt acc) rt tl)

(** Returns the list of keys that should be repeated and optionally, the next timout *)
let on_timeout t =
  let rec loop past acc_keys acc_repeats = function
    | [] -> List.rev acc_keys, acc_repeats
    | (kv, rt) :: tl when rt <= past ->
        loop past ((kv, repeated_timeout) :: acc_keys) (kv :: acc_repeats) tl
    | (kv, rt) :: tl ->
        loop past ((kv, rt - past) :: acc_keys) acc_repeats tl
  in
  match t.waiting with
  | Some past ->
      let pressed_keys, repeats = loop past [] [] t.pressed_keys in
      let timeout = next_timeout pressed_keys in
      { waiting = timeout; pressed_keys }, repeats, timeout
  | None -> t, [], None
