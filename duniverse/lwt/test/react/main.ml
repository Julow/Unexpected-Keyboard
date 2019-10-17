(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



Test.run "react" [
  Test_lwt_event.suite;
  Test_lwt_signal.suite;
]
