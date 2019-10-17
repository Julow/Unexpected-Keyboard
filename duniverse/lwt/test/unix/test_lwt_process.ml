(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



open Test
open Lwt.Infix

let suite = suite "lwt_process" [
  (* The sleep command is not available on Win32. *)
  test "lazy_undefined" ~only_if:(fun () -> not Sys.win32)
    (fun () ->
      Lwt_process.with_process_in
        ~timeout:1. ("sleep", [| "sleep"; "2" |])
          (fun p ->
            Lwt.catch
              (fun () -> Lwt_io.read p#stdout)
              (fun _ -> Lwt.return ""))
        >>= fun _ -> Lwt.return_true)
]
