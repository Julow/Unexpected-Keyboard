let _ =
  let%lwt () = Lwt.return 5 in
  Lwt.return ();;
