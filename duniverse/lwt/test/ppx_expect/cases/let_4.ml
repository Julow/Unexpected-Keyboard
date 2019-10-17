let _ =
  let%lwt foo = Lwt.return () and bar = Lwt.return 5 in
  Lwt.return (foo + bar);;
