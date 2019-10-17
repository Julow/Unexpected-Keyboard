(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



open Test
open Lwt.Infix

let testchan () =
  let b = Buffer.create 6 in
  let f buf ofs len =
    let bytes = Bytes.create len in
    Lwt_bytes.blit_to_bytes buf ofs bytes 0 len;
    Buffer.add_bytes b bytes;
    Lwt.return len
  in
  let oc = Lwt_io.make ~mode:Output f in
  let fmt = Lwt_fmt.of_channel oc in
  fmt, (fun () -> Buffer.contents b)

let suite = suite "lwt_fmt" [
    test "flushing" (fun () ->
        let fmt, f = testchan () in
        Lwt_fmt.fprintf fmt "%s%i%s%!" "bla" 3 "blo" >>= fun () ->
        Lwt.return (f () = {|bla3blo|})
      );
    test "with combinator" (fun () ->
        let fmt, f = testchan () in
        Lwt_fmt.fprintf fmt "%a%!" Format.pp_print_int 3 >>= fun () ->
        Lwt.return (f () = {|3|})
      );
    test "box" (fun () ->
        let fmt, f = testchan () in
        Lwt_fmt.fprintf fmt "@[<v2>%i@,%i@]%!" 1 2 >>= fun () ->
        Lwt.return (f () = "1\n  2")
      );
    test "boxsplit" (fun () ->
        let fmt, f = testchan () in
        Lwt_fmt.fprintf fmt "@[<v2>%i" 1 >>= fun () ->
        Lwt_fmt.fprintf fmt "@,%i@]" 2 >>= fun () ->
        Lwt_fmt.flush fmt >>= fun () ->
        Lwt.return (f () = "1\n  2")
      );
    test "box close with flush" (fun () ->
        let fmt, f = testchan () in
        Lwt_fmt.fprintf fmt "@[<v2>%i" 1 >>= fun () ->
        Lwt_fmt.fprintf fmt "@,%i" 2 >>= fun () ->
        Lwt_fmt.flush fmt >>= fun () ->
        Lwt.return (f () = "1\n  2")
      );

    test "stream"  (fun () ->
        let stream, fmt = Lwt_fmt.make_stream () in
        Lwt_fmt.fprintf fmt "@[<v2>%i@,%i@]%!" 1 2 >>= fun () ->
        Lwt.return (Lwt_stream.get_available stream = [
            String ("1", 0, 1);
            String ("\n", 0, 1);
            String  ("                                                                                ", 0, 2);
            String ("2", 0, 1);
            Flush])
      );
  ]
