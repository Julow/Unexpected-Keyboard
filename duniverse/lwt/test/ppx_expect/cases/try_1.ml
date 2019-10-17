let _ =
  try%lwt
    5
  with _ -> Lwt.return ();;
