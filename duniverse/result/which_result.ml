let () =
  let version =
    Scanf.sscanf Sys.argv.(1) "%d.%d" (fun major minor -> (major, minor))
  in
  let file =
    if version < (4, 03) then
      "result-as-newtype.ml"
    else
      "result-as-alias.ml"
  in
  print_string file
