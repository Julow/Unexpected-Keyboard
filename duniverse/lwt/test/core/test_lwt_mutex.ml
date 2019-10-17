(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



open Lwt.Infix
open Test

let suite = suite "lwt_mutex" [
  (* See https://github.com/ocsigen/lwt/pull/202#issue-123451878. *)
  test "cancel"
    (fun () ->
      let mutex = Lwt_mutex.create () in

      (* Thread 1: take the mutex and wait. *)
      let thread_1_wait, resume_thread_1 = Lwt.wait () in
      let thread_1 = Lwt_mutex.with_lock mutex (fun () -> thread_1_wait) in

      (* Thread 2: block on the mutex. *)
      let thread_2_locked_mutex = ref false in
      let thread_2 =
        Lwt_mutex.lock mutex >|= fun () ->
        thread_2_locked_mutex := true
      in

      (* Cancel thread 2, and make sure it is canceled. *)
      Lwt.cancel thread_2;
      Lwt.catch
        (fun () -> thread_2 >>= fun () -> Lwt.return_false)
        (function
        | Lwt.Canceled -> Lwt.return_true
        | _ -> Lwt.return_false)
      >>= fun thread_2_canceled ->

      (* Thread 1: release the mutex. *)
      Lwt.wakeup resume_thread_1 ();
      thread_1 >>= fun () ->

      (* Thread 3: try to take the mutex. Thread 2 should not have it locked,
         since thread 2 was canceled. *)
      Lwt_mutex.lock mutex >|= fun () ->

      not !thread_2_locked_mutex && thread_2_canceled);

  (* See https://github.com/ocsigen/lwt/pull/202#issuecomment-227092595. *)
  test "cancel while queued by unlock"
    (fun () ->
      let mutex = Lwt_mutex.create () in

      (* Thread 1: take the mutex and wait. *)
      let thread_1_wait, resume_thread_1 = Lwt.wait () in
      let thread_1 = Lwt_mutex.with_lock mutex (fun () -> thread_1_wait) in

      (* Thread 2: block on the mutex, then set a flag and release it. *)
      let thread_2_waiter_executed = ref false in
      let thread_2 =
        Lwt_mutex.lock mutex >|= fun () ->
        thread_2_waiter_executed := true;
        Lwt_mutex.unlock mutex
      in

      (* Thread 3: wrap the wakeup of thread 2 in a wakeup of thread 3. *)
      let top_level_waiter, wake_top_level_waiter = Lwt.wait () in
      let while_waking =
        top_level_waiter >>= fun () ->
        (* Inside thread 3 wakeup. *)

        (* Thread 1: release the mutex. This queues thread 2 using
           wakeup_later inside Lwt_mutex.unlock. *)
        Lwt.wakeup resume_thread_1 ();
        thread_1 >>= fun () ->

        (* Confirm the mutex is now considered locked by thread 2. *)
        let mutex_passed = Lwt_mutex.is_locked mutex in
        (* Confirm thread 2 hasn't executed its bind (well, map). It is
           queued. *)
        let thread_2_was_queued = not !thread_2_waiter_executed in

        (* Try to cancel thread 2. *)
        Lwt.cancel thread_2;

        (* Complete thread 2 and check it has not been canceled. *)
        Lwt.catch
          (fun () -> thread_2 >>= fun () -> Lwt.return_false)
          (function
          | Lwt.Canceled -> Lwt.return_true
          | _ -> Lwt.return_false)
        >|= fun thread_2_canceled ->

        (* Confirm that thread 2 ran, and released the mutex. *)
        mutex_passed &&
          thread_2_was_queued &&
          not thread_2_canceled &&
          !thread_2_waiter_executed &&
          not (Lwt_mutex.is_locked mutex)
      in

      (* Run thread 3.
       * Keep this as wakeup_later to test the issue on 2.3.2 reported in
       * https://github.com/ocsigen/lwt/pull/202
       * See also:
       * https://github.com/ocsigen/lwt/pull/261
       *)
      Lwt.wakeup_later wake_top_level_waiter ();
      while_waking);
]
