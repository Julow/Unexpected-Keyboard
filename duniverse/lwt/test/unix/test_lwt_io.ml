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

open Test
open Lwt.Infix

exception Dummy_error

let local =
  let last_port = ref 4321 in
  fun () ->
    incr last_port;
    Unix.ADDR_INET (Unix.inet_addr_loopback, !last_port)

(* Helpers for [establish_server] tests. *)
module Establish_server =
struct
  let with_client f =
    let local = local () in

    let handler_finished, notify_handler_finished = Lwt.wait () in

    Lwt_io.establish_server_with_client_address
      local
      (fun _client_address channels ->
        Lwt.finalize
          (fun () -> f channels)
          (fun () ->
            Lwt.wakeup notify_handler_finished ();
            Lwt.return_unit))

    >>= fun server ->

    let client_finished =
      Lwt_io.with_connection
        local
        (fun (_, out_channel) ->
          Lwt_io.write out_channel "hello world" >>= fun () ->
          handler_finished)
    in

    client_finished >>= fun () ->
    Lwt_io.shutdown_server server

  (* Hacky is_closed functions that attempt to read from/write to the channels
     to see if they are closed. *)
  let is_closed_in channel =
    Lwt.catch
      (fun () -> Lwt_io.read_char channel >|= fun _ -> false)
      (function
      | Lwt_io.Channel_closed _ -> Lwt.return_true
      | _ -> Lwt.return_false)

  let is_closed_out channel =
    Lwt.catch
      (fun () -> Lwt_io.write_char channel 'a' >|= fun () -> false)
      (function
      | Lwt_io.Channel_closed _ -> Lwt.return_true
      | _ -> Lwt.return_false)
end

let suite = suite "lwt_io" [
  test "auto-flush"
    (fun () ->
      let sent = ref [] in
      let oc =
        Lwt_io.make
          ~mode:Lwt_io.output
          (fun buf ofs len ->
            let bytes = Bytes.create len in
            Lwt_bytes.blit_to_bytes buf ofs bytes 0 len;
            sent := bytes :: !sent;
            Lwt.return len)
      in
      Lwt_io.write oc "foo" >>= fun () ->
      Lwt_io.write oc "bar" >>= fun () ->
      if !sent <> [] then
        Lwt.return false
      else
        Lwt_unix.yield () >>= fun () ->
        Lwt.return (!sent = [Bytes.of_string "foobar"]));

  test "auto-flush in atomic"
    (fun () ->
      let sent = ref [] in
      let oc =
        Lwt_io.make
          ~mode:Lwt_io.output
          (fun buf ofs len ->
            let bytes = Bytes.create len in
            Lwt_bytes.blit_to_bytes buf ofs bytes 0 len;
            sent := bytes :: !sent;
            Lwt.return len)
      in
      Lwt_io.atomic
        (fun oc ->
          Lwt_io.write oc "foo" >>= fun () ->
          Lwt_io.write oc "bar" >>= fun () ->
          if !sent <> [] then
            Lwt.return false
          else
            Lwt_unix.yield () >>= fun () ->
            Lwt.return (!sent = [Bytes.of_string "foobar"]))
        oc);

  (* Without the corresponding bugfix, which is to handle ENOTCONN from
     Lwt_unix.shutdown, this test raises an exception from the handler's calls
     to close. *)
  test "establish_server_1: shutdown: client closes first"
    ~only_if:(fun () ->
      not (Lwt_config._HAVE_LIBEV && Lwt_config.libev_default))
  (* Note: this test is currently flaky on Linux with libev enabled, so we skip
     it in that case. *)
    (fun () ->
      let wait_for_client, client_finished = Lwt.wait () in

      let handler_wait, run_handler = Lwt.wait () in
      let handler =
        handler_wait >>= fun (in_channel, out_channel) ->
        wait_for_client >>= fun () ->
        Lwt_io.close in_channel >>= fun () ->
        Lwt_io.close out_channel >>= fun () ->
        Lwt.return_true
      in

      let local = local () in

      let server =
        (Lwt_io.Versioned.establish_server_1 [@ocaml.warning "-3"])
          local (fun channels -> Lwt.wakeup run_handler channels)
      in

      Lwt_io.with_connection local (fun _ -> Lwt.return_unit) >>= fun () ->
      Lwt.wakeup client_finished ();
      Lwt_io.shutdown_server server >>= fun () ->
      handler);

  (* Counterpart to establish_server: shutdown test. Confirms that shutdown is
     implemented correctly in open_connection. *)
  test "open_connection: shutdown: server closes first"
    (fun () ->
      let wait_for_server, server_finished = Lwt.wait () in

      let local = local () in

      let server =
        (Lwt_io.Versioned.establish_server_1 [@ocaml.warning "-3"])
          local (fun (in_channel, out_channel) ->
            Lwt.async (fun () ->
              Lwt_io.close in_channel >>= fun () ->
              Lwt_io.close out_channel >|= fun () ->
              Lwt.wakeup server_finished ()))
      in

      Lwt_io.with_connection local (fun _ ->
        wait_for_server >>= fun () ->
        Lwt.return_true)

      >>= fun result ->

      Lwt_io.shutdown_server server >|= fun () ->
      result);

  test "establish_server: implicit close"
    (fun () ->
      let open Establish_server in

      let in_channel' = ref Lwt_io.stdin in
      let out_channel' = ref Lwt_io.stdout in

      let in_open_in_handler = ref false in
      let out_open_in_handler = ref false in

      let run =
        Establish_server.with_client
          (fun (in_channel, out_channel) ->
            in_channel' := in_channel;
            out_channel' := out_channel;

            is_closed_out out_channel >>= fun yes ->
            out_open_in_handler := not yes;

            is_closed_in in_channel >|= fun yes ->
            in_open_in_handler := not yes)
      in

      run >>= fun () ->
      (* Give a little time for the close system calls on the connection sockets
         to complete. The Lwt_io and Lwt_unix APIs do not currently allow
         binding on the implicit closes of these sockets, so resorting to a
         delay. *)
      Lwt_unix.sleep 0.05 >>= fun () ->

      is_closed_in !in_channel' >>= fun in_closed_after_handler ->
      is_closed_out !out_channel' >|= fun out_closed_after_handler ->

      !out_open_in_handler &&
      !in_open_in_handler &&
      in_closed_after_handler &&
      out_closed_after_handler);

  test ~sequential:true "establish_server: implicit close on exception"
    (fun () ->
      let open Establish_server in

      let in_channel' = ref Lwt_io.stdin in
      let out_channel' = ref Lwt_io.stdout in
      let exit_raised = ref false in

      let run () =
        Establish_server.with_client
          (fun (in_channel, out_channel) ->
            in_channel' := in_channel;
            out_channel' := out_channel;
            raise Exit)
      in

      with_async_exception_hook
        (function
        | Exit -> exit_raised := true;
        | _ -> ())
        run

      >>= fun () ->
      (* See comment in other implicit close test. *)
      Lwt_unix.sleep 0.05 >>= fun () ->

      is_closed_in !in_channel' >>= fun in_closed_after_handler ->
      is_closed_out !out_channel' >|= fun out_closed_after_handler ->

      in_closed_after_handler && out_closed_after_handler);

  (* This does a simple double close of the channels (second close is implicit).
     If something breaks, the test will finish with an exception, or
     Lwt.async_exception_hook will kill the process. *)
  test "establish_server: explicit close"
    (fun () ->
      let open Establish_server in

      let closed_explicitly = ref false in

      let run =
        Establish_server.with_client
          (fun (in_channel, out_channel) ->
            Lwt_io.close in_channel >>= fun () ->
            Lwt_io.close out_channel >>= fun () ->
            is_closed_in in_channel >>= fun in_closed_in_handler ->
            is_closed_out out_channel >|= fun out_closed_in_handler ->
            closed_explicitly := in_closed_in_handler && out_closed_in_handler)
      in

      run >|= fun () ->
      !closed_explicitly);

  test "with_connection"
    (fun () ->
      let open Establish_server in

      let in_channel' = ref Lwt_io.stdin in
      let out_channel' = ref Lwt_io.stdout in

      let local = local () in

      Lwt_io.establish_server_with_client_address local
        (fun _client_address _channels -> Lwt.return_unit)
      >>= fun server ->

      Lwt_io.with_connection local (fun (in_channel, out_channel) ->
        in_channel' := in_channel;
        out_channel' := out_channel;
        Lwt.return_unit)

      >>= fun () ->
      Lwt_io.shutdown_server server >>= fun () ->
      is_closed_in !in_channel' >>= fun in_closed ->
      is_closed_out !out_channel' >|= fun out_closed ->
      in_closed && out_closed);

  (* Makes the channel fail with EBADF on close. Tries to close the channel
     manually, and handles the exception. When with_close_connection tries to
     close the socket again implicitly, that should not raise the exception
     again. *)
  test "with_close_connection: no duplicate exceptions"
    (fun () ->
      let exceptions_observed = ref 0 in

      let expecting_ebadf f =
        Lwt.catch f
          (function
            | Unix.Unix_error (Unix.EBADF, _, _) ->
              exceptions_observed := !exceptions_observed + 1;
              Lwt.return_unit
            | exn ->
              Lwt.fail exn) [@ocaml.warning "-4"]
      in

      let fd_r, fd_w = Lwt_unix.pipe () in
      let in_channel = Lwt_io.of_fd ~mode:Lwt_io.input fd_r in
      let out_channel = Lwt_io.of_fd ~mode:Lwt_io.output fd_w in

      Lwt_unix.close fd_r >>= fun () ->
      Lwt_unix.close fd_w >>= fun () ->

      expecting_ebadf (fun () ->
        Lwt_io.with_close_connection
          (fun _ ->
            expecting_ebadf (fun () -> Lwt_io.close in_channel) >>= fun () ->
            expecting_ebadf (fun () -> Lwt_io.close out_channel))
          (in_channel, out_channel))
      >|= fun () ->
      !exceptions_observed = 2);

  test "open_temp_file"
    (fun () ->
       Lwt_io.open_temp_file () >>= fun (fname, out_chan) ->
       Lwt_io.write out_chan "test file content" >>= fun () ->
       Lwt_io.close out_chan >>= fun _ ->
       Unix.unlink fname; Lwt.return_true
    );

  test "with_temp_filename"
    (fun () ->
       let prefix = "test_tempfile" in
      let filename = ref "." in
      let wrap f (filename', chan) = filename := filename'; f chan in
      let write_data chan = Lwt_io.write chan "test file content" in
       let write_data_fail _ = Lwt.fail Dummy_error in
      Lwt_io.with_temp_file (wrap write_data) ~prefix >>= fun _ ->
      let no_temps1 = not (Sys.file_exists !filename) in
       Lwt.catch
         (fun () -> Lwt_io.with_temp_file (wrap write_data_fail))
         (fun exn ->
            if exn = Dummy_error
            then Lwt.return (not (Sys.file_exists !filename))
            else Lwt.return_false
         )
       >>= fun no_temps2 ->
       Lwt.return (no_temps1 && no_temps2)
    );

  (* Verify that no exceptions are thrown if the function passed to
     with_temp_file closes the channel on its own. *)
  test "with_temp_filename close handle"
    (fun () ->
       let f (_, chan) = Lwt_io.write chan "test file content" >>= fun _ ->
         Lwt_io.close chan in
       Lwt_io.with_temp_file f >>= fun _ -> Lwt.return_true;
    );

  test "create_temp_dir" begin fun () ->
    let prefix = "temp_dir" in
    let suffix = "_foo" in
    Lwt_io.create_temp_dir ~parent:Filename.current_dir_name ~prefix ~suffix ()
      >>= fun path ->

    let name = Filename.basename path in
    let prefix_matches = String.sub name 0 (String.length prefix) = prefix in
    let actual_suffix =
      String.sub
        name (String.length name - String.length suffix) (String.length suffix)
    in
    let suffix_matches = actual_suffix = suffix in
    let directory_exists = Sys.is_directory path in

    Lwt_unix.rmdir path >>= fun () ->

    Lwt.return (prefix_matches && suffix_matches && directory_exists)
  end;

  test "with_temp_dir" ~sequential:true begin fun () ->
    Lwt_io.with_temp_dir ~parent:Filename.current_dir_name ~prefix:"temp_dir"
        begin fun path ->

      let directory_existed = Sys.is_directory path in

      open_out (Filename.concat path "foo") |> close_out;
      open_out (Filename.concat path "bar") |> close_out;
      let had_files = Array.length (Sys.readdir path) = 2 in

      Lwt.return (path, directory_existed, had_files)
    end >>= fun (path, directory_existed, had_files) ->

    let directory_removed = not (Sys.file_exists path) in

    Lwt.return (directory_existed && had_files && directory_removed)
  end;

  test "file_length on directory" begin fun () ->
    Lwt.catch
      (fun () ->
        Lwt_io.file_length "." >>= fun _ ->
        Lwt.return false)
      (function
      | Unix.Unix_error (Unix.EISDIR, "file_length", ".") ->
        Lwt.return true
      | exn -> Lwt.fail exn)
  end;

  test "input channel of_bytes initial position"
    (fun () ->
       let ichan = Lwt_io.of_bytes ~mode:Lwt_io.input @@ Lwt_bytes.of_string "abcd" in
       Lwt.return (Lwt_io.position ichan = 0L)
    );

  test "input channel of_bytes position after read"
    (fun () ->
       let ichan = Lwt_io.of_bytes ~mode:Lwt_io.input @@ Lwt_bytes.of_string "abcd" in
       Lwt_io.read_char ichan >|= fun _ ->
       Lwt_io.position ichan = 1L
    );

  test "input channel of_bytes position after set_position"
    (fun () ->
       let ichan = Lwt_io.of_bytes ~mode:Lwt_io.input @@ Lwt_bytes.of_string "abcd" in
       Lwt_io.set_position ichan 2L >|= fun () ->
       Lwt_io.position ichan = 2L
    );

  test "output channel of_bytes initial position"
    (fun () ->
       let ochan = Lwt_io.of_bytes ~mode:Lwt_io.output @@ Lwt_bytes.create 4 in
       Lwt.return (Lwt_io.position ochan = 0L)
    );

  test "output channel of_bytes position after read"
    (fun () ->
       let ochan = Lwt_io.of_bytes ~mode:Lwt_io.output @@ Lwt_bytes.create 4 in
       Lwt_io.write_char ochan 'a' >|= fun _ ->
       Lwt_io.position ochan = 1L
    );

  test "output channel of_bytes position after set_position"
    (fun () ->
       let ochan = Lwt_io.of_bytes ~mode:Lwt_io.output @@ Lwt_bytes.create 4 in
       Lwt_io.set_position ochan 2L >|= fun _ ->
       Lwt_io.position ochan = 2L
    );

  test "NumberIO.LE.read_int" begin fun () ->
    Lwt_bytes.of_string "\x01\x02\x03\x04"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.LE.read_int
    >|= (=) 0x04030201
  end;

  test "NumberIO.BE.read_int" begin fun () ->
    Lwt_bytes.of_string "\x01\x02\x03\x04"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.BE.read_int
    >|= (=) 0x01020304
  end;

  test "NumberIO.LE.read_int16" begin fun () ->
    Lwt_bytes.of_string "\x01\x02"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.LE.read_int16
    >|= (=) 0x0201
  end;

  test "NumberIO.BE.read_int16" begin fun () ->
    Lwt_bytes.of_string "\x01\x02"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.BE.read_int16
    >|= (=) 0x0102
  end;

  test "NumberIO.LE.read_int16, negative" begin fun () ->
    Lwt_bytes.of_string "\xfe\xff"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.LE.read_int16
    >|= (=) (-2)
  end;

  test "NumberIO.BE.read_int16, negative" begin fun () ->
    Lwt_bytes.of_string "\xff\xfe"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.BE.read_int16
    >|= (=) (-2)
  end;

  test "NumberIO.LE.read_int32" begin fun () ->
    Lwt_bytes.of_string "\x01\x02\x03\x04"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.LE.read_int32
    >|= (=) 0x04030201l
  end;

  test "NumberIO.BE.read_int32" begin fun () ->
    Lwt_bytes.of_string "\x01\x02\x03\x04"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.BE.read_int32
    >|= (=) 0x01020304l
  end;

  test "NumberIO.LE.read_int64" begin fun () ->
    Lwt_bytes.of_string "\x01\x02\x03\x04\x05\x06\x07\x08"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.LE.read_int64
    >|= (=) 0x0807060504030201L
  end;

  test "NumberIO.BE.read_int64" begin fun () ->
    Lwt_bytes.of_string "\x01\x02\x03\x04\x05\x06\x07\x08"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.BE.read_int64
    >|= (=) 0x0102030405060708L
  end;

  test "NumberIO.LE.read_float32" begin fun () ->
    Lwt_bytes.of_string "\x80\x01\x81\x47"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.LE.read_float32
    >|= (=) 66051.
  end;

  test "NumberIO.BE.read_float32" begin fun () ->
    Lwt_bytes.of_string "\x47\x81\x01\x80"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.BE.read_float32
    >|= (=) 66051.
  end;

  test "NumberIO.LE.read_float64" begin fun () ->
    Lwt_bytes.of_string "\x70\x60\x50\x40\x30\x20\xf0\x42"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.LE.read_float64
    >|= Int64.bits_of_float
    >|= (=) 0x42F0203040506070L
  end;

  test "NumberIO.BE.read_float64" begin fun () ->
    Lwt_bytes.of_string "\x42\xf0\x20\x30\x40\x50\x60\x70"
    |> Lwt_io.(of_bytes ~mode:input)
    |> Lwt_io.BE.read_float64
    >|= Int64.bits_of_float
    >|= (=) 0x42F0203040506070L
  end;

  test "NumberIO.LE.write_int" begin fun () ->
    let buffer = Lwt_bytes.create 4 in
    Lwt_io.LE.write_int (Lwt_io.(of_bytes ~mode:output) buffer)
      0x01020304 >>= fun () ->
    Lwt.return (Lwt_bytes.to_string buffer = "\x04\x03\x02\x01")
  end;

  test "NumberIO.BE.write_int" begin fun () ->
    let buffer = Lwt_bytes.create 4 in
    Lwt_io.BE.write_int (Lwt_io.(of_bytes ~mode:output) buffer)
      0x01020304 >>= fun () ->
    Lwt.return (Lwt_bytes.to_string buffer = "\x01\x02\x03\x04")
  end;

  test "NumberIO.LE.write_int16" begin fun () ->
    let buffer = Lwt_bytes.create 2 in
    Lwt_io.LE.write_int16 (Lwt_io.(of_bytes ~mode:output) buffer)
      0x0102 >>= fun () ->
    Lwt.return (Lwt_bytes.to_string buffer = "\x02\x01")
  end;

  test "NumberIO.BE.write_int16" begin fun () ->
    let buffer = Lwt_bytes.create 2 in
    Lwt_io.BE.write_int16 (Lwt_io.(of_bytes ~mode:output) buffer)
      0x0102 >>= fun () ->
    Lwt.return (Lwt_bytes.to_string buffer = "\x01\x02")
  end;

  test "NumberIO.LE.write_int32" begin fun () ->
    let buffer = Lwt_bytes.create 4 in
    Lwt_io.LE.write_int32 (Lwt_io.(of_bytes ~mode:output) buffer)
      0x01020304l >>= fun () ->
    Lwt.return (Lwt_bytes.to_string buffer = "\x04\x03\x02\x01")
  end;

  test "NumberIO.BE.write_int32" begin fun () ->
    let buffer = Lwt_bytes.create 4 in
    Lwt_io.BE.write_int32 (Lwt_io.(of_bytes ~mode:output) buffer)
      0x01020304l >>= fun () ->
    Lwt.return (Lwt_bytes.to_string buffer = "\x01\x02\x03\x04")
  end;

  test "NumberIO.LE.write_int64" begin fun () ->
    let buffer = Lwt_bytes.create 8 in
    Lwt_io.LE.write_int64 (Lwt_io.(of_bytes ~mode:output) buffer)
      0x0102030405060708L >>= fun () ->
    Lwt.return (Lwt_bytes.to_string buffer = "\x08\x07\x06\x05\x04\x03\x02\x01")
  end;

  test "NumberIO.BE.write_int64" begin fun () ->
    let buffer = Lwt_bytes.create 8 in
    Lwt_io.BE.write_int64 (Lwt_io.(of_bytes ~mode:output) buffer)
      0x0102030405060708L >>= fun () ->
    Lwt.return (Lwt_bytes.to_string buffer = "\x01\x02\x03\x04\x05\x06\x07\x08")
  end;

  test "NumberIO.LE.write_float32" begin fun () ->
    let buffer = Lwt_bytes.create 4 in
    Lwt_io.LE.write_float32 (Lwt_io.(of_bytes ~mode:output) buffer)
      66051. >>= fun () ->
    Lwt.return (Lwt_bytes.to_string buffer = "\x80\x01\x81\x47")
  end;

  test "NumberIO.BE.write_float32" begin fun () ->
    let buffer = Lwt_bytes.create 4 in
    Lwt_io.BE.write_float32 (Lwt_io.(of_bytes ~mode:output) buffer)
      66051. >>= fun () ->
    Lwt.return (Lwt_bytes.to_string buffer = "\x47\x81\x01\x80")
  end;

  test "NumberIO.LE.write_float64" begin fun () ->
    let buffer = Lwt_bytes.create 8 in
    Lwt_io.LE.write_float64 (Lwt_io.(of_bytes ~mode:output) buffer)
      (Int64.float_of_bits 0x42F0203040506070L) >>= fun () ->
    Lwt.return (Lwt_bytes.to_string buffer = "\x70\x60\x50\x40\x30\x20\xf0\x42")
  end;

  test "NumberIO.BE.write_float64" begin fun () ->
    let buffer = Lwt_bytes.create 8 in
    Lwt_io.BE.write_float64 (Lwt_io.(of_bytes ~mode:output) buffer)
      (Int64.float_of_bits 0x42F0203040506070L) >>= fun () ->
    Lwt.return (Lwt_bytes.to_string buffer = "\x42\xf0\x20\x30\x40\x50\x60\x70")
  end;
]
