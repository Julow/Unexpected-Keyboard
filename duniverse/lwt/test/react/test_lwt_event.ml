(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



open Test
open Lwt

let suite = suite "lwt_event" [
  test "to_stream"
    (fun () ->
       let event, push = React.E.create () in
       let stream = Lwt_react.E.to_stream event in
       let t =  Lwt_stream.next stream in
       assert (state t = Sleep);
       push 42;
       return (state t = Return 42));

  test "to_stream 2"
    (fun () ->
       let event, push = React.E.create () in
       let stream = Lwt_react.E.to_stream event in
       push 1;
       push 2;
       push 3;
       Lwt.bind (Lwt_stream.nget 3 stream) (fun l ->
       return (l = [1; 2; 3])));

  test "map_s"
    (fun () ->
       let l = ref [] in
       let event, push = React.E.create () in
       let event' = Lwt_react.E.map_s (fun x -> l := x :: !l; return ()) event in
       ignore event';
       push 1;
       return (!l = [1]));

  test "map_p"
    (fun () ->
       let l = ref [] in
       let event, push = React.E.create () in
       let event' = Lwt_react.E.map_p (fun x -> l := x :: !l; return ()) event in
       ignore event';
       push 1;
       return (!l = [1]));

  test "limit_race"
    (fun () ->
       let l = ref [] in
       let event, push = Lwt_react.E.create() in
       let prepend n = l := n :: !l
       in
       event
       |> Lwt_react.E.limit (fun () ->
           let p = Lwt_unix.sleep 1. in
           Lwt.async (fun () ->
               Lwt_unix.sleep 0.1 >|= fun () ->
               Lwt.on_success p (fun () -> push 2)); p)
       |> React.E.map prepend
       |> ignore;
       push 0;
       push 1;

       Lwt_main.run (Lwt_unix.sleep 2.5);
       return (!l = [2;2;0]));


  test "of_stream"
    (fun () ->
       let stream, push = Lwt_stream.create () in
       let l = ref [] in
       let event = React.E.map (fun x -> l := x :: !l) (Lwt_react.E.of_stream stream) in
       ignore event;
       push (Some 1);
       push (Some 2);
       push (Some 3);
       Lwt.wakeup_paused ();
       return (!l = [3; 2; 1]));

  test "limit"
    (fun () ->
       let event, push = React.E.create () in
       let cond        = Lwt_condition.create () in
       let event'      = Lwt_react.E.limit (fun () -> Lwt_condition.wait cond) event in
       let l           = ref [] in
       let event''     = React.E.map (fun x -> l := x :: !l) event' in
         ignore event';
         ignore event'';
         push 1;
         push 0;
         push 2; (* overwrites previous 0 *)
         Lwt_condition.signal cond ();
         push 3;
         Lwt_condition.signal cond ();
         push 4;
         Lwt_condition.signal cond ();
         return (!l = [4; 3; 2; 1]));

  test "with_finaliser lifetime" begin fun () ->
    let e, push = React.E.create () in
    let finalizer_ran = ref false in
    let e' = Lwt_react.E.with_finaliser (fun () -> finalizer_ran := true) e in

    Gc.full_major ();
    let check1 = !finalizer_ran = false in

    let p = Lwt_react.E.next e' in
    push ();
    p >>= fun () ->

    Gc.full_major ();
    let check2 = !finalizer_ran = true in

    Lwt.return (check1 && check2)
  end;
]
