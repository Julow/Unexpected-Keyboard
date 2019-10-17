(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



let temp_name =
  let rng = Random.State.make_self_init () in
  fun () ->
    let number = Random.State.int rng 10000 in
    Printf.sprintf (*"_build/"*)"lwt-testing-%04d" number

let temp_file () =
  Filename.temp_file (*~temp_dir:"_build"*) "lwt-testing-" ""

let temp_directory () =
  let rec attempt () =
    let path = temp_name () in
    try
      Unix.mkdir path 0o755;
      path
    with Unix.Unix_error (Unix.EEXIST, "mkdir", _) -> attempt ()
  in
  attempt ()
