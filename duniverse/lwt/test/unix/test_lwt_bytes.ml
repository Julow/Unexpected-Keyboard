(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)

open Lwt.Infix
open Test

let bytes_equal (b1:Bytes.t) (b2:Bytes.t) = b1 = b2

let tcp_server_client_exchange server_logic client_logic =
  let server_is_ready, notify_server_is_ready = Lwt.wait () in
  let server () =
    let sock = Lwt_unix.socket Lwt_unix.PF_INET Lwt_unix.SOCK_STREAM 0 in
    let sockaddr = Lwt_unix.ADDR_INET (Unix.inet_addr_loopback, 0) in
    Lwt_unix.bind sock sockaddr
    >>= fun () ->
    let server_address = Lwt_unix.getsockname sock in
    let () = Lwt_unix.listen sock 5 in
    Lwt.wakeup_later notify_server_is_ready server_address;
    Lwt_unix.accept sock
    >>= fun (fd_client, _) ->
    server_logic fd_client
    >>= fun _n -> Lwt_unix.close fd_client
    >>= fun () -> Lwt_unix.close sock
  in
  let client () =
    server_is_ready
    >>= fun sockaddr ->
    let sock = Lwt_unix.socket Lwt_unix.PF_INET Lwt_unix.SOCK_STREAM 0 in
    Lwt_unix.connect sock sockaddr
    >>= fun () ->
    client_logic sock
    >>= fun _n -> Lwt_unix.close sock
  in
  Lwt.join [client (); server ()]

let udp_server_client_exchange server_logic client_logic =
  let server_is_ready, notify_server_is_ready = Lwt.wait () in
  let server () =
    let sock = Lwt_unix.socket Lwt_unix.PF_INET Lwt_unix.SOCK_DGRAM 0 in
    let sockaddr = Lwt_unix.ADDR_INET (Unix.inet_addr_loopback, 0) in
    Lwt_unix.bind sock sockaddr
    >>= fun () ->
    let server_address = Lwt_unix.getsockname sock in
    Lwt.wakeup_later notify_server_is_ready server_address;
    server_logic sock
    >>= fun (_n, _sockaddr) -> Lwt_unix.close sock
  in
  let client () =
    server_is_ready
    >>= fun sockaddr ->
    let sock = Lwt_unix.socket Lwt_unix.PF_INET Lwt_unix.SOCK_DGRAM 0 in
    client_logic sock sockaddr
    >>= fun (_n) -> Lwt_unix.close sock
  in
  Lwt.join [client (); server ()]

let gen_buf n =
  let buf = Lwt_bytes.create n in
  let () = Lwt_bytes.fill buf 0 n '\x00' in
  buf

(* The two following helpers only focus on the behavior of
 * Lwt_bytes.mincore and Lwt_bytes.wait_mincore with different arguments that
 * represents correct or bad bounds.
 *
 * The main purposes of those functions are not tested.
 * *)

let file_suffix =
  let last_file_suffix = ref 0 in
  fun () ->
    incr last_file_suffix;
    !last_file_suffix

let test_mincore buff_len offset n_states =
  let test_file = Printf.sprintf "bytes_mincore_write_%i" (file_suffix ()) in
  Lwt_unix.openfile test_file [O_RDWR;O_TRUNC; O_CREAT] 0o666
  >>= fun fd ->
  let buf_write = gen_buf buff_len in
  Lwt_bytes.write fd buf_write 0 buff_len
  >>= fun _n ->
  Lwt_unix.close fd
  >>= fun () ->
  let fd = Unix.openfile test_file [O_RDONLY] 0 in
  let shared = false in
  let size = buff_len in
  let buffer = Lwt_bytes.map_file ~fd ~shared ~size () in
  let states = Array.make n_states false in
  let () = Lwt_bytes.mincore buffer offset states in
  Lwt.return ()

let test_wait_mincore buff_len offset =
  let test_file = Printf.sprintf "bytes_mincore_write_%i" (file_suffix ()) in
  Lwt_unix.openfile test_file [O_RDWR;O_TRUNC; O_CREAT] 0o666
  >>= fun fd ->
  let buf_write = gen_buf buff_len in
  Lwt_bytes.write fd buf_write 0 buff_len
  >>= fun _n ->
  Lwt_unix.close fd
  >>= fun () ->
  let fd = Unix.openfile test_file [O_RDONLY] 0 in
  let shared = false in
  let size = buff_len in
  let buffer = Lwt_bytes.map_file ~fd ~shared ~size () in
  Lwt_bytes.wait_mincore buffer offset

let suite = suite "lwt_bytes" [
    test "create" begin fun () ->
      let len = 5 in
      let buff = Lwt_bytes.create len in
      let len' = Bigarray.Array1.dim buff in
      Lwt.return (len = len')
    end;

    test "get/set" begin fun () ->
      let buff = Lwt_bytes.create 4 in
      let () = Lwt_bytes.set buff 0 'a' in
      let () = Lwt_bytes.set buff 1 'b' in
      let () = Lwt_bytes.set buff 2 'c' in
      let check = Lwt_bytes.get buff 0 = 'a' &&
                  Lwt_bytes.get buff 1 = 'b' &&
                  Lwt_bytes.get buff 2 = 'c'
      in Lwt.return check
    end;

    test "get out of bounds : lower limit" begin fun () ->
      let buff = Lwt_bytes.create 3 in
      try
        let _ = Lwt_bytes.get buff (-1) in
        Lwt.return false
      with
      | Invalid_argument _ -> Lwt.return true
      | _ -> Lwt.return false
    end;

    test "get out of bounds : upper limit" begin fun () ->
      let buff = Lwt_bytes.create 3 in
      try
        let _ = Lwt_bytes.get buff 3 in
        Lwt.return false
      with
      | Invalid_argument _ -> Lwt.return true
      | _ -> Lwt.return false
    end;

    test "set out of bounds : lower limit" begin fun () ->
      let buff = Lwt_bytes.create 3 in
      try
        let () = Lwt_bytes.set buff (-1) 'a' in
        Lwt.return true
      with
      | Invalid_argument _ -> Lwt.return true
      | _ -> Lwt.return false
    end;

    test "set out of bounds : upper limit" begin fun () ->
      let buff = Lwt_bytes.create 3 in
      try
        let () = Lwt_bytes.set buff 3 'a' in
        Lwt.return true
      with
      | Invalid_argument _ -> Lwt.return true
      | _ -> Lwt.return false
    end;

    test "unsafe_get/unsafe_set" begin fun () ->
      let buff = Lwt_bytes.create 4 in
      let () = Lwt_bytes.unsafe_set buff 0 'a' in
      let () = Lwt_bytes.unsafe_set buff 1 'b' in
      let () = Lwt_bytes.unsafe_set buff 2 'c' in
      let check = Lwt_bytes.unsafe_get buff 0 = 'a' &&
                  Lwt_bytes.unsafe_get buff 1 = 'b' &&
                  Lwt_bytes.unsafe_get buff 2 = 'c'
      in Lwt.return check
    end;

    test "of bytes" begin fun () ->
      let bytes = Bytes.of_string "abc" in
      let buff = Lwt_bytes.of_bytes bytes in
      let check = Lwt_bytes.get buff 0 = Bytes.get bytes 0 &&
                  Lwt_bytes.get buff 1 = Bytes.get bytes 1 &&
                  Lwt_bytes.get buff 2 = Bytes.get bytes 2
      in Lwt.return check
    end;

    test "of string" begin fun () ->
      let buff = Lwt_bytes.of_string "abc" in
      let check = Lwt_bytes.get buff 0 = 'a' &&
                  Lwt_bytes.get buff 1 = 'b' &&
                  Lwt_bytes.get buff 2 = 'c'
      in Lwt.return check
    end;

    test "to bytes" begin fun () ->
      let bytes = Bytes.of_string "abc" in
      let buff = Lwt_bytes.of_bytes bytes in
      let bytes' = Lwt_bytes.to_bytes buff in
      let check = bytes_equal bytes bytes' in
      Lwt.return check
    end;

    test "to string" begin fun () ->
      let str = "abc" in
      let buff = Lwt_bytes.of_string str in
      let str' = Lwt_bytes.to_string buff in
      let check = str = str' in
      Lwt.return check
    end;

    test "blit" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      let () = Lwt_bytes.blit buf1 0 buf2 3 3 in
      let check = "abcabc" = Lwt_bytes.to_string buf2 in
      Lwt.return check
    end;

    test "blit source out of bounds: lower limit" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      try
        let() = Lwt_bytes.blit buf1 (-1) buf2 3 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return true
      | _ -> Lwt.return false
    end;

    test "blit source out of bounds: upper limit" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      try
        let() = Lwt_bytes.blit buf1 1 buf2 3 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return true
      | _ -> Lwt.return false
    end;

    test "blit destination out of bounds: lower limit" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      try
        let() = Lwt_bytes.blit buf1 0 buf2 (-1) 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return true
      | _ -> Lwt.return false
    end;

    test "blit destination out of bounds: upper limit" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      try
        let() = Lwt_bytes.blit buf1 0 buf2 4 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return true
      | _ -> Lwt.return false
    end;

    test "blit length out of bounds: lower limit" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      try
        let() = Lwt_bytes.blit buf1 0 buf2 3 (-1) in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return true
      | _ -> Lwt.return false
    end;

    test "blit from bytes" begin fun () ->
      let bytes1 = Bytes.of_string "abc" in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      let () = Lwt_bytes.blit_from_bytes bytes1 0 buf2 3 3 in
      let check = "abcabc" = Lwt_bytes.to_string buf2 in
      Lwt.return check
    end;

    test "blit from bytes source out of bounds: lower limit" begin fun () ->
      let bytes1 = Bytes.of_string "abc" in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      try
        let () = Lwt_bytes.blit_from_bytes bytes1 (-1) buf2 3 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "blit from bytes source out of bounds: upper limit" begin fun () ->
      let bytes1 = Bytes.of_string "abc" in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      try
        let () = Lwt_bytes.blit_from_bytes bytes1 1 buf2 3 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "blit from bytes destination out of bounds: lower limit" begin fun () ->
      let bytes1 = Bytes.of_string "abc" in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      try
        let () = Lwt_bytes.blit_from_bytes bytes1 0 buf2 (-1) 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "blit from bytes destination out of bounds: upper limit" begin fun () ->
      let bytes1 = Bytes.of_string "abc" in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      try
        let () = Lwt_bytes.blit_from_bytes bytes1 0 buf2 4 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "blit from bytes length out of bounds: lower limit" begin fun () ->
      let bytes1 = Bytes.of_string "abc" in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      try
        let () = Lwt_bytes.blit_from_bytes bytes1 0 buf2 3 (-1) in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "blit to bytes" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let bytes2 = Bytes.of_string str2 in
      let () = Lwt_bytes.blit_to_bytes buf1 0 bytes2 3 3 in
      let check = "abcabc" = Bytes.to_string bytes2 in
      Lwt.return check
    end;

    test "blit to bytes source out of bounds: lower limit" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let bytes2 = Bytes.of_string str2 in
      try
        let () = Lwt_bytes.blit_to_bytes buf1 (-1) bytes2 3 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "blit to bytes source out of bounds: upper limit" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let bytes2 = Bytes.of_string str2 in
      try
        let () = Lwt_bytes.blit_to_bytes buf1 1 bytes2 3 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "blit to bytes destination out of bounds: lower limit" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let bytes2 = Bytes.of_string str2 in
      try
        let () = Lwt_bytes.blit_to_bytes buf1 0 bytes2 (-1) 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "blit to bytes destination out of bounds: upper limit" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let bytes2 = Bytes.of_string str2 in
      try
        let () = Lwt_bytes.blit_to_bytes buf1 0 bytes2 4 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "blit to bytes length out of bounds: lower limit" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let bytes2 = Bytes.of_string str2 in
      try
        let () = Lwt_bytes.blit_to_bytes buf1 0 bytes2 3 (-1) in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "unsafe blit" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      let () = Lwt_bytes.unsafe_blit buf1 0 buf2 3 3 in
      let check = "abcabc" = Lwt_bytes.to_string buf2 in
      Lwt.return check
    end;

    test "unsafe blit from bytes" begin fun () ->
      let bytes1 = Bytes.of_string "abc" in
      let str2 = "abcdef" in
      let buf2 = Lwt_bytes.of_string str2 in
      let () = Lwt_bytes.unsafe_blit_from_bytes bytes1 0 buf2 3 3 in
      let check = "abcabc" = Lwt_bytes.to_string buf2 in
      Lwt.return check
    end;

    test "unsafe blit to bytes" begin fun () ->
      let str1 = "abc" in
      let buf1 = Lwt_bytes.of_string str1 in
      let str2 = "abcdef" in
      let bytes2 = Bytes.of_string str2 in
      let () = Lwt_bytes.unsafe_blit_to_bytes buf1 0 bytes2 3 3 in
      let check = "abcabc" = Bytes.to_string bytes2 in
      Lwt.return check
    end;

    test "proxy" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      let buf' = Lwt_bytes.proxy buf 3 3 in
      let check1 = "def" = Lwt_bytes.to_string buf' in
      let () = Lwt_bytes.set buf 3 'a' in
      let check2 = "aef" = Lwt_bytes.to_string buf' in
      Lwt.return (check1 && check2)
    end;

    test "proxy offset out of bounds: lower limit" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      try
        let _ = Lwt_bytes.proxy buf (-1) 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "proxy offset out of bounds: upper limit" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      try
        let _ = Lwt_bytes.proxy buf 4 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "proxy length out of bounds: lower limit" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      try
        let _ = Lwt_bytes.proxy buf 3 (-1) in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "extract" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      let buf' = Lwt_bytes.extract buf 3 3 in
      let check = "def" = Lwt_bytes.to_string buf' in
      Lwt.return check
    end;

    test "extract offset out of bounds: lower limit" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      try
        let _ = Lwt_bytes.extract buf (-1) 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "extract offset out of bounds: upper limit" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      try
        let _ = Lwt_bytes.extract buf 4 3 in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "extract length out of bounds: lower limit" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      try
        let _ = Lwt_bytes.extract buf 3 (-1) in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "copy" begin fun () ->
      let str = "abc" in
      let buf = Lwt_bytes.of_string str in
      let buf' = Lwt_bytes.copy buf in
      let check = str = Lwt_bytes.to_string buf' in
      Lwt.return check
    end;

    test "fill" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      let () = Lwt_bytes.fill buf 3 3 'a' in
      let check = "abcaaa" = Lwt_bytes.to_string buf in
      Lwt.return check
    end;

    test "fill offset out of bounds: lower limit" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      try
        let () = Lwt_bytes.fill buf (-1) 3 'a' in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "fill offset out of bounds: upper limit" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      try
        let () = Lwt_bytes.fill buf 4 3 'a' in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "fill length out of bounds lower limit" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      try
        let () = Lwt_bytes.fill buf 3 (-1) 'a' in
        Lwt.return_false
      with
      | Invalid_argument _ -> Lwt.return_true
      | _ -> Lwt.return_false
    end;

    test "unsafe fill" begin fun () ->
      let str = "abcdef" in
      let buf = Lwt_bytes.of_string str in
      let () = Lwt_bytes.unsafe_fill buf 3 3 'a' in
      let check = "abcaaa" = Lwt_bytes.to_string buf in
      Lwt.return check
    end;

    test "bytes read" begin fun () ->
      let test_file = "bytes_io_data" in
      Lwt_unix.openfile test_file [O_RDONLY] 0
      >>= fun fd ->
      let buf = Lwt_bytes.create 6 in
      Lwt_bytes.read fd buf 0 6
      >>= fun _n ->
      let check = "abcdef" = Lwt_bytes.to_string buf in
      Lwt_unix.close fd
      >>= fun () ->
      Lwt.return check
    end;

    test "bytes write" begin fun () ->
      let test_file = "bytes_io_data_write" in
      Lwt_unix.openfile test_file [O_RDWR;O_TRUNC; O_CREAT] 0o666
      >>= fun fd ->
      let buf_write = Lwt_bytes.of_string "abc" in
      Lwt_bytes.write fd buf_write 0 3
      >>= fun _n ->
      Lwt_unix.close fd
      >>= fun () ->
      Lwt_unix.openfile test_file [O_RDONLY] 0
      >>= fun fd ->
      let buf_read = Lwt_bytes.create 3 in
      Lwt_bytes.read fd buf_read 0 3
      >>= fun _n ->
      let check = buf_write = buf_read in
      Lwt_unix.close fd
      >>= fun () ->
      Lwt.return check
    end;

    test "bytes recv" ~only_if:(fun () -> not Sys.win32) begin fun () ->
      let buf = gen_buf 6 in
      let server_logic socket =
        Lwt_unix.write_string socket "abcdefghij" 0 9
      in
      let client_logic socket =
        Lwt_bytes.recv socket buf 0 6 []
      in
      tcp_server_client_exchange server_logic client_logic
      >>= fun () ->
      let check = "abcdef" = Lwt_bytes.to_string buf in
      Lwt.return check
    end;

    test "bytes send" ~only_if:(fun () -> not Sys.win32) begin fun () ->
      let buf = gen_buf 6 in
      let server_logic socket =
        Lwt_bytes.send socket (Lwt_bytes.of_string "abcdef") 0 6 []
      in
      let client_logic socket =
        Lwt_bytes.recv socket buf 0 6 []
      in
      tcp_server_client_exchange server_logic client_logic
      >>= fun () ->
      let check = "abcdef" = Lwt_bytes.to_string buf in
      Lwt.return check
    end;

    test "bytes recvfrom" ~only_if:(fun () -> not Sys.win32) begin fun () ->
      let buf = gen_buf 6 in
      let server_logic socket =
        Lwt_bytes.recvfrom socket buf 0 6 []
      in
      let client_logic socket sockaddr =
        Lwt_unix.sendto socket (Bytes.of_string "abcdefghij") 0 9 [] sockaddr
      in
      udp_server_client_exchange server_logic client_logic
      >>= fun () ->
      let check = "abcdef" = Lwt_bytes.to_string buf in
      Lwt.return check
    end;

    test "bytes sendto" ~only_if:(fun () -> not Sys.win32) begin fun () ->
      let buf = gen_buf 6 in
      let server_logic socket =
        Lwt_bytes.recvfrom socket buf 0 6 []
      in
      let client_logic socket sockaddr =
        let message = Lwt_bytes.of_string "abcdefghij" in
        Lwt_bytes.sendto socket message 0 9 [] sockaddr
      in
      udp_server_client_exchange server_logic client_logic
      >>= fun () ->
      let check = "abcdef" = Lwt_bytes.to_string buf in
      Lwt.return check
    end;

    test "bytes recv_msg" ~only_if:(fun () -> not Sys.win32) begin fun () ->
      let buffer = gen_buf 6 in
      let offset = 0 in
      let io_vectors = [Lwt_bytes.io_vector ~buffer ~offset ~length:6] in
      let server_logic socket =
        (Lwt_bytes.recv_msg [@ocaml.warning "-3"]) ~socket ~io_vectors
      in
      let client_logic socket sockaddr =
        let message = Lwt_bytes.of_string "abcdefghij" in
        Lwt_bytes.sendto socket message 0 9 [] sockaddr
      in
      udp_server_client_exchange server_logic client_logic
      >>= fun () ->
      let check = "abcdef" = Lwt_bytes.to_string buffer in
      Lwt.return check
    end;

    test "bytes send_msg" ~only_if:(fun () -> not Sys.win32) begin fun () ->
      let buffer = gen_buf 6 in
      let offset = 0 in
      let server_logic socket =
        let io_vectors = [Lwt_bytes.io_vector ~buffer ~offset ~length:6] in
        (Lwt_bytes.recv_msg [@ocaml.warning "-3"]) ~socket ~io_vectors
      in
      let client_logic socket sockaddr =
        Lwt_unix.connect socket sockaddr
        >>= fun () ->
        let message = Lwt_bytes.of_string "abcdefghij" in
        let io_vectors = [Lwt_bytes.io_vector ~buffer:message ~offset ~length:9] in
        (Lwt_bytes.send_msg [@ocaml.warning "-3"]) ~socket ~io_vectors ~fds:[]
      in
      udp_server_client_exchange server_logic client_logic
      >>= fun () ->
      let check = "abcdef" = Lwt_bytes.to_string buffer in
      Lwt.return check
    end;

    test "map_file" begin fun () ->
      let test_file = "bytes_io_data" in
      let fd = Unix.openfile test_file [O_RDONLY] 0 in
      let shared = false in
      let size = 6 in
      let buffer = Lwt_bytes.map_file ~fd ~shared ~size () in
      let check = "abcdef" = Lwt_bytes.to_string buffer in
      let () = Unix.close fd in
      Lwt.return check
    end;

    test "page_size" begin fun () ->
      let sizes = [4096; 65536] in
      Lwt.return (List.mem Lwt_bytes.page_size sizes)
    end;

    test "mincore buffer length = page_size * 2, n_states = 1"
      ~only_if:(fun () -> not Sys.win32) begin fun () ->
      test_mincore (Lwt_bytes.page_size * 2) Lwt_bytes.page_size 1
      >>= fun () -> Lwt.return true
    end;

    test "mincore buffer length = page_size * 2, n_states = 2"
      ~only_if:(fun () -> not Sys.win32) begin fun () ->
      Lwt.catch
        (fun () ->
           test_mincore (Lwt_bytes.page_size * 2) Lwt_bytes.page_size 2
           >>= fun () -> Lwt.return false
        )
        (function
          | Invalid_argument _message -> Lwt.return true
          | exn -> Lwt.fail exn
        )
    end;

    test "mincore buffer length = page_size * 2 + 1, n_states = 2"
      ~only_if:(fun () -> not Sys.win32) begin fun () ->
      test_mincore (Lwt_bytes.page_size * 2 + 1) Lwt_bytes.page_size 2
      >>= fun () ->
      Lwt.return true
    end;

    test "mincore buffer length = page_size , n_states = 0"
      ~only_if:(fun () -> not Sys.win32) begin fun () ->
      test_mincore (Lwt_bytes.page_size * 2 + 1) Lwt_bytes.page_size 0
      >>= fun () -> Lwt.return true
    end;

    test "wait_mincore correct bounds"
      ~only_if:(fun () -> not Sys.win32) begin fun () ->
      test_wait_mincore (Lwt_bytes.page_size * 2 + 1) Lwt_bytes.page_size
      >>= fun () -> Lwt.return true
    end;

    test "wait_mincore offset < 0"
      ~only_if:(fun () -> not Sys.win32) begin fun () ->
      Lwt.catch
        (fun () ->
           test_wait_mincore (Lwt_bytes.page_size * 2 + 1) (-1)
           >>= fun () -> Lwt.return false
        )
        (function
          | Invalid_argument _message -> Lwt.return true
          | exn -> Lwt.fail exn
        )
    end;

    test "wait_mincore offset > buffer length"
      ~only_if:(fun () -> not Sys.win32) begin fun () ->
      Lwt.catch
        (fun () ->
           let buff_len = Lwt_bytes.page_size * 2 + 1 in
           test_wait_mincore buff_len (buff_len + 1)
           >>= fun () -> Lwt.return false
        )
        (function
          | Invalid_argument _message -> Lwt.return true
          | exn -> Lwt.fail exn
        )
    end;
  ]
