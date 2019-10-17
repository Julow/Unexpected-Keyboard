include String

let empty = ""
let of_string = copy
let to_string = copy

let sub_string = sub
let blit_string = blit

let unsafe_to_string : t -> string = fun s -> s
let unsafe_of_string : string -> t = fun s -> s

let extend s left right =
  (* length of the final string *)
  let dstlen = left + length s + right in
  (* length of the included portion of the input string *)
  let srclen = min 0 left + length s + min 0 right in
  let t = create dstlen in
  if srclen > 0 then blit s (max 0 (-left)) t (max 0 left) srclen;
  t

let init len f =
  let s = create len in
  for i = 0 to len - 1 do
    set s i (f i);
  done;
  s

let mapi f input =
  let output = create (length input) in
  for i = 0 to length input - 1 do
    output.[i] <- f i input.[i];
  done;
  output

let cat = (^)
