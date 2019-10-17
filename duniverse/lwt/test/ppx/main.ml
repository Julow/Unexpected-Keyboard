open Test
open Lwt

let suite = suite "ppx" [
  test "let"
    (fun () ->
       let%lwt x = return 3 in
       return (x + 1 = 4)
    ) ;

  test "nested let"
    (fun () ->
       let%lwt x = return 3 in
       let%lwt y = return 4 in
       return (x + y = 7)
    ) ;

  test "and let"
    (fun () ->
       let%lwt x = return 3
       and y = return 4 in
       return (x + y = 7)
    ) ;

  test "match"
    (fun () ->
       let x = Lwt.return (Some 3) in
       match%lwt x with
       | Some x -> return (x + 1 = 4)
       | None -> return false
    ) ;

  test "match-exn"
    (fun () ->
       let x = Lwt.return (Some 3) in
       let x' = Lwt.fail Not_found in
       let%lwt a =
         match%lwt x with
         | exception Not_found -> return false
         | Some x -> return (x = 3)
         | None -> return false
       and b =
         match%lwt x' with
         | exception Not_found -> return true
         | _ -> return false
       in
       Lwt.return (a && b)
    ) ;

  test "if"
    (fun () ->
       let x = Lwt.return true in
       let%lwt a =
         if%lwt x then Lwt.return_true else Lwt.return_false
       in
       let%lwt b =
         if%lwt x>|= not then Lwt.return_false else Lwt.return_true
       in
       (if%lwt x >|= not then Lwt.return_unit) >>= fun () ->
       Lwt.return (a && b)
    ) ;

  test "for" (* Test for proper sequencing *)
    (fun () ->
       let r = ref [] in
       let f x =
         let%lwt () = Lwt_unix.sleep 0.2 in Lwt.return (r := x :: !r)
       in
       let%lwt () =
         for%lwt x = 3 to 5 do f x done
       in return (!r = [5 ; 4 ; 3])
    ) ;

  test "while" (* Test for proper sequencing *)
    (fun () ->
       let r = ref [] in
       let f x =
         let%lwt () = Lwt_unix.sleep 0.2 in Lwt.return (r := x :: !r)
       in
       let%lwt () =
         let c = ref 2 in
         while%lwt !c < 5 do incr c ; f !c done
       in return (!r = [5 ; 4 ; 3])
    ) ;

  test "assert"
    (fun () ->
       let%lwt () = assert%lwt true
       in return true
    ) ;

  test "raise"
    (fun () ->
       Lwt.catch (fun () -> [%lwt raise Not_found])
         (fun exn -> return (exn = Not_found))
    ) ;

  test "try"
    (fun () ->
       try%lwt
         Lwt.fail Not_found
       with _ -> return true
    ) [@warning("@8@11")] ;

  test "try raise"
    (fun () ->
       try%lwt
         raise Not_found
       with _ -> return true
    ) [@warning("@8@11")] ;

  test "try fallback"
    (fun () ->
       try%lwt
         try%lwt
           Lwt.fail Not_found
         with Failure _ -> return false
       with Not_found -> return true
    ) [@warning("@8@11")] ;

  test "finally body"
    (fun () ->
       let x = ref false in
       begin
         (try%lwt
           return_unit
         with
         | _ -> return_unit
         ) [%finally x := true; return_unit]
       end >>= fun () ->
       return !x
    ) ;

  test "finally exn"
    (fun () ->
       let x = ref false in
       begin
         (try%lwt
           raise Not_found
         with
         | _ -> return_unit
         ) [%finally x := true; return_unit]
       end >>= fun () ->
       return !x
    ) ;

  test "finally exn default"
    (fun () ->
       let x = ref false in
       try%lwt
         ( raise Not_found )[%finally x := true; return_unit]
         >>= fun () ->
         return false
       with Not_found ->
         return !x
    ) ;

  test "structure let"
    (fun () ->
       let module M =
       struct
         let%lwt result = Lwt.return_true
       end
       in
       Lwt.return M.result
    ) ;
]

let _ = Test.run "ppx" [ suite ]
