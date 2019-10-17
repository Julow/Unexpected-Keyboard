(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



(* [Lwt_sequence] is deprecated – we don't want users outside Lwt using it.
   However, it is still used internally by Lwt. So, briefly disable warning 3
   ("deprecated"), and create a local, non-deprecated alias for
   [Lwt_sequence] that can be referred to by the rest of the code in this
   module without triggering any more warnings. *)
[@@@ocaml.warning "-3"]
module Lwt_sequence = Lwt_sequence
[@@@ocaml.warning "+3"]

open Lwt.Infix

let enter_iter_hooks = Lwt_sequence.create ()
let leave_iter_hooks = Lwt_sequence.create ()
let yielded = Lwt_sequence.create ()

let yield () = (Lwt.add_task_r [@ocaml.warning "-3"]) yielded

let rec run t =
  (* Wakeup paused threads now. *)
  Lwt.wakeup_paused ();
  match Lwt.poll t with
  | Some x ->
    x
  | None ->
    (* Call enter hooks. *)
    Lwt_sequence.iter_l (fun f -> f ()) enter_iter_hooks;
    (* Do the main loop call. *)
    Lwt_engine.iter (Lwt.paused_count () = 0 && Lwt_sequence.is_empty yielded);
    (* Wakeup paused threads again. *)
    Lwt.wakeup_paused ();
    (* Wakeup yielded threads now. *)
    if not (Lwt_sequence.is_empty yielded) then begin
      let tmp = Lwt_sequence.create () in
      Lwt_sequence.transfer_r yielded tmp;
      Lwt_sequence.iter_l (fun wakener -> Lwt.wakeup wakener ()) tmp
    end;
    (* Call leave hooks. *)
    Lwt_sequence.iter_l (fun f -> f ()) leave_iter_hooks;
    run t

let exit_hooks = Lwt_sequence.create ()

let rec call_hooks () =
  match Lwt_sequence.take_opt_l exit_hooks with
  | None ->
    Lwt.return_unit
  | Some f ->
    Lwt.catch
      (fun () -> f ())
      (fun _  -> Lwt.return_unit) >>= fun () ->
    call_hooks ()

let () =
  at_exit (fun () ->
    Lwt.abandon_wakeups ();
    run (call_hooks ()))

let at_exit f = ignore (Lwt_sequence.add_l f exit_hooks)

module type Hooks =
sig
  type 'return_value kind
  type hook

  val add_first : (unit -> unit kind) -> hook
  val add_last : (unit -> unit kind) -> hook
  val remove : hook -> unit
  val remove_all : unit -> unit
end

module type Hook_sequence =
sig
  type 'return_value kind
  val sequence : (unit -> unit kind) Lwt_sequence.t
end

module Wrap_hooks (Sequence : Hook_sequence) =
struct
  type 'a kind = 'a Sequence.kind
  type hook = (unit -> unit Sequence.kind) Lwt_sequence.node

  let add_first hook_fn =
    let hook_node = Lwt_sequence.add_l hook_fn Sequence.sequence in
    hook_node

  let add_last hook_fn =
    let hook_node = Lwt_sequence.add_r hook_fn Sequence.sequence in
    hook_node

  let remove hook_node =
    Lwt_sequence.remove hook_node

  let remove_all () =
    Lwt_sequence.iter_node_l Lwt_sequence.remove Sequence.sequence
end

module Enter_iter_hooks =
  Wrap_hooks (struct
    type 'return_value kind = 'return_value
    let sequence = enter_iter_hooks
  end)

module Leave_iter_hooks =
  Wrap_hooks (struct
    type 'return_value kind = 'return_value
    let sequence = leave_iter_hooks
  end)

module Exit_hooks =
  Wrap_hooks (struct
    type 'return_value kind = 'return_value Lwt.t
    let sequence = exit_hooks
  end)
