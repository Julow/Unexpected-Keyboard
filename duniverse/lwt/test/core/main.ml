(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



Test.run "core"
  (Test_lwt.suites @ [
    Test_lwt_stream.suite;
    Test_lwt_list.suite_primary;
    Test_lwt_list.suite_intensive;
    Test_lwt_switch.suite;
    Test_lwt_mutex.suite;
    Test_lwt_result.suite;
    Test_lwt_mvar.suite;
    Test_lwt_condition.suite;
    Test_lwt_pool.suite;
    Test_lwt_sequence.suite;
  ])
