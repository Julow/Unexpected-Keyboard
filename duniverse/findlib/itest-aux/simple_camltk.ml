open Tk;;

let top = openTk() in 
Frx_after.idle
  closeTk;
mainLoop()
;;

print_string "OK\n"
;;

