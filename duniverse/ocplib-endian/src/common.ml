[@@@warning "-32"]

let sign8 v =
  (v lsl ( Sys.word_size - 9 )) asr ( Sys.word_size - 9 )

let sign16 v =
  (v lsl ( Sys.word_size - 17 )) asr ( Sys.word_size - 17 )

let get_uint8 s off =
  Char.code (get_char s off)
let get_int8 s off =
  ((get_uint8 s off) lsl ( Sys.word_size - 9 )) asr ( Sys.word_size - 9 )
let set_int8 s off v =
  (* It is ok to cast using unsafe_chr because both String.set
     and Bigarray.Array1.set (on bigstrings) use the 'store unsigned int8'
     primitives that effectively extract the bits before writing *)
  set_char s off (Char.unsafe_chr v)

let unsafe_get_uint8 s off =
  Char.code (unsafe_get_char s off)
let unsafe_get_int8 s off =
  ((unsafe_get_uint8 s off) lsl ( Sys.word_size - 9 )) asr ( Sys.word_size - 9 )
let unsafe_set_int8 s off v =
  unsafe_set_char s off (Char.unsafe_chr v)
