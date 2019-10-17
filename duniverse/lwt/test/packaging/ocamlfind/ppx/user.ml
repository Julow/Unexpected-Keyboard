let () =
  let p =
    let%lwt () = Lwt.return () in
    Lwt.return ()
  in
  ignore p
