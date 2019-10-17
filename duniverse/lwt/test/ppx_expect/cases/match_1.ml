let _ =
  match%lwt
    Lwt.return 5
  with
  | () ->
    Lwt.return ();;
