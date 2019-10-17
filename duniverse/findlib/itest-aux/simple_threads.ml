let tl =
  List.map
    (fun _ ->
      Thread.create 
        (fun () ->
           for i = 1 to 1000 do
              let _ = 1+1 in ()
           done)
        ())
    [ (); (); (); (); () ]
  in

  List.iter Thread.join tl
;;

print_string "OK\n";;

