(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



(** Module [Lwt_result]: explicit error handling *)

open Result

type (+'a, +'b) t = ('a, 'b) Result.result Lwt.t

let return x = Lwt.return (Ok x)
let fail e = Lwt.return (Error e)

let lift = Lwt.return
let ok x = Lwt.map (fun y -> Ok y) x

let map f e =
  Lwt.map
    (function
      | Error e -> Error e
      | Ok x -> Ok (f x))
    e

let map_err f e =
  Lwt.map
    (function
      | Error e -> Error (f e)
      | Ok x -> Ok x)
    e

let catch e =
  Lwt.catch
    (fun () -> ok e)
    fail

let get_exn e =
  Lwt.bind e
    (function
      | Ok x -> Lwt.return x
      | Error e -> Lwt.fail e)

let bind e f =
  Lwt.bind e
    (function
      | Error e -> Lwt.return (Error e)
      | Ok x -> f x)

let bind_lwt e f =
  Lwt.bind e
    (function
      | Ok x -> ok (f x)
      | Error e -> fail e)

let bind_result e f =
  Lwt.map
    (function
      | Error e -> Error e
      | Ok x -> f x)
    e

let bind_lwt_err e f =
  Lwt.bind e
    (function
      | Error e -> Lwt.bind (f e) fail
      | Ok x -> return x)

module Infix = struct
  let (>>=) = bind
  let (>|=) e f = map f e
end

include Infix
