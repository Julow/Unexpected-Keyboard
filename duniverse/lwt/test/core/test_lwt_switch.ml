(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



open Lwt.Infix
open Test

let suite = suite "lwt_switch" [
  test "turn_off, add_hook"
    (fun () ->
      let hook_1_calls = ref 0 in
      let hook_2_calls = ref 0 in

      let hook call_counter () =
        call_counter := !call_counter + 1;
        Lwt.return_unit
      in

      let switch = Lwt_switch.create () in
      Lwt_switch.add_hook (Some switch) (hook hook_1_calls);
      Lwt_switch.add_hook (Some switch) (hook hook_2_calls);

      let check_1 = !hook_1_calls = 0 in
      let check_2 = !hook_2_calls = 0 in

      Lwt_switch.turn_off switch >>= fun () ->

      let check_3 = !hook_1_calls = 1 in
      let check_4 = !hook_2_calls = 1 in

      Lwt_switch.turn_off switch >|= fun () ->

      let check_5 = !hook_1_calls = 1 in
      let check_6 = !hook_2_calls = 1 in

      let check_7 =
        try
          Lwt_switch.add_hook (Some switch) (fun () -> Lwt.return_unit);
          false
        with Lwt_switch.Off ->
          true
      in

      check_1 && check_2 && check_3 && check_4 && check_5 && check_6 &&
        check_7);

  test "turn_off: hook exception"
    (fun () ->
      let hook () = raise Exit in

      let switch = Lwt_switch.create () in
      Lwt_switch.add_hook (Some switch) hook;

      Lwt.catch
        (fun () -> Lwt_switch.turn_off switch >|= fun () -> false)
        (function
        | Exit -> Lwt.return_true
        | _ -> Lwt.return_false));

  test "with_switch: regular exit"
    (fun () ->
      let hook_called = ref false in

      Lwt_switch.with_switch (fun switch ->
        Lwt_switch.add_hook (Some switch) (fun () ->
          hook_called := true;
          Lwt.return_unit);

        Lwt.return_unit)

      >|= fun () -> !hook_called);

  test "with_switch: exception"
    (fun () ->
      let hook_called = ref false in
      let exception_caught = ref false in

      Lwt.catch
        (fun () ->
          Lwt_switch.with_switch (fun switch ->
            Lwt_switch.add_hook (Some switch) (fun () ->
              hook_called := true;
              Lwt.return_unit);

            raise Exit))
        (function
        | Exit ->
          exception_caught := true;
          Lwt.return_unit
        | _ ->
          Lwt.return_unit)

      >|= fun () -> !hook_called && !exception_caught);

  test "check"
    (fun () ->
      Lwt_switch.check None;

      let switch = Lwt_switch.create () in
      Lwt_switch.check (Some switch);

      Lwt_switch.turn_off switch >|= fun () ->
      try Lwt_switch.check (Some switch); false
      with Lwt_switch.Off -> true);

  test "is_on"
    (fun () ->
      let switch = Lwt_switch.create () in
      let check_1 = Lwt_switch.is_on switch in
      Lwt_switch.turn_off switch >|= fun () ->
      let check_2 = not (Lwt_switch.is_on switch) in
      check_1 && check_2);

  test "add_hook_or_exec"
    (fun () ->
      let hook_calls = ref 0 in

      let hook () =
        hook_calls := !hook_calls + 1;
        Lwt.return_unit
      in

      Lwt_switch.add_hook_or_exec None hook >>= fun () ->
      let check_1 = !hook_calls = 0 in

      let switch = Lwt_switch.create () in
      Lwt_switch.add_hook_or_exec (Some switch) hook >>= fun () ->
      let check_2 = !hook_calls = 0 in

      Lwt_switch.turn_off switch >>= fun () ->
      let check_3 = !hook_calls = 1 in

      Lwt_switch.add_hook_or_exec (Some switch) hook >|= fun () ->
      let check_4 = !hook_calls = 2 in

      check_1 && check_2 && check_3 && check_4);

  test "turn_off waits for hooks: regular exit"
    (fun () ->
      let hooks_finished = ref 0 in

      let hook () =
        Lwt.pause () >>= fun () ->
        hooks_finished := !hooks_finished + 1;
        Lwt.return_unit
      in

      let switch = Lwt_switch.create () in
      Lwt_switch.add_hook (Some switch) hook;
      Lwt_switch.add_hook (Some switch) hook;

      Lwt_switch.turn_off switch >|= fun () ->
      !hooks_finished = 2);

  test "turn_off waits for hooks: hook exception"
    (fun () ->
      let hooks_finished = ref 0 in

      let successful_hook () =
        Lwt.pause () >>= fun () ->
        hooks_finished := !hooks_finished + 1;
        Lwt.return_unit
      in

      let failing_hook () =
        hooks_finished := !hooks_finished + 1;
        raise Exit
      in

      let switch = Lwt_switch.create () in
      Lwt_switch.add_hook (Some switch) successful_hook;
      Lwt_switch.add_hook (Some switch) failing_hook;
      Lwt_switch.add_hook (Some switch) successful_hook;

      Lwt.catch
        (fun () -> Lwt_switch.turn_off switch)
        (fun _ -> Lwt.return_unit) >|= fun () ->
      !hooks_finished = 3);
]
