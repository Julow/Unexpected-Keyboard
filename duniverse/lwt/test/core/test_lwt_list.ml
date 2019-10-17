(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



open Test
open Lwt.Infix

let (<=>) v v' =
  assert (Lwt.state v = v')

let test_iter f test_list =
  let incr_ x = Lwt.return (incr x) in
  let () =
    let l = [ref 0; ref 0; ref 0] in
    let t = f incr_ l in
    t <=> Lwt.Return ();
    List.iter2 (fun v r -> assert (v = !r)) [1; 1; 1] l
  in
  let () =
    let l = [ref 0; ref 0; ref 0] in
    let t, w = Lwt.wait () in
    let r = ref [incr_; (fun x -> t >>= (fun () -> incr_ x)); incr_] in
    let t' = f (fun x ->
      let f = List.hd !r in
      let t = f x in
      r := List.tl !r;
      t) l
    in
    t' <=> Sleep;
    List.iter2 (fun v r -> assert (v = !r)) test_list l;
    Lwt.wakeup w ();
    List.iter2 (fun v r -> assert (v = !r)) [1; 1; 1] l;
    t' <=> Lwt.Return ()
  in
  ()

let test_exception list_combinator =
  (* This really should be a local exception, but local exceptions require OCaml
     4.04, while Lwt still supports, and is tested on, 4.02. *)
  let module E =
    struct
      exception Exception
    end
  in
  let open E in

  let number_of_callback_calls = ref 0 in

  let callback _ =
    incr number_of_callback_calls;
    match !number_of_callback_calls with
    | 2 -> raise Exception
    | _ -> Lwt.return ()
  in

  (* Even though the callback will raise immediately for one of the list
     elements, we expect the final promise that represents the entire list
     operation to be created (and rejected with the raised exception). The
     raised exception should not be leaked up past the creation of the
     promise. *)
  let p =
    try
      list_combinator callback [(); (); ()]
    with _exn ->
      assert false
  in

  (* Check that the promise was rejected with the expected exception. *)
  assert (Lwt.state p = Lwt.Fail Exception)

let test_map f test_list =
  let t, w = Lwt.wait () in
  let t', _ = Lwt.task () in
  let get =
    let r = ref 0 in
    let c = ref 0 in
    fun () ->
      let th =
        incr c;
        match !c with
        | 5 -> t
        | 8 -> t'
        | _ -> Lwt.return ()
      in
      th >>= (fun () ->
        incr r;
        Lwt.return (!r))
  in
  let () =
    let l = [(); (); ()] in
    let t1 = f get l in
    t1 <=> Lwt.Return [1; 2; 3];
    let t2 = f get l in
    t2 <=> Lwt.Sleep;
    let t3 = f get l in
    t3 <=> Lwt.Sleep;
    Lwt.cancel t';
    t3 <=> Lwt.Fail Lwt.Canceled;
    Lwt.wakeup w ();
    t2 <=> Lwt.Return test_list;
  in
  ()

let test_parallelism map =
  let t, w = Lwt.wait () in
  let g _ =
    Lwt.wakeup_later w ();
    Lwt.return () in
  let f x =
    if x = 0 then t >>= (fun _ -> Lwt.return ())
    else g x
  in
  let p = map f [0; 1] in
  p >>= (fun _ -> Lwt.return_true)

let test_serialization ?(rev=false) map =
  let other_ran = ref false in
  let k = if rev then 1 else 0 in
  let f x =
    if x = k then
      Lwt.pause () >>= fun () ->
      assert(not !other_ran);
      Lwt.return ()
    else begin
      other_ran := true;
      Lwt.return ()
    end
  in
  let p = map f [0; 1] in
  p >>= (fun _ -> Lwt.return_true)

let test_for_all_true f =
  let l = [true; true] in
  f (fun x -> Lwt.return (x = true)) l

let test_for_all_false f =
  let l = [true; true] in
  f (fun x -> Lwt.return (x = false)) l >>= fun b ->
  Lwt.return (not b)

let test_exists_true f =
  let l = [true; false] in
  f (fun x -> Lwt.return (x = true)) l >>= fun b ->
  Lwt.return b

let test_exists_false f =
  let l = [true; true] in
  f (fun x -> Lwt.return (x = false)) l >>= fun b ->
  Lwt.return (not b)

let test_filter f =
  let l = [1; 2; 3; 4] in
  f (fun x -> Lwt.return (x mod 2 = 0)) l >>= fun after ->
  Lwt.return (after = [2; 4])

let test_partition f =
  let l = [1; 2; 3; 4] in
  f (fun x -> Lwt.return (x <= 2)) l >>= fun (a, b) ->
  Lwt.return (a = [1; 2] && b = [3; 4])

let test_filter_map f =
  let l = [1; 2; 3; 4] in
  let fn = (fun x ->
    if x mod 2 = 0 then Lwt.return_some (x * 2) else Lwt.return_none) in
  f fn l >>= fun after ->
  Lwt.return (after = [4; 8])

let test_iter_i f =
  let count = ref 0 in
  let l = [1; 2; 3] in
  f (fun i n -> count := !count + i + n; Lwt.return_unit) l >>= fun () ->
  Lwt.return (!count = 9)

let test_map_i f =
  let l = [0; 0; 0] in
  f (fun i n -> Lwt.return (i + n)) l >>= fun after ->
  Lwt.return (after = [0; 1; 2])

let test_rev_map f =
  let l = [1; 2; 3] in
  f (fun n -> Lwt.return (n * 2)) l >>= fun after ->
  Lwt.return (after = [6; 4; 2])

let suite_primary = suite "lwt_list" [
  test "iter_p" begin fun () ->
    test_iter Lwt_list.iter_p [1; 0; 1];
    test_exception Lwt_list.iter_p;
    Lwt.return true
  end;

  test "iter_s" begin fun () ->
    test_iter Lwt_list.iter_s [1; 0; 0];
    test_exception Lwt_list.iter_s;
    Lwt.return true
  end;

  test "map_p" begin fun () ->
    test_map Lwt_list.map_p [4; 8; 5];
    test_exception Lwt_list.map_p;
    Lwt.return true
  end;

  test "map_s" begin fun () ->
    test_map Lwt_list.map_s [4; 7; 8];
    test_exception Lwt_list.map_s;
    Lwt.return true
  end;

  test "fold_left_s" begin fun () ->
    let l = [1; 2; 3] in
    let f acc v = Lwt.return (v::acc) in
    let t = Lwt_list.fold_left_s f [] l in
    t <=> Lwt.Return (List.rev l);
    Lwt.return true
  end;

  test "for_all_s"
    (fun () -> test_for_all_true Lwt_list.for_all_s);

  test "for_all_p"
    (fun () -> test_for_all_true Lwt_list.for_all_p);

  test "exists_s true"
    (fun () -> test_exists_true Lwt_list.exists_s);

  test "exists_p true"
    (fun () -> test_exists_true Lwt_list.exists_p);

  test "exists_s false"
    (fun () -> test_exists_false Lwt_list.exists_s);

  test "exists_p false"
    (fun () -> test_exists_false Lwt_list.exists_p);

  test "filter_s"
    (fun () -> test_filter Lwt_list.filter_s);

  test "filter_p"
    (fun () -> test_filter Lwt_list.filter_p);

  test "partition_p"
    (fun () -> test_partition Lwt_list.partition_p);

  test "partition_s"
    (fun () -> test_partition Lwt_list.partition_s);

  test "filter_map_p"
    (fun () -> test_filter_map Lwt_list.filter_map_p);

  test "filter_map_s"
    (fun () -> test_filter_map Lwt_list.filter_map_s);

  test "iteri_p"
    (fun () -> test_iter_i Lwt_list.iteri_p);

  test "iteri_s"
    (fun () -> test_iter_i Lwt_list.iteri_s);

  test "mapi_p"
    (fun () -> test_map_i Lwt_list.mapi_p);

  test "mapi_s"
    (fun () -> test_map_i Lwt_list.mapi_s);

  test "find_s existing" begin fun () ->
    let l = [1; 2; 3] in
    Lwt_list.find_s (fun n -> Lwt.return ((n mod 2) = 0)) l >>= fun result ->
    Lwt.return (result = 2)
  end;

  test "find_s missing" begin fun () ->
    let l = [1; 3] in
    Lwt.catch
      (fun () ->
        Lwt_list.find_s (fun n ->
          Lwt.return ((n mod 2) = 0)) l >>= fun _result ->
        Lwt.return false)
      (function
        | Not_found -> Lwt.return true
        | _ -> Lwt.return false)
  end;

  test "rev_map_p"
    (fun () -> test_rev_map Lwt_list.rev_map_p);

  test "rev_map_s"
    (fun () -> test_rev_map Lwt_list.rev_map_s);

  test "fold_right_s" begin fun () ->
    let l = [1; 2; 3] in
    Lwt_list.fold_right_s (fun a n -> Lwt.return (a + n)) l 0 >>= fun result ->
    Lwt.return (result = 6)
  end;

  test "iteri_p exception" begin fun () ->
    let i f = Lwt_list.iteri_p (fun _ x -> f x) in
    test_exception i;
    Lwt.return true
  end;

  test "iteri_s exception" begin fun () ->
    let i f = Lwt_list.iteri_s (fun _ x -> f x) in
    test_exception i;
    Lwt.return true
  end;

  test "map_s exception" begin fun () ->
    test_exception Lwt_list.map_s;
    Lwt.return true
  end;

  test "map_p exception" begin fun () ->
    test_exception Lwt_list.map_p;
    Lwt.return true
  end;

  test "mapi_s exception" begin fun () ->
    let m f = Lwt_list.mapi_s (fun _ x -> f x) in
    test_exception m;
    Lwt.return true
  end;

  test "mapi_p exception" begin fun () ->
    let m f = Lwt_list.mapi_p (fun _ x -> f x) in
    test_exception m;
    Lwt.return true
  end;

  test "rev_map_s exception" begin fun () ->
    test_exception Lwt_list.rev_map_s;
    Lwt.return true
  end;

  test "rev_map_p exception" begin fun () ->
    test_exception Lwt_list.rev_map_p;
    Lwt.return true
  end;

  test "fold_left_s exception" begin fun () ->
    let m f = Lwt_list.fold_left_s (fun _ x -> f x) () in
    test_exception m;
    Lwt.return true
  end;

  test "fold_right_s exception" begin fun() ->
    let m f l = Lwt_list.fold_right_s (fun x _ -> f x) l () in
    test_exception m;
    Lwt.return true
  end;

  test "for_all_p exception" begin fun () ->
    let m f =
      Lwt_list.for_all_p (fun x -> f x >>= (fun _ -> Lwt.return_true)) in
    test_exception m;
    Lwt.return true
  end;

  test "for_all_s exception" begin fun () ->
    let m f =
      Lwt_list.for_all_s (fun x -> f x >>= (fun _ -> Lwt.return_true)) in
    test_exception m;
    Lwt.return true
  end;

  test "exists_p exception" begin fun () ->
    let m f =
      Lwt_list.exists_p (fun x -> f x >>= (fun _ -> Lwt.return_false)) in
    test_exception m;
    Lwt.return true
  end;

  test "exists_s exception" begin fun () ->
    let m f =
      Lwt_list.exists_s (fun x -> f x >>= (fun _ -> Lwt.return_false)) in
    test_exception m;
    Lwt.return true
  end;

  test "find_s exception" begin fun () ->
    let m f = Lwt_list.find_s (fun x -> f x >>= (fun _ -> Lwt.return_false)) in
    test_exception m;
    Lwt.return true
  end;

  test "filter_p exception" begin fun () ->
    let m f =
      Lwt_list.filter_p (fun x -> f x >>= (fun _ -> Lwt.return_false)) in
    test_exception m;
    Lwt.return true;
  end;

  test "filter_s exception" begin fun () ->
    let m f =
      Lwt_list.filter_s (fun x -> f x >>= (fun _ -> Lwt.return_false)) in
    test_exception m;
    Lwt.return true;
  end;

  test "filter_map_p exception" begin fun () ->
    let m f =
      Lwt_list.filter_map_p (fun x -> f x >>= (fun _ -> Lwt.return (Some ())))
    in
    test_exception m;
    Lwt.return true;
  end;

  test "filter_map_s exception" begin fun () ->
    let m f =
      Lwt_list.filter_map_s (fun x -> f x >>= (fun _ -> Lwt.return (Some ())))
    in
    test_exception m;
    Lwt.return true;
  end;

  test "partition_p exception" begin fun () ->
    let m f =
      Lwt_list.partition_p (fun x -> f x >>= (fun _ -> Lwt.return_false)) in
    test_exception m;
    Lwt.return true;
  end;

  test "partition_s exception" begin fun () ->
    let m f =
      Lwt_list.partition_s (fun x -> f x >>= (fun _ -> Lwt.return_false)) in
    test_exception m;
    Lwt.return true;
  end;

  test "iter_p parallelism" begin fun () ->
    test_parallelism Lwt_list.iter_p
  end;

  test "iter_s serialization" begin fun () ->
    test_serialization Lwt_list.iter_s
  end;

  test "iteri_p parallelism" begin fun () ->
    let iter f = Lwt_list.iteri_p (fun _ x -> f x) in
    test_parallelism iter
  end;

  test "iteri_s serialization" begin fun () ->
    let iter f = Lwt_list.iteri_s (fun _ x -> f x) in
    test_serialization iter
  end;

  test "map_p parallelism" begin fun () ->
    test_parallelism Lwt_list.map_p
  end;

  test "map_s serialization" begin fun () ->
    test_serialization Lwt_list.map_s
  end;

  test "mapi_p parallelism" begin fun () ->
    let m f = Lwt_list.mapi_p (fun _ x -> f x) in
    test_parallelism m
  end;

  test "mapi_s serialization" begin fun () ->
    let m f = Lwt_list.mapi_s (fun _ x -> f x) in
    test_serialization m
  end;

  test "rev_map_p parallelism" begin fun () ->
    test_parallelism Lwt_list.rev_map_p
  end;

  test "rev_map_s serialization" begin fun () ->
    test_serialization Lwt_list.rev_map_s
  end;

  test "fold_left_s serialization" begin fun () ->
    let m f =
      Lwt_list.fold_left_s (fun _ x -> f x >>= fun _ -> Lwt.return ()) () in
    test_serialization m
  end;

  test "fold_right_s serialization" begin fun () ->
    let m f l =
      Lwt_list.fold_right_s (fun x _ -> f x >>= fun _ -> Lwt.return ()) l () in
    test_serialization ~rev:true m
  end;

  test "filter_map_p parallelism" begin fun () ->
    let m f =
      Lwt_list.filter_map_p (fun x -> f x >>= fun u -> Lwt.return (Some u)) in
    test_parallelism m
  end;

  test "filter_map_s serlialism" begin fun () ->
    let m f =
      Lwt_list.filter_map_s (fun x -> f x >>= fun u -> Lwt.return (Some u)) in
    test_serialization m
  end;

  test "for_all_p parallelism" begin fun () ->
    let m f = Lwt_list.for_all_p (fun x -> f x >>= fun _ -> Lwt.return_true) in
    test_parallelism m
  end;

  test "for_all_s serialization" begin fun () ->
    let m f = Lwt_list.for_all_s (fun x -> f x >>= fun _ -> Lwt.return_true) in
    test_serialization m
  end;

  test "exists_p parallelism" begin fun () ->
    let m f = Lwt_list.exists_p (fun x -> f x >>= fun _ -> Lwt.return_false) in
    test_parallelism m
  end;

  test "exists_s serialization" begin fun () ->
    let m f = Lwt_list.exists_s (fun x -> f x >>= fun _ -> Lwt.return_false) in
    test_serialization m
  end;

  test "find_s serialization" begin fun () ->
    let m f = Lwt_list.find_s (fun x -> f x >>= fun _ -> Lwt.return_false) in
    let handler e =
      if e = Not_found then Lwt.return_true
      else Lwt.return_false
    in
    Lwt.catch (fun () -> test_serialization m) handler
  end;

  test "filter_p parallelism" begin fun () ->
    let m f = Lwt_list.filter_p (fun x -> f x >>= fun _ -> Lwt.return_true) in
    test_parallelism m
  end;

  test "filter_s serialization" begin fun () ->
    let m f = Lwt_list.filter_s (fun x -> f x >>= fun _ -> Lwt.return_true) in
    test_serialization m
  end;


  test "filter_map_s serialization" begin fun () ->
    let m f =
      Lwt_list.filter_map_s (fun x -> f x >>= fun u -> Lwt.return (Some u)) in
    test_serialization m
  end;

  test "partition_p parallelism" begin fun () ->
    let m f l =
      Lwt_list.partition_p (fun x -> f x >>= fun _ -> Lwt.return true) l in
    test_parallelism m
  end;

  test "partition_s serialization" begin fun () ->
    let m f l =
      Lwt_list.partition_s (fun x -> f x >>= fun _ -> Lwt.return true) l in
    test_serialization m
  end;
]

let test_big_list m =
  let make_list n = Array.to_list @@ Array.init n (fun x -> x) in
  let f _ = Lwt.return () in
  m f (make_list 10_000_000) >>= (fun _ -> Lwt.return_true)

let suite_intensive = suite "lwt_list big lists"
    ~only_if:(fun () ->
        try Sys.getenv "LWT_STRESS_TEST" = "true" with
        | Not_found -> false) [
  test "iter_p big list" begin fun () ->
    test_big_list Lwt_list.iter_p
  end;

  test "iter_s big list" begin fun () ->
    test_big_list Lwt_list.iter_s
  end;

  test "iteri_p big list" begin fun () ->
    let iter f = Lwt_list.iteri_p (fun _ x -> f x) in
    test_big_list iter
  end;

  test "iteri_s big list" begin fun () ->
    let iter f = Lwt_list.iteri_s (fun _ x -> f x) in
    test_serialization iter
  end;

  test "map_p big list" begin fun () ->
    test_big_list Lwt_list.map_p
  end;

  test "map_s big list" begin fun () ->
    test_serialization Lwt_list.map_s
  end;

  test "mapi_p big list" begin fun () ->
    let m f = Lwt_list.mapi_p (fun _ x -> f x) in
    test_big_list m
  end;

  test "mapi_s big list" begin fun () ->
    let m f = Lwt_list.mapi_s (fun _ x -> f x) in
    test_big_list m
  end;

  test "rev_map_p big list" begin fun () ->
    test_big_list Lwt_list.rev_map_p
  end;

  test "rev_map_s big list" begin fun () ->
    test_big_list Lwt_list.rev_map_s
  end;

  test "fold_left_s big list" begin fun () ->
    let m f =
      Lwt_list.fold_left_s (fun _ x -> f x >>= fun _ -> Lwt.return ()) () in
    test_big_list m
  end;

  test "fold_right_s big list" begin fun () ->
    let m f l =
      Lwt_list.fold_right_s (fun x _ -> f x >>= fun _ -> Lwt.return ()) l () in
    test_big_list m
  end;

  test "for_all_p big list" begin fun () ->
    let m f = Lwt_list.for_all_p (fun x -> f x >>= fun _ -> Lwt.return_true) in
    test_big_list m
  end;

  test "for_all_s big list" begin fun () ->
    let m f = Lwt_list.for_all_s (fun x -> f x >>= fun _ -> Lwt.return_true) in
    test_big_list m
  end;

  test "exists_p big list" begin fun () ->
    let m f = Lwt_list.exists_p (fun x -> f x >>= fun _ -> Lwt.return_false) in
    test_big_list m
  end;

  test "exists_s big list" begin fun () ->
    let m f = Lwt_list.exists_s (fun x -> f x >>= fun _ -> Lwt.return_false) in
    test_big_list m
  end;

  test "find_s big list" begin fun () ->
    let m f = Lwt_list.find_s (fun x -> f x >>= fun _ -> Lwt.return_false) in
    let handler e =
      if e = Not_found then Lwt.return_true
      else Lwt.return_false
    in
    Lwt.catch (fun () -> test_big_list m) handler
  end;

  test "filter_p big list" begin fun () ->
    let m f = Lwt_list.filter_p (fun x -> f x >>= fun _ -> Lwt.return_true) in
    test_big_list m
  end;

  test "filter_s big list" begin fun () ->
    let m f = Lwt_list.filter_s (fun x -> f x >>= fun _ -> Lwt.return_true) in
    test_big_list m
  end;

  test "filter_map_p big list" begin fun () ->
    let m f =
      Lwt_list.filter_map_p (fun x -> f x >>= fun u -> Lwt.return (Some u)) in
    test_big_list m
  end;

  test "filter_map_s big list" begin fun () ->
    let m f =
      Lwt_list.filter_map_s (fun x -> f x >>= fun u -> Lwt.return (Some u)) in
    test_big_list m
  end;

  test "partition_p big list" begin fun () ->
    let m f l =
      Lwt_list.partition_p (fun x -> f x >>= fun _ -> Lwt.return true) l in
    test_big_list m
  end;

  test "partition_s big list" begin fun () ->
    let m f l =
      Lwt_list.partition_s (fun x -> f x >>= fun _ -> Lwt.return true) l in
    test_big_list m
  end;
]
