(try 
  Sys.remove "itest-aux/testdb.db"
with 
  _ -> ());

let _ =
  Dbm.opendbm "itest-aux/testdb" [ Dbm.Dbm_rdwr; Dbm.Dbm_create ] 0o777 in

print_string "OK\n";;

