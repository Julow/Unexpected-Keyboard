let _ =
  match%lwt
    Lwt.return ()
  with
  | () ->
    5;;
