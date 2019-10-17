(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



(** Main loop and event queue *)

(** This module controls the ``main-loop'' of Lwt. *)

val run : 'a Lwt.t -> 'a
  (** [run p] calls the Lwt scheduler repeatedly until [p] resolves,
      and returns the value of [p] if it is fulfilled. If [p] is rejected with
      an exception, that exception is raised.

      Every native or bytecode program that uses Lwt should always use
      this function for evaluating a promise at the top level
      (such as its main function or main loop),
      otherwise promises that depend on I/O operations will not be resolved.

      Example:
      {[
let main () = Lwt_io.write_line Lwt_io.stdout "hello world"

let () = Lwt_main.run @@ main ()
      ]}

      When targeting JavaScript, [Lwt_main.run] is not available,
      but neither it's necessary since
      the JS environment automatically takes care of the I/O considerations.


      Note that you should avoid using [run] inside threads
      - The calling threads will not resume before [run]
        returns.
      - Successive invocations of [run] are serialized: an
        invocation of [run] will not terminate before all
        subsequent invocations are terminated.

      Note also that it is not safe to call [run] in a function
      registered with [Pervasives.at_exit], use the {!at_exit}
      function of this module instead. *)

val yield : unit -> unit Lwt.t
  (** [yield ()] is a threads which suspends itself and then resumes
      as soon as possible and terminates. *)



(** Hook sequences. Each module of this type is a set of hooks, to be run by Lwt
    at certain points during execution. See modules {!Enter_iter_hooks},
    {!Leave_iter_hooks}, and {!Exit_hooks}. *)
module type Hooks =
sig
  type 'return_value kind
  (** Hooks are functions of either type [unit -> unit] or [unit -> unit Lwt.t];
      this type constructor is used only to express both possibilities in one
      signature. *)

  type hook
  (** Values of type [hook] represent hooks that have been added, so that they
      can be removed later (if needed). *)

  val add_first : (unit -> unit kind) -> hook
  (** Adds a hook to the hook sequence underlying this module, to be run
      {e first}, before any other hooks already added. *)

  val add_last : (unit -> unit kind) -> hook
  (** Adds a hook to the hook sequence underlying this module, to be run
      {e last}, after any other hooks already added. *)

  val remove : hook -> unit
  (** Removes a hook added by {!add_first} or {!add_last}. *)

  val remove_all : unit -> unit
  (** Removes all hooks from the hook sequence underlying this module. *)
end

(** Hooks, of type [unit -> unit], that are called before each iteration of the
    Lwt main loop. *)
module Enter_iter_hooks :
  Hooks with type 'return_value kind = 'return_value

(** Hooks, of type [unit -> unit], that are called after each iteration of the
    Lwt main loop. *)
module Leave_iter_hooks :
  Hooks with type 'return_value kind = 'return_value

(** Promise-returning hooks, of type [unit -> unit Lwt.t], that are called at
    process exit. Exceptions raised by these hooks are ignored. *)
module Exit_hooks :
  Hooks with type 'return_value kind = 'return_value Lwt.t



[@@@ocaml.warning "-3"]

val enter_iter_hooks : (unit -> unit) Lwt_sequence.t
  [@@ocaml.deprecated
    " Use module Lwt_main.Enter_iter_hooks."]
(** @deprecated Use module {!Enter_iter_hooks}. *)

val leave_iter_hooks : (unit -> unit) Lwt_sequence.t
  [@@ocaml.deprecated
    " Use module Lwt_main.Leave_iter_hooks."]
(** @deprecated Use module {!Leave_iter_hooks}. *)

val exit_hooks : (unit -> unit Lwt.t) Lwt_sequence.t
  [@@ocaml.deprecated
    " Use module Lwt_main.Exit_hooks."]
(** @deprecated Use module {!Exit_hooks}. *)

[@@@ocaml.warning "+3"]



val at_exit : (unit -> unit Lwt.t) -> unit
(** [Lwt_main.at_exit hook] is the same as
    [ignore (Lwt_main.Exit_hooks.add_first hook)]. *)
