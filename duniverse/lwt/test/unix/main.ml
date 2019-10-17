(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



let () =
  Test.concurrent "unix" [
    Test_lwt_unix.suite;
    Test_lwt_io.suite;
    Test_lwt_io_non_block.suite;
    Test_lwt_process.suite;
    Test_lwt_engine.suite;
    Test_mcast.suite;
    Test_lwt_fmt.suite;
    Test_lwt_timeout.suite;
    Test_lwt_bytes.suite;
    Test_sleep_and_timeout.suite;
  ]
