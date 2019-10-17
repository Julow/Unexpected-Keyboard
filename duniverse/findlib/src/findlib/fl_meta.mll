(* $Id$
 * ----------------------------------------------------------------------
 *
 *)

{ open Fl_metatoken }

rule token =
  parse [ 'A'-'Z' 'a'-'z' '_' '0'-'9' '.' ]+ 
  {
    Name (Lexing.lexeme lexbuf)
  } 

  | '('
  {
    LParen
  } 

  | ')'
  { 
    RParen
  } 

  | "+="
  {
    PlusEqual
  }

  | '='
  {
    Equal
  } 

  | '-'
  {
    Minus
  }

  | ','
  {
    Comma
  } 

  | '"' [^ '"' '\\' ]* ( ( "\\\\" | "\\\"" ) [^ '"' '\\' ]* )* '"'
  {
    let s1 = Lexing.lexeme lexbuf in
    let s2 = String.sub s1 1 (String.length s1 - 2) in
    let l2 = String.length s2 in
    let b = Buffer.create 80 in
    let rec fill i =
      if i<l2 then
	match s2.[i] with
        | '\\' -> Buffer.add_char b s2.[i+1]; fill (i+2)
        | c    -> Buffer.add_char b c;        fill (i+1) in
    fill 0;
    String (Buffer.contents b)
  } 

  | [ ' ' '\t' '\r' ]
  { 
    Space
  } 

  | '\n'
  {
    Newline
  } 

  | '#' [^ '\n']* '\n'
  {
    Newline
  } 

  | '#' [^ '\n']* eof
  {
    Eof
  } 

  | eof
  {
    Eof
  }

  | _
  {
    Unknown
  }   

{}

