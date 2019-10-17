(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



open Test

exception Dummy_error

let suite = suite "lwt_condition" [

  test "basic wait" begin fun () ->
    let c = Lwt_condition.create () in
    let w = Lwt_condition.wait c in
    let () = Lwt_condition.signal c 1 in
    Lwt.bind w (fun v -> Lwt.return (v = 1))
  end;

  test "mutex unlocked during wait" begin fun () ->
    let c = Lwt_condition.create () in
    let m = Lwt_mutex.create () in
    let _ = Lwt_mutex.lock m in
    let w = Lwt_condition.wait ~mutex:m c in
    Lwt.return (Lwt.state w = Lwt.Sleep
                && not (Lwt_mutex.is_locked m))
  end;

  test "mutex relocked after wait" begin fun () ->
    let c = Lwt_condition.create () in
    let m = Lwt_mutex.create () in
    let _ = Lwt_mutex.lock m in
    let w = Lwt_condition.wait ~mutex:m c in
    let () = Lwt_condition.signal c 1 in
    Lwt.bind w (fun v ->
      Lwt.return (v = 1 && Lwt_mutex.is_locked m))
  end;

  test "signal is not sticky" begin fun () ->
    let c = Lwt_condition.create () in
    let () = Lwt_condition.signal c 1 in
    let w = Lwt_condition.wait c in
    Lwt.return (Lwt.state w = Lwt.Sleep)
  end;

  test "broadcast" begin fun () ->
    let c = Lwt_condition.create () in
    let w1 = Lwt_condition.wait c in
    let w2 = Lwt_condition.wait c in
    let () = Lwt_condition.broadcast c 1 in
    Lwt.bind w1 (fun v1 ->
    Lwt.bind w2 (fun v2 ->
    Lwt.return (v1 = 1 && v2 = 1)))
  end;

  test "broadcast exception" begin fun () ->
    let c = Lwt_condition.create () in
    let w1 = Lwt_condition.wait c in
    let w2 = Lwt_condition.wait c in
    let () = Lwt_condition.broadcast_exn c Dummy_error in
    Lwt.try_bind
      (fun () -> w1)
      (fun _ -> Lwt.return_false)
      (fun exn1 ->
        Lwt.try_bind
          (fun () -> w2)
          (fun _ -> Lwt.return_false)
          (fun exn2 ->
            Lwt.return (exn1 = Dummy_error && exn2 = Dummy_error)))
  end;

]
