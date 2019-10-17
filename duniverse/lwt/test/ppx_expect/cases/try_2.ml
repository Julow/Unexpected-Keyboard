let _ =
  try%lwt
    Lwt.return ()
  with _ -> 5;;
