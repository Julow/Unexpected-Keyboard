(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



open Test

exception Dummy_error

let suite = suite "lwt_pool" [

  test "basic create-use" begin fun () ->
    let gen = fun () -> Lwt.return () in
    let p = Lwt_pool.create 1 gen in
    Lwt.return (Lwt.state (Lwt_pool.use p Lwt.return) = Lwt.Return ())
  end;

  test "creator exception" begin fun () ->
    let gen = fun () -> Lwt.fail Dummy_error in
    let p = Lwt_pool.create 1 gen in
    let u = Lwt_pool.use p (fun _ -> Lwt.return 0) in
    Lwt.return (Lwt.state u = Lwt.Fail Dummy_error)
  end;

  test "pool elements are reused" begin fun () ->
    let gen = (fun () -> let n = ref 0 in Lwt.return n) in
    let p = Lwt_pool.create 1 gen in
    let _ = Lwt_pool.use p (fun n -> n := 1; Lwt.return !n) in
    let u2 = Lwt_pool.use p (fun n -> Lwt.return !n) in
    Lwt.return (Lwt.state u2 = Lwt.Return 1)
  end;

  test "pool elements are validated when returned" begin fun () ->
    let gen = (fun () -> let n = ref 0 in Lwt.return n) in
    let v l = Lwt.return (!l = 0) in
    let p = Lwt_pool.create 1 ~validate:v gen in
    let _ = Lwt_pool.use p (fun n -> n := 1; Lwt.return !n) in
    let u2 = Lwt_pool.use p (fun n -> Lwt.return !n) in
    Lwt.return (Lwt.state u2 = Lwt.Return 0)
  end;

  test "validation exceptions are propagated to users" begin fun () ->
    let c = Lwt_condition.create () in
    let gen = (fun () -> let l = ref 0 in Lwt.return l) in
    let v l = if !l = 0 then Lwt.return true else Lwt.fail Dummy_error in
    let p = Lwt_pool.create 1 ~validate:v gen in
    let u1 = Lwt_pool.use p (fun l -> l := 1; Lwt_condition.wait c) in
    let u2 = Lwt_pool.use p (fun l -> Lwt.return !l) in
    let () = Lwt_condition.signal c "done" in
    Lwt.bind u1 (fun v1 ->
    Lwt.try_bind
      (fun () -> u2)
      (fun _ -> Lwt.return_false)
      (fun exn2 ->
        Lwt.return (v1 = "done" && exn2 = Dummy_error)))
  end;

  test "multiple creation" begin fun () ->
    let gen = (fun () -> let n = ref 0 in Lwt.return n) in
    let p = Lwt_pool.create 2 gen in
    let _ = Lwt_pool.use p (fun n -> n := 1; Lwt.pause ()) in
    let u2 = Lwt_pool.use p (fun n -> Lwt.return !n) in
    Lwt.return (Lwt.state u2 = Lwt.Return 0)
  end;

  test "users of an empty pool will wait" begin fun () ->
    let gen = (fun () -> Lwt.return 0) in
    let p = Lwt_pool.create 1 gen in
    let _ = Lwt_pool.use p (fun _ -> Lwt.pause ()) in
    let u2 = Lwt_pool.use p Lwt.return in
    Lwt.return (Lwt.state u2 = Lwt.Sleep)
  end;

  test "on check, good elements are retained" begin fun () ->
    let gen = (fun () -> let n = ref 1 in Lwt.return n) in
    let c = (fun x f -> f (!x > 0)) in
    let p = Lwt_pool.create 1 ~check: c gen in
    let _ = Lwt_pool.use p (fun n -> n := 2; Lwt.fail Dummy_error) in
    let u2 = Lwt_pool.use p (fun n -> Lwt.return !n) in
    Lwt.return (Lwt.state u2 = Lwt.Return 2)
  end;

  test "on check, bad elements are disposed of and replaced" begin fun () ->
    let gen = (fun () -> let n = ref 1 in Lwt.return n) in
    let check = (fun n f -> f (!n > 0)) in
    let disposed = ref false in
    let dispose _ = disposed := true; Lwt.return_unit in
    let p = Lwt_pool.create 1 ~check ~dispose gen in
    let task = (fun n -> incr n; Lwt.return !n) in
    let _ = Lwt_pool.use p (fun n -> n := 0; Lwt.fail Dummy_error) in
    let u2 = Lwt_pool.use p task in
    Lwt.return (Lwt.state u2 = Lwt.Return 2 && !disposed)
  end;

  test "clear disposes of all elements" begin fun () ->
    let gen = (fun () -> let n = ref 1 in Lwt.return n) in
    let count = ref 0 in
    let dispose _ = incr count; Lwt.return_unit in
    let p = Lwt_pool.create 2 ~dispose gen in
    let u = Lwt_pool.use p (fun _ -> Lwt.pause ()) in
    let _ = Lwt_pool.use p (fun _ -> Lwt.return_unit) in
    let _ = Lwt_pool.clear p in
    Lwt.bind u (fun () -> Lwt.return (!count = 2))
  end;

  test "waiter are notified on replacement" begin fun () ->
    let c = Lwt_condition.create () in
    let gen = (fun () -> let l = ref 0 in Lwt.return l) in
    let v l = if !l = 0 then Lwt.return true else Lwt.fail Dummy_error in
    let p = Lwt_pool.create 1 ~validate:v gen in
    let u1 = Lwt_pool.use p (fun l -> l := 1; Lwt_condition.wait c) in
    let u2 = Lwt_pool.use p (fun l -> Lwt.return !l) in
    let u3 = Lwt_pool.use p (fun l -> Lwt.return !l) in
    let () = Lwt_condition.signal c "done" in
    Lwt.bind u1 (fun v1 ->
    Lwt.bind u3 (fun v3 ->
    Lwt.try_bind
      (fun () -> u2)
      (fun _ -> Lwt.return_false)
      (fun exn2 ->
        Lwt.return (v1 = "done" && exn2 = Dummy_error && v3 = 0))))
  end;

  test "waiter are notified on replacement exception" begin fun () ->
    let c = Lwt_condition.create () in
    let k = ref true in
    let gen = fun () ->
      if !k then
        let l = ref 0 in Lwt.return l
      else
        Lwt.fail Dummy_error
    in
    let v l = if !l = 0 then Lwt.return true else Lwt.fail Dummy_error in
    let p = Lwt_pool.create 1 ~validate:v gen in
    let u1 = Lwt_pool.use p (fun l -> l := 1; k:= false; Lwt_condition.wait c) in
    let u2 = Lwt_pool.use p (fun l -> Lwt.return !l) in
    let u3 = Lwt_pool.use p (fun l -> Lwt.return !l) in
    let () = Lwt_condition.signal c "done" in
    Lwt.bind u1 (fun v1 ->
    Lwt.try_bind
      (fun () -> u2)
      (fun _ -> Lwt.return_false)
      (fun exn2 ->
        Lwt.try_bind
          (fun () -> u3)
          (fun _ -> Lwt.return_false)
          (fun exn3 ->
            Lwt.return
              (v1 = "done" && exn2 = Dummy_error && exn3 = Dummy_error))))
  end;

  test "check and validate can be used together" begin fun () ->
    let gen = (fun () -> let l = ref 0 in Lwt.return l) in
    let v l = Lwt.return (!l > 0) in
    let c l f = f (!l > 1) in
    let cond = Lwt_condition.create() in
    let p = Lwt_pool.create 1 ~validate:v ~check:c gen in
    let _ = Lwt_pool.use p (fun l -> l := 1; Lwt_condition.wait cond) in
    let _ = Lwt_pool.use p (fun l -> l := 2; Lwt.fail Dummy_error) in
    let u3 = Lwt_pool.use p (fun l -> Lwt.return !l) in
    let () = Lwt_condition.signal cond "done" in
    Lwt.bind u3 (fun v ->
    Lwt.return (v = 2))
  end;

  test "verify default check behavior" begin fun () ->
    let gen = (fun () -> let l = ref 0 in Lwt.return l) in
    let cond = Lwt_condition.create() in
    let p = Lwt_pool.create 1 gen in
    let _ = Lwt_pool.use p (fun l ->
      Lwt.bind (Lwt_condition.wait cond)
        (fun _ -> l:= 1; Lwt.fail Dummy_error)) in
    let u2 = Lwt_pool.use p (fun l -> Lwt.return !l) in
    let () = Lwt_condition.signal cond "done" in
    Lwt.bind u2 (fun v ->
    Lwt.return (v = 1))
  end;

  ]
