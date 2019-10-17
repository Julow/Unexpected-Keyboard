(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



open Test

exception Dummy_error

let suite =
  suite "lwt_result" [
    test "maps"
      (fun () ->
         let x = Lwt_result.return 0 in
         let correct = Lwt_result.return 1 in
         Lwt.return (Lwt_result.map ((+) 1) x = correct)
      );

    test ">|= is a variant of map"
      (fun () ->
         let x = Lwt_result.return 0 in
         let correct = Lwt_result.return 1 in
         Lwt.return (Lwt_result.(>|=) x ((+) 1) = correct)
      );

    test "map, error case"
      (fun () ->
         let x = Lwt_result.fail 0 in
         Lwt.return (Lwt_result.map ((+) 1) x = x)
      );

    test "map_err"
      (fun () ->
         let x = Lwt_result.return 0 in
         Lwt.return (Lwt_result.map_err ((+) 1) x = x)
      );

    test "map_err, error case"
      (fun () ->
         let x = Lwt_result.fail 0 in
         let correct = Lwt_result.fail 1 in
         Lwt.return (Lwt_result.map_err ((+) 1) x = correct)
      );

    test "bind"
      (fun () ->
         let x = Lwt_result.return 0 in
         let correct = Lwt_result.return 1 in
         let actual = Lwt_result.bind x (fun y -> Lwt_result.return (y + 1)) in
         Lwt.return (actual = correct)
      );

    test "bind, error case"
      (fun () ->
         let x = Lwt_result.fail 0 in
         let actual = Lwt_result.bind x (fun y -> Lwt_result.return (y + 1)) in
         Lwt.return (actual = x)
      );

    test "ok"
      (fun () ->
         let x = Lwt.return 0 in
         Lwt.return (Lwt_result.ok x = Lwt_result.return 0)
      );

    test "catch"
      (fun () ->
         let x = Lwt.return 0 in
         Lwt.return (Lwt_result.catch x = Lwt_result.return 0)
      );

    test "catch, error case"
      (fun () ->
         let x = Lwt.fail Dummy_error in
         Lwt.return (Lwt_result.catch x = Lwt_result.fail Dummy_error)
      );

    test "get_exn"
      (fun () ->
         let x = Lwt_result.return 0 in
         Lwt.return (Lwt_result.get_exn x = Lwt.return 0)
      );

    test "get_exn, error case"
      (fun () ->
         let x = Lwt_result.fail Dummy_error in
         Lwt.return (Lwt_result.get_exn x = Lwt.fail Dummy_error)
      );

    test "bind_lwt"
      (fun () ->
         let x = Lwt_result.return 0 in
         let f y = Lwt.return (y + 1) in
         Lwt.return (Lwt_result.bind_lwt x f = Lwt_result.return 1)
      );

    test "bind_lwt, error case"
      (fun () ->
         let x = Lwt_result.fail 0 in
         let f y = Lwt.return (y + 1) in
         Lwt.return (Lwt_result.bind_lwt x f = Lwt_result.fail 0)
      );

    test "bind_lwt_err"
      (fun () ->
         let x = Lwt_result.return 0 in
         let f y = Lwt.return (y + 1) in
         Lwt.return (Lwt_result.bind_lwt_err x f = Lwt_result.return 0)
      );

    test "bind_lwt_err, error case"
      (fun () ->
         let x = Lwt_result.fail 0 in
         let f y = Lwt.return (y + 1) in
         Lwt.return (Lwt_result.bind_lwt_err x f = Lwt_result.fail 1)
      );

    test "bind_result"
      (fun () ->
         let x = Lwt_result.return 0 in
         let f y = Result.Ok (y + 1) in
         Lwt.return (Lwt_result.bind_result x f = Lwt_result.return 1)
      );

    test "bind_result, error case"
      (fun () ->
         let x = Lwt_result.fail 0 in
         let f y = Result.Ok (y + 1) in
         Lwt.return (Lwt_result.bind_result x f = Lwt_result.fail 0)
      );
  ]
