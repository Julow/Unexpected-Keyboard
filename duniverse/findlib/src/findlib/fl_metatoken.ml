(* $Id$
 * ----------------------------------------------------------------------
 *
 *)


type token =
    Name of string
  | LParen 
  | RParen
  | Equal
  | PlusEqual
  | Minus
  | Comma
  | String of string
  | Space
  | Newline
  | Eof
  | Unknown
;;


let name_tok = function
  | Name s -> Some s
  | _ -> None

let string_tok = function
  | String s -> Some s
  | _ -> None

let const_tok constant tok =
  match constant with
    | Name _ | String _ -> failwith "expect: only for constant tokens"
    | LParen | RParen | Equal | PlusEqual | Minus
    | Comma | Space | Newline | Eof | Unknown ->
      if constant = tok then Some ()
      else None
