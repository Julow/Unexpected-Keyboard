let () =
  let open Lwt.Infix in

  let p =
    let%bind x = Lwt.return 1 in
    let%map y = Lwt.return (x + 1) in
    y + 1
  in

  let x = Lwt_main.run p in

  if x = 3 then
    exit 0
  else
    exit 1
