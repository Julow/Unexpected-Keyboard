let _ =
  match%lwt
    Lwt.return ()
  with
  | () ->
    Lwt.return ()
  | exception End_of_file ->
    Lwt.return 5
