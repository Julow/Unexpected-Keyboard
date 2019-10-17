let () =
  let version =
    Scanf.sscanf Sys.argv.(1) "%d.%d" (fun major minor -> (major, minor)) in
  let implementation =
    if version < (4, 06) then
      "mmap_bigarray.ml"
    else
      "mmap_unix.ml"
  in
  print_string implementation
