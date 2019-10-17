(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



open Test
open Lwt.Infix

(* The CLOEXEC tests use execv(2) to execute this code, by passing --cloexec to
   the copy of the tester in the child process. This is a module side effect
   that interprets that --cloexec argument. *)
let () =
  let is_fd_open fd =
    let fd  = (Obj.magic (int_of_string fd) : Unix.file_descr) in
    let buf = Bytes.create 1 in
    try
      ignore (Unix.read fd buf 0 1);
      true
    with Unix.Unix_error (Unix.EBADF, _, _) ->
      false
  in

  match Sys.argv with
  | [|_; "--cloexec"; fd; "--open"|] ->
    if is_fd_open fd then
      exit 0
    else
      exit 1
  | [|_; "--cloexec"; fd; "--closed"|] ->
    if is_fd_open fd then
      exit 1
    else
      exit 0
  | _ ->
    ()

let test_cloexec ~closed flags =
  Lwt_unix.openfile "/dev/zero" (Unix.O_RDONLY :: flags) 0o644 >>= fun fd ->
  match Lwt_unix.fork () with
  | 0 ->
    let fd = string_of_int (Obj.magic (Lwt_unix.unix_file_descr fd)) in
    let expected_status = if closed then "--closed" else "--open" in
    (* There's no portable way to obtain the tester executable name (which may
       even no longer exist at this point), but argv[0] fortunately has the
       right value when the tests are run in the Lwt dev environment. *)
    Unix.execv Sys.argv.(0) [|""; "--cloexec"; fd; expected_status|]
  | n ->
    Lwt_unix.close fd >>= fun () ->
    Lwt_unix.waitpid [] n >>= function
    | _, Unix.WEXITED 0 -> Lwt.return_true
    | _, (Unix.WEXITED _ | Unix.WSIGNALED _ | Unix.WSTOPPED _) ->
      Lwt.return_false

let openfile_tests = [
  test "openfile: O_CLOEXEC" ~only_if:(fun () -> not Sys.win32)
    (fun () -> test_cloexec ~closed:true [Unix.O_CLOEXEC]);

  test "openfile: O_CLOEXEC not given" ~only_if:(fun () -> not Sys.win32)
    (fun () -> test_cloexec ~closed:false []);

#if OCAML_VERSION >= (4, 05, 0)
  test "openfile: O_KEEPEXEC" ~only_if:(fun () -> not Sys.win32)
    (fun () -> test_cloexec ~closed:false [Unix.O_KEEPEXEC]);

  test "openfile: O_CLOEXEC, O_KEEPEXEC" ~only_if:(fun () -> not Sys.win32)
    (fun () -> test_cloexec ~closed:true [Unix.O_CLOEXEC; Unix.O_KEEPEXEC]);

  test "openfile: O_KEEPEXEC, O_CLOEXEC" ~only_if:(fun () -> not Sys.win32)
    (fun () -> test_cloexec ~closed:true [Unix.O_KEEPEXEC; Unix.O_CLOEXEC]);
#endif
]

let utimes_tests = [
  test "utimes: basic"
    (fun () ->
      let temporary_file = Test_unix.temp_file () in

      Lwt_unix.utimes temporary_file 1. 2. >>= fun () ->
      let stat = Unix.stat temporary_file in
      let c1 = stat.Unix.st_atime = 1. in
      let c2 = stat.Unix.st_mtime = 2. in

      Lwt.return (c1 && c2));

  test "utimes: current time"
    (fun () ->
      (* Unix.stat reports times about an hour away from those set by
         Unix.utimes on Windows on MinGW. Have not searched for the root cause
         yet. *)
      let acceptable_delta = if Sys.win32 then 7200. else 2. in
      let now = Unix.gettimeofday () in

      let temporary_file = Test_unix.temp_file () in

      Lwt_unix.utimes temporary_file 1. 2. >>= fun () ->
      Lwt_unix.utimes temporary_file 0. 0. >>= fun () ->
      let stat = Unix.stat temporary_file in
      let c1 = abs_float (stat.Unix.st_atime -. now) < acceptable_delta in
      let c2 = abs_float (stat.Unix.st_mtime -. now) < acceptable_delta in

      Lwt.return (c1 && c2));

  test "utimes: missing file"
    (fun () ->
      Lwt.catch
        (fun () -> Lwt_unix.utimes "non-existent-file" 0. 0.)
        (function
        | Unix.Unix_error (Unix.ENOENT, "utimes", _) -> Lwt.return_unit
        | Unix.Unix_error (Unix.EUNKNOWNERR _, "utimes", _) -> Lwt.return_unit
        | e -> Lwt.fail e) [@ocaml.warning "-4"] >>= fun () ->
      Lwt.return_true);
]

let readdir_tests =
  let populate n =
    let path = Test_unix.temp_directory () in

    let filenames =
      let rec loop n acc =
        if n <= 0 then acc
        else loop (n - 1) ((string_of_int n)::acc)
      in
      loop n []
    in

    List.iter (fun filename ->
      let fd =
        Unix.(openfile
          (Filename.concat path filename) [O_WRONLY; O_CREAT] 0o644)
      in
      Unix.close fd)
      filenames;

    path, ["."; ".."] @ filenames
  in

  let equal, subset =
    let module StringSet = Set.Make (String) in
    (* Necessary before 4.02. *)
    let of_list l =
      List.fold_left (fun set n -> StringSet.add n set) StringSet.empty l in

    (fun filenames filenames' ->
      StringSet.equal (of_list filenames) (of_list filenames')),

    (fun filenames filenames' ->
      StringSet.subset (of_list filenames) (of_list filenames'))
  in

  let read_all directory =
    let rec loop acc =
      Lwt.catch
        (fun () ->
          Lwt_unix.readdir directory >>= fun filename ->
          Lwt.return (Some filename))
        (function
          | End_of_file -> Lwt.return_none
          | exn -> Lwt.fail exn)
      >>= function
        | None -> Lwt.return acc
        | Some filename -> loop (filename::acc)
    in
    loop []
  in

  let read_n directory n =
    let rec loop n acc =
      if n <= 0 then Lwt.return acc
      else
        Lwt_unix.readdir directory >>= fun filename ->
        loop (n - 1) (filename::acc)
    in
    loop n []
  in

  [
    test "readdir: basic"
      (fun () ->
        let path, filenames = populate 5 in

        Lwt_unix.opendir path >>= fun directory ->
        read_all directory >>= fun filenames' ->
        Lwt_unix.closedir directory >>= fun () ->

        Lwt.return (List.length filenames' = 7 && equal filenames filenames'));

    test "readdir: rewinddir"
      (fun () ->
        let path, filenames = populate 5 in

        Lwt_unix.opendir path >>= fun directory ->
        read_n directory 3 >>= fun filenames' ->
        Lwt_unix.rewinddir directory >>= fun () ->
        read_all directory >>= fun filenames'' ->
        Lwt_unix.closedir directory >>= fun () ->

        Lwt.return
          (List.length filenames' = 3 &&
           subset filenames' filenames &&
           List.length filenames'' = 7 &&
           equal filenames'' filenames));

    test "readdir: readdir_n"
      (fun () ->
        let path, filenames = populate 5 in

        Lwt_unix.opendir path >>= fun directory ->
        Lwt_unix.readdir_n directory 3 >>= fun filenames' ->
        Lwt_unix.readdir_n directory 10 >>= fun filenames'' ->
        Lwt_unix.closedir directory >>= fun () ->

        let all = (Array.to_list filenames') @ (Array.to_list filenames'') in

        Lwt.return
          (Array.length filenames' = 3 &&
           Array.length filenames'' = 4 &&
           equal all filenames));

    test "readdir: files_of_directory"
      (fun () ->
        let path, filenames = populate 5 in

        let stream = Lwt_unix.files_of_directory path in
        Lwt_stream.to_list stream >>= fun filenames' ->

        Lwt.return (equal filenames' filenames));

    (* Should make sure Win32 behaves in the same way as well. *)
    test "readdir: already closed" ~only_if:(fun () -> not Sys.win32)
      (fun () ->
        let path, _ = populate 0 in

        Lwt_unix.opendir path >>= fun directory ->
        Lwt_unix.closedir directory >>= fun () ->

        let expect_ebadf tag t =
          let tag = "Lwt_unix." ^ tag in
          Lwt.catch
            (fun () ->
              t () >>= fun () ->
              Lwt.return_false)
            (function
              | Unix.Unix_error (Unix.EBADF, tag', _) when tag' = tag ->
                Lwt.return_true
              | exn -> Lwt.fail exn) [@ocaml.warning "-4"]
        in

        Lwt_list.for_all_s (fun (tag, t) -> expect_ebadf tag t)
          ["readdir", (fun () -> Lwt_unix.readdir directory >|= ignore);
           "readdir_n", (fun () -> Lwt_unix.readdir_n directory 1 >|= ignore);
           "rewinddir", (fun () -> Lwt_unix.rewinddir directory);
           "closedir", (fun () -> Lwt_unix.closedir directory)]);
  ]

let io_vectors_byte_count_tests =
  let open Lwt_unix.IO_vectors in
  [ test "io_vector_byte_count: basic"
      (fun () ->
         let iov = create () in
         append_bytes iov (Bytes.create 10) 0 10;
         append_bigarray iov (Lwt_bytes.create 10) 0 10;
         Lwt.return (byte_count iov = 20));

    test "io_vector_byte_count: offsets, partials"
      (fun () ->
         let iov = create () in
         append_bytes iov (Bytes.create 10) 5 1;
         append_bigarray iov (Lwt_bytes.create 10) 1 1;
         Lwt.return (byte_count iov = 2));

    test "io_vector_byte_count: drops"
      (fun () ->
         let iov = create () in
         append_bytes iov (Bytes.create 10) 5 1;
         append_bigarray iov (Lwt_bytes.create 10) 1 1;
         drop iov 1;
         Lwt.return (byte_count iov = 1));
  ]

let readv_tests =
  (* All buffers are initially filled with '_'. *)
  let make_io_vectors vecs =
    let open Lwt_unix.IO_vectors in
    let io_vectors = create () in
    let underlying =
      List.map (function
        | `Bytes (prefix, slice_length, suffix) ->
          let buffer = Bytes.make (prefix + slice_length + suffix) '_' in
          append_bytes io_vectors buffer prefix slice_length;
          `Bytes buffer
        | `Bigarray (prefix, slice_length, suffix) ->
          let total_length = prefix + slice_length + suffix in
          let buffer = Lwt_bytes.create total_length in
          Lwt_bytes.fill buffer 0 total_length '_';
          append_bigarray io_vectors buffer prefix slice_length;
          `Bigarray buffer)
        vecs
    in
    io_vectors, underlying
  in

  let writer write_fd data = fun () ->
    let data = Bytes.unsafe_of_string data in
    Lwt_unix.write write_fd data 0 (Bytes.length data) >>= fun bytes_written ->
    Lwt_unix.close write_fd >>= fun () ->
    (* Instrumentation for debugging an unreliable test. *)
    if bytes_written <> Bytes.length data then
      Printf.eprintf "\nwritev: expected to write %i bytes; wrote %i\n"
        (Bytes.length data) bytes_written;
    Lwt.return (bytes_written = Bytes.length data)
  in

  let reader read_fd io_vectors underlying expected_count expected_data =
      fun () ->
    Gc.full_major ();
    let t = Lwt_unix.readv read_fd io_vectors in
    Gc.full_major ();
    t >>= fun bytes_read ->
    Lwt_unix.close read_fd >>= fun () ->

    let actual =
      List.fold_left (fun acc -> function
        | `Bytes buffer -> acc ^ (Bytes.unsafe_to_string buffer)
        | `Bigarray buffer -> acc ^ (Lwt_bytes.to_string buffer))
        "" underlying
    in

    (* Instrumentation for an unreliable test. *)
    if bytes_read <> expected_count then
      Printf.eprintf "\nreadv: expected to read %i bytes; read %i\n"
        expected_count bytes_read;
    if actual <> expected_data then
      Printf.eprintf "\nreadv: expected to read %s; read %s\n"
        expected_data actual;

    Lwt.return (actual = expected_data && bytes_read = expected_count)
  in

  [
    test "readv: basic non-blocking" ~only_if:(fun () -> not Sys.win32)
      (fun () ->
        let io_vectors, underlying =
          make_io_vectors
            [`Bytes (1, 3, 1);
             `Bigarray (1, 4, 1)]
        in

        let read_fd, write_fd = Lwt_unix.pipe () in

        Lwt_list.for_all_s (fun t -> t ())
          [writer write_fd "foobar";
           reader read_fd io_vectors underlying 6 "_foo__bar__"]);

    test "readv: basic blocking" ~only_if:(fun () -> not Sys.win32)
      (fun () ->
        let io_vectors, underlying =
          make_io_vectors
            [`Bytes (1, 3, 1);
             `Bigarray (1, 4, 1)]
        in

        let read_fd, write_fd = Lwt_unix.pipe () in
        Lwt_unix.set_blocking read_fd true;

        Lwt_list.for_all_s (fun t -> t ())
          [writer write_fd "foobar";
           reader read_fd io_vectors underlying 6 "_foo__bar__"]);

    test "readv: drop" ~only_if:(fun () -> not Sys.win32)
      (fun () ->
        let io_vectors, underlying =
          make_io_vectors
            [`Bytes (0, 1, 0);
             `Bytes (1, 4, 1)]
        in
        Lwt_unix.IO_vectors.drop io_vectors 2;

        let read_fd, write_fd = Lwt_unix.pipe () in

        Lwt_list.for_all_s (fun t -> t ())
          [writer write_fd "foobar";
           reader read_fd io_vectors underlying 3 "___foo_"]);

    test "readv: iovecs exceeding limit"
      ~only_if:(fun () -> not Sys.win32 &&
                          Lwt_unix.IO_vectors.system_limit <> None)
      (fun () ->
        let limit =
          match Lwt_unix.IO_vectors.system_limit with
          | Some limit -> limit
          | None -> assert false
        in

        let underlying =
          Array.init (limit + 1) (fun _ -> `Bytes (Bytes.make 1 '_'))
          |> Array.to_list
        in

        let io_vectors = Lwt_unix.IO_vectors.create () in
        List.iter (fun (`Bytes buffer) ->
          Lwt_unix.IO_vectors.append_bytes io_vectors buffer 0 1) underlying;

        let expected = String.make limit 'a' in

        let read_fd, write_fd = Lwt_unix.pipe () in

        Lwt_list.for_all_s (fun t -> t ())
          [writer write_fd (expected ^ "a");
           reader read_fd io_vectors underlying limit (expected ^ "_")]);
  ]

let writev_tests =
  let make_io_vectors vecs =
    let open Lwt_unix.IO_vectors in
    let io_vectors = create () in
    List.iter (function
      | `Bytes (s, offset, length) ->
        append_bytes io_vectors (Bytes.unsafe_of_string s) offset length
      | `Bigarray (s, offset, length) ->
        append_bigarray io_vectors (Lwt_bytes.of_string s) offset length)
      vecs;
    io_vectors
  in

  let writer ?blocking write_fd io_vectors data_length = fun () ->
    Lwt_unix.blocking write_fd >>= fun is_blocking ->
    Gc.full_major ();
    let t = Lwt_unix.writev write_fd io_vectors in
    Gc.full_major ();
    t >>= fun bytes_written ->
    Lwt_unix.close write_fd >>= fun () ->
    let blocking_matches =
      match blocking, is_blocking with
      | Some v, v' when v <> v' -> false
      | _ -> true
    in
    Lwt.return (bytes_written = data_length && blocking_matches)
  in

  let reader read_fd ?(not_readable = false) expected_data = fun () ->
    if not_readable then
      let readable = Lwt_unix.readable read_fd in
      Lwt_unix.close read_fd >>= fun () ->
      Lwt.return (not readable)
    else
      let open! Lwt_io in
      let channel = of_fd ~mode:input read_fd in
      read channel >>= fun read_data ->
      close channel >>= fun () ->
      Lwt.return (read_data = expected_data)
  in

  [
    test "writev: basic non-blocking" ~only_if:(fun () -> not Sys.win32)
      (fun () ->
        let io_vectors =
          make_io_vectors
            [`Bytes ("foo", 0, 3);
             `Bytes ("bar", 0, 3);
             `Bigarray ("baz", 0, 3)]
        in

        let read_fd, write_fd = Lwt_unix.pipe () in

        Lwt_list.for_all_s (fun t -> t ())
          [writer ~blocking:false write_fd io_vectors 9;
           reader read_fd "foobarbaz"]);

    test "writev: basic blocking" ~only_if:(fun () -> not Sys.win32)
      (fun () ->
        let io_vectors =
          make_io_vectors
            [`Bytes ("foo", 0, 3);
             `Bytes ("bar", 0, 3);
             `Bigarray ("baz", 0, 3)]
        in

        let read_fd, write_fd = Lwt_unix.pipe () in
        Lwt_unix.set_blocking write_fd true;

        Lwt_list.for_all_s (fun t -> t ())
          [writer ~blocking:true write_fd io_vectors 9;
           reader read_fd "foobarbaz"]);

    test "writev: slices" ~only_if:(fun () -> not Sys.win32)
      (fun () ->
        let io_vectors =
          make_io_vectors [`Bytes ("foo", 1, 2); `Bigarray ("bar", 1, 2)] in

        let read_fd, write_fd = Lwt_unix.pipe () in

        Lwt_list.for_all_s (fun t -> t ())
          [writer write_fd io_vectors 4;
           reader read_fd "ooar"]);

    test "writev: drop, is_empty" ~only_if:(fun () -> not Sys.win32)
      (fun () ->
        let io_vectors =
          make_io_vectors
            [`Bytes ("foo", 0, 3);
             `Bytes ("bar", 0, 3);
             `Bigarray ("baz", 0, 3)]
        in

        let read_fd, write_fd = Lwt_unix.pipe () in

        let initially_empty = Lwt_unix.IO_vectors.is_empty io_vectors in

        Lwt_unix.IO_vectors.drop io_vectors 4;
        let empty_after_partial_drop =
          Lwt_unix.IO_vectors.is_empty io_vectors in

        Lwt_list.for_all_s (fun t -> t ())
          [writer write_fd io_vectors 5;
           reader read_fd "arbaz"] >>= fun io_correct ->

        Lwt_unix.IO_vectors.drop io_vectors 5;
        let empty_after_exact_drop = Lwt_unix.IO_vectors.is_empty io_vectors in

        Lwt_unix.IO_vectors.drop io_vectors 100;
        let empty_after_excess_drop = Lwt_unix.IO_vectors.is_empty io_vectors in

        Lwt.return
          (not initially_empty &&
           not empty_after_partial_drop &&
           io_correct &&
           empty_after_exact_drop &&
           empty_after_excess_drop));

    test "writev: degenerate vectors" ~only_if:(fun () -> not Sys.win32)
      (fun () ->
        let io_vectors =
          make_io_vectors
            [`Bytes ("foo", 0, 0);
             `Bigarray ("bar", 0, 0)]
        in

        let read_fd, write_fd = Lwt_unix.pipe () in

        let initially_empty = Lwt_unix.IO_vectors.is_empty io_vectors in

        Lwt_list.for_all_s (fun t -> t ())
          [writer write_fd io_vectors 0;
           reader read_fd ""] >>= fun io_correct ->

        Lwt.return (initially_empty && io_correct));

    test "writev: bad iovec" ~only_if:(fun () -> not Sys.win32)
      (fun () ->
        let negative_offset = make_io_vectors [`Bytes ("foo", -1, 3)] in
        let negative_length = make_io_vectors [`Bytes ("foo", 0, -1)] in
        let out_of_bounds = make_io_vectors [`Bytes ("foo", 1, 3)] in

        let negative_offset' = make_io_vectors [`Bigarray ("foo", -1, 3)] in
        let negative_length' = make_io_vectors [`Bigarray ("foo", 0, -1)] in
        let out_of_bounds' = make_io_vectors [`Bigarray ("foo", 1, 3)] in

        let read_fd, write_fd = Lwt_unix.pipe () in

        let writer io_vectors =
          fun () ->
            Lwt.catch
              (fun () ->
                Lwt_unix.writev write_fd io_vectors >>= fun _ ->
                Lwt.return_false)
              (function
                | Invalid_argument _ -> Lwt.return_true
                | e -> Lwt.fail e)
        in

        let close write_fd = fun () ->
          Lwt_unix.close write_fd >>= fun () -> Lwt.return_true in

        Lwt_list.for_all_s (fun t -> t ())
          [writer negative_offset;
           writer negative_length;
           writer out_of_bounds;
           writer negative_offset';
           writer negative_length';
           writer out_of_bounds';
           reader read_fd ~not_readable:true "";
           close write_fd]);

    test "writev: iovecs exceeding limit"
      ~only_if:(fun () -> not Sys.win32 &&
                          Lwt_unix.IO_vectors.system_limit <> None)
      (fun () ->
        let limit =
          match Lwt_unix.IO_vectors.system_limit with
          | Some limit -> limit
          | None -> assert false
        in

        let io_vectors =
          let open Lwt_unix.IO_vectors in
          let io_vectors = create () in
          let rec loop count =
            if count < 1 then io_vectors
            else
              (append_bytes io_vectors (Bytes.unsafe_of_string "a") 0 1;
              loop (count - 1))
          in
          loop (limit + 1)
        in

        let read_fd, write_fd = Lwt_unix.pipe () in

        Lwt_list.for_all_s (fun t -> t ())
          [writer write_fd io_vectors limit;
           reader read_fd (String.make limit 'a')]);

    test "writev: negative drop" ~only_if:(fun () -> not Sys.win32)
      (fun () ->
        let io_vectors = make_io_vectors [`Bytes ("foo", 0, 3)] in
        Lwt_unix.IO_vectors.drop io_vectors (-1);

        let read_fd, write_fd = Lwt_unix.pipe () in

        Lwt_list.for_all_s (fun t -> t ())
          [writer write_fd io_vectors 3;
           reader read_fd "foo"] >>= fun io_correct ->

        Lwt.return
          (io_correct &&
           not (Lwt_unix.IO_vectors.is_empty io_vectors)));
  ]

let send_recv_msg_tests = [
  test "send_msg, recv_msg" ~only_if:(fun () -> not Sys.win32) begin fun () ->
    let socket_1, socket_2 = Lwt_unix.(socketpair PF_UNIX SOCK_STREAM 0) in
    let pipe_read, pipe_write = Lwt_unix.pipe () in

    let source_buffer = Bytes.of_string "_foo_bar_" in
    let source_iovecs = Lwt_unix.IO_vectors.create () in
    Lwt_unix.IO_vectors.append_bytes source_iovecs source_buffer 1 3;
    Lwt_unix.IO_vectors.append_bytes source_iovecs source_buffer 5 3;

    Lwt_unix.Versioned.send_msg_2
      ~socket:socket_1
      ~io_vectors:source_iovecs
      ~fds:[Lwt_unix.unix_file_descr pipe_write] >>= fun n ->
    if n <> 6 then
      Lwt.return false

    else
      let destination_buffer = Bytes.of_string "_________" in
      let destination_iovecs = Lwt_unix.IO_vectors.create () in
      Lwt_unix.IO_vectors.append_bytes
        destination_iovecs destination_buffer 5 3;
      Lwt_unix.IO_vectors.append_bytes
        destination_iovecs destination_buffer 1 3;

      Lwt_unix.Versioned.recv_msg_2
        ~socket:socket_2 ~io_vectors:destination_iovecs >>= fun (n, fds) ->
      let succeeded =
        match n, fds, Bytes.to_string destination_buffer with
        | 6, [fd], "_bar_foo_" -> Some fd
        | _ -> None
      in
      match succeeded with
      | None ->
        Lwt.return false
      | Some fd ->

        let n = Unix.write fd (Bytes.of_string "baz") 0 3 in
        if n <> 3 then
          Lwt.return false

        else
          let buffer = Bytes.create 3 in
          Lwt_unix.read pipe_read buffer 0 3 >>= fun n ->
          match n, Bytes.to_string buffer with
          | 3, "baz" ->
            Lwt_unix.close socket_1 >>= fun () ->
            Lwt_unix.close socket_2 >>= fun () ->
            Lwt_unix.close pipe_read >>= fun () ->
            Lwt_unix.close pipe_write >>= fun () ->
            Unix.close fd;
            Lwt.return true

          | _ ->
            Lwt.return false
  end;

  test "send_msg, recv_msg (old)" ~only_if:(fun () -> not Sys.win32)
      begin fun () ->

    let socket_1, socket_2 = Lwt_unix.(socketpair PF_UNIX SOCK_STREAM 0) in
    let pipe_read, pipe_write = Lwt_unix.pipe () in

    let source_buffer = "_foo_bar_" in
    let source_iovecs = Lwt_unix.[
      {
        iov_buffer = source_buffer;
        iov_offset = 1;
        iov_length = 3;
      };
      {
        iov_buffer = source_buffer;
        iov_offset = 5;
        iov_length = 3;
      };
    ]
    in

    (Lwt_unix.send_msg [@ocaml.warning "-3"])
      ~socket:socket_1
      ~io_vectors:source_iovecs
      ~fds:[Lwt_unix.unix_file_descr pipe_write] >>= fun n ->
    if n <> 6 then
      Lwt.return false

    else
      let destination_buffer = "_________" in
      let destination_iovecs = Lwt_unix.[
        {
          iov_buffer = destination_buffer;
          iov_offset = 5;
          iov_length = 3;
        };
        {
          iov_buffer = destination_buffer;
          iov_offset = 1;
          iov_length = 3;
        };
      ]
      in

      (Lwt_unix.recv_msg [@ocaml.warning "-3"])
        ~socket:socket_2 ~io_vectors:destination_iovecs >>= fun (n, fds) ->
      let succeeded =
        match n, fds, destination_buffer with
        | 6, [fd], "_bar_foo_" -> Some fd
        | _ -> None
      in
      match succeeded with
      | None ->
        Lwt.return false
      | Some fd ->

        let n = Unix.write fd (Bytes.of_string "baz") 0 3 in
        if n <> 3 then
          Lwt.return false

        else
          let buffer = Bytes.create 3 in
          Lwt_unix.read pipe_read buffer 0 3 >>= fun n ->
          match n, Bytes.to_string buffer with
          | 3, "baz" ->
            Lwt_unix.close socket_1 >>= fun () ->
            Lwt_unix.close socket_2 >>= fun () ->
            Lwt_unix.close pipe_read >>= fun () ->
            Lwt_unix.close pipe_write >>= fun () ->
            Unix.close fd;
            Lwt.return true

          | _ ->
            Lwt.return false
  end;

  test "send_msg, recv_msg (Lwt_bytes, old)" ~only_if:(fun () -> not Sys.win32)
      begin fun () ->

    let socket_1, socket_2 = Lwt_unix.(socketpair PF_UNIX SOCK_STREAM 0) in
    let pipe_read, pipe_write = Lwt_unix.pipe () in

    let source_buffer = Lwt_bytes.of_string "_foo_bar_" in
    let source_iovecs = Lwt_bytes.[
      {
        iov_buffer = source_buffer;
        iov_offset = 1;
        iov_length = 3;
      };
      {
        iov_buffer = source_buffer;
        iov_offset = 5;
        iov_length = 3;
      };
    ]
    in

    (Lwt_bytes.send_msg [@ocaml.warning "-3"])
      ~socket:socket_1
      ~io_vectors:source_iovecs
      ~fds:[Lwt_unix.unix_file_descr pipe_write] >>= fun n ->
    if n <> 6 then
      Lwt.return false

    else
      let destination_buffer = Lwt_bytes.of_string "_________" in
      let destination_iovecs = Lwt_bytes.[
        {
          iov_buffer = destination_buffer;
          iov_offset = 5;
          iov_length = 3;
        };
        {
          iov_buffer = destination_buffer;
          iov_offset = 1;
          iov_length = 3;
        };
      ]
      in

      (Lwt_bytes.recv_msg [@ocaml.warning "-3"])
        ~socket:socket_2 ~io_vectors:destination_iovecs >>= fun (n, fds) ->
      let succeeded =
        match n, fds, Lwt_bytes.to_string destination_buffer with
        | 6, [fd], "_bar_foo_" -> Some fd
        | _ -> None
      in
      match succeeded with
      | None ->
        Lwt.return false
      | Some fd ->

        let n = Unix.write fd (Bytes.of_string "baz") 0 3 in
        if n <> 3 then
          Lwt.return false

        else
          let buffer = Bytes.create 3 in
          Lwt_unix.read pipe_read buffer 0 3 >>= fun n ->
          match n, Bytes.to_string buffer with
          | 3, "baz" ->
            Lwt_unix.close socket_1 >>= fun () ->
            Lwt_unix.close socket_2 >>= fun () ->
            Lwt_unix.close pipe_read >>= fun () ->
            Lwt_unix.close pipe_write >>= fun () ->
            Unix.close fd;
            Lwt.return true

          | _ ->
            Lwt.return false
  end;
]

let bind_tests_address = Unix.(ADDR_INET (inet_addr_loopback, 56100))

let bind_tests = [
  test "bind: basic"
    (fun () ->
      let socket = Lwt_unix.(socket PF_INET SOCK_STREAM 0) in

      Lwt.finalize
        (fun () ->
          Lwt_unix.bind socket bind_tests_address >>= fun () ->
          Lwt.return (Unix.getsockname (Lwt_unix.unix_file_descr socket)))
        (fun () ->
          Lwt_unix.close socket)

      >>= fun address' ->

      Lwt.return (address' = bind_tests_address));

  test "bind: Unix domain" ~only_if:(fun () -> not Sys.win32)
    (fun () ->
      let socket = Lwt_unix.(socket PF_UNIX SOCK_STREAM 0) in

      let rec bind_loop attempts =
        if attempts <= 0 then
          Lwt.fail (Unix.Unix_error (Unix.EADDRINUSE, "bind", ""))
        else
          let path = Test_unix.temp_name () in
          let address = Unix.(ADDR_UNIX path) in
          Lwt.catch
            (fun () ->
              Lwt_unix.bind socket address >>= fun () ->
              Lwt.return_true)
            (function
              | Unix.Unix_error (Unix.EADDRINUSE, "bind", _) -> Lwt.return_false
              | Unix.Unix_error (Unix.EPERM, "bind", _) ->
                (* On EPERM, assume that we are under WSL, but in the Windows
                   filesystem. If this ever results in a false positive, this
                   test should add a check for WSL by checking for the existence
                   of /proc/version, reading it, and checking its contents for
                   the string "WSL". *)
                Lwt.fail Skip
              | e -> Lwt.fail e) [@ocaml.warning "-4"] >>= fun bound ->
          if bound then
            Lwt.return path
          else
            bind_loop (attempts - 1)
      in

      Lwt.finalize
        (fun () ->
          bind_loop 5 >>= fun chosen_path ->
          let actual_path =
            Unix.getsockname (Lwt_unix.unix_file_descr socket) in
          Lwt.return (chosen_path, actual_path))
        (fun () ->
          Lwt_unix.close socket)
      >>= fun (chosen_path, actual_path) ->

      let actual_path =
        match actual_path with
        | Unix.ADDR_UNIX path -> path
        | Unix.ADDR_INET _ -> assert false
      in

      (try Unix.unlink chosen_path
      with _ -> ());
      (try Unix.unlink actual_path
      with _ -> ());

      (* Compare with a prefix of the actual path, due to
         https://github.com/ocaml/ocaml/pull/987 *)
      try
        Lwt.return
          (chosen_path = String.sub actual_path 0 (String.length chosen_path))
      with Invalid_argument _ -> Lwt.return_false);

  test "bind: closed"
    (fun () ->
      let socket = Lwt_unix.(socket PF_INET SOCK_STREAM 0) in
      Lwt_unix.close socket >>= fun () ->
      Lwt.catch
        (fun () ->
          Lwt_unix.bind socket bind_tests_address >>= fun () ->
          Lwt.return_false)
        (function
          | Unix.Unix_error (Unix.EBADF, _, _) -> Lwt.return_true
          | e -> Lwt.fail e) [@ocaml.warning "-4"]);

  test "bind: aborted"
    (fun () ->
      let socket = Lwt_unix.(socket PF_INET SOCK_STREAM 0) in
      Lwt_unix.abort socket Exit;
      Lwt.finalize
        (fun () ->
          Lwt.catch
            (fun () ->
              Lwt_unix.bind socket bind_tests_address >>= fun () ->
              Lwt.return_false)
            (function
              | Exit -> Lwt.return_true
              | e -> Lwt.fail e))
        (fun () -> Lwt_unix.close socket));
]

let dir_tests = [
    test "getcwd"
    (fun () ->
      Lwt_unix.getcwd () >>= fun (_:string) ->
      Lwt.return_true
    );
    test "getcwd and chdir"
    (fun () ->
      Lwt_unix.getcwd () >>= fun here ->
      Lwt_unix.chdir here >>= fun () ->
      Lwt_unix.getcwd () >>= fun there ->
      Lwt.return (here = there)
    );
    test "getcwd and Unix.getcwd"
    (fun () ->
      let unix_here = Unix.getcwd () in
      Lwt_unix.getcwd () >>= fun here ->
      Lwt.return (here = unix_here)
    );
]

let lwt_preemptive_tests = [
  test "run_in_main" begin fun () ->
    let f () =
      Lwt_preemptive.run_in_main (fun () ->
        Lwt_unix.sleep 0.01 >>= fun () ->
        Lwt.return 42)
    in
    Lwt_preemptive.detach f () >>= fun x ->
    Lwt.return (x = 42)
  end;
]

let getlogin_works =
  if Sys.win32 then
    false
  else
    match Unix.getlogin () with
    | _ -> true
    | exception Unix.Unix_error _ -> false

let lwt_user_tests = [
  test "getlogin and Unix.getlogin" ~only_if:(fun () -> getlogin_works) begin fun () ->
    let unix_user = Unix.getlogin () in
    Lwt_unix.getlogin () >>= fun user ->
    Lwt.return (user = unix_user)
  end;
  test "getpwnam and Unix.getpwnam" ~only_if:(fun () -> getlogin_works) begin fun () ->
    let unix_user = Unix.getlogin () in
    let unix_password = Unix.getpwnam unix_user in
    Lwt_unix.getpwnam unix_user >>= fun password ->
    Lwt.return (password = unix_password)
  end;
  test "getpwuid and Unix.getpwuid" ~only_if:(fun () -> getlogin_works) begin fun () ->
    let pwnam = Unix.getpwnam (Unix.getlogin ()) in
    let unix_pwuid = Unix.getpwuid pwnam.pw_uid in
    Lwt_unix.getpwuid pwnam.pw_uid >>= fun pwuid ->
    Lwt.return (pwuid = unix_pwuid)
  end;
  test "getgrgid and Unix.getgrgid" ~only_if:(fun () -> not Sys.win32) begin fun () ->
    let group_id = Unix.getgid () in
    let unix_group = Unix.getgrgid group_id in
    Lwt_unix.getgrgid group_id >>= fun group ->
    Lwt.return (group = unix_group)
  end;
  test "getgrnam and Unix.getgrnam" ~only_if:(fun () -> not Sys.win32) begin fun () ->
    let group_id = Unix.getgid () in
    let unix_group = Unix.getgrgid group_id in
    let group_name = unix_group.gr_name in
    Lwt_unix.getgrnam group_name >>= fun group ->
    Lwt.return (group = unix_group)
  end
]

let suite =
  suite "lwt_unix"
    (openfile_tests @
     utimes_tests @
     readdir_tests @
     io_vectors_byte_count_tests @
     readv_tests @
     writev_tests @
     send_recv_msg_tests @
     bind_tests @
     dir_tests @
     lwt_preemptive_tests @
     lwt_user_tests
    )
