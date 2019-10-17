(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



(** Helpers for tests. *)

type test
(** Type of a test *)

type suite
(** Type of a suite of tests *)

exception Skip
(** In some tests, it is only clear that the test should be skipped after it has
    started running (for example, after an attempted system call raises a
    certain exception, indicating it is not supported).

    Such tests should raise [Test.Skip], or reject their final promise with
    [Test.Skip]. *)

val test_direct : string -> ?only_if:(unit -> bool) -> (unit -> bool) -> test
(** Defines a test. [run] must returns [true] if the test succeeded
    and [false] otherwise. [only_if] is used to conditionally skip the
    test. *)

val test :
  string ->
  ?only_if:(unit -> bool) ->
  ?sequential:bool ->
  (unit -> bool Lwt.t) ->
    test
(** Like [test_direct], but defines a test which runs a thread. *)

val suite : string -> ?only_if:(unit -> bool) -> test list -> suite
(** Defines a suite of tests *)

val run : string -> suite list -> unit
(** Run all the given tests and exit the program with an exit code
    of [0] if all tests succeeded and with [1] otherwise. *)

val concurrent : string -> suite list -> unit
(** Same as [run], but runs all the tests concurrently. *)

val with_async_exception_hook : (exn -> unit) -> (unit -> 'a Lwt.t) -> 'a Lwt.t
(** [Test.with_async_exception_hook hook f] sets [!Lwt.async_exception_hook] to
    [hook], runs [f ()], and then restores [!Lwt.async_exception_hook] to its
    former value. *)
