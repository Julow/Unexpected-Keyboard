external swap16 : int -> int = "%bswap16"
external swap32 : int32 -> int32 = "%bswap_int32"
external swap64 : int64 -> int64 = "%bswap_int64"
external swapnative : nativeint -> nativeint = "%bswap_native"

module BigEndian = struct

  let get_char = get_char
  let get_uint8 = get_uint8
  let get_int8 = get_int8
  let set_char = set_char
  let set_int8 = set_int8

#include "be_ocaml_401.ml"
#include "common_float.ml"

end

module BigEndian_unsafe = struct

  let get_char = unsafe_get_char
  let get_uint8 = unsafe_get_uint8
  let get_int8 = unsafe_get_int8
  let set_char = unsafe_set_char
  let set_int8 = unsafe_set_int8
  let get_16 = unsafe_get_16
  let get_32 = unsafe_get_32
  let get_64 = unsafe_get_64
  let set_16 = unsafe_set_16
  let set_32 = unsafe_set_32
  let set_64 = unsafe_set_64

#include "be_ocaml_401.ml"
#include "common_float.ml"

end

module LittleEndian = struct

  let get_char = get_char
  let get_uint8 = get_uint8
  let get_int8 = get_int8
  let set_char = set_char
  let set_int8 = set_int8

#include "le_ocaml_401.ml"
#include "common_float.ml"

end

module LittleEndian_unsafe = struct

  let get_char = unsafe_get_char
  let get_uint8 = unsafe_get_uint8
  let get_int8 = unsafe_get_int8
  let set_char = unsafe_set_char
  let set_int8 = unsafe_set_int8
  let get_16 = unsafe_get_16
  let get_32 = unsafe_get_32
  let get_64 = unsafe_get_64
  let set_16 = unsafe_set_16
  let set_32 = unsafe_set_32
  let set_64 = unsafe_set_64

#include "le_ocaml_401.ml"
#include "common_float.ml"

end

module NativeEndian = struct

  let get_char = get_char
  let get_uint8 = get_uint8
  let get_int8 = get_int8
  let set_char = set_char
  let set_int8 = set_int8

#include "ne_ocaml_401.ml"
#include "common_float.ml"

end

module NativeEndian_unsafe = struct

  let get_char = unsafe_get_char
  let get_uint8 = unsafe_get_uint8
  let get_int8 = unsafe_get_int8
  let set_char = unsafe_set_char
  let set_int8 = unsafe_set_int8
  let get_16 = unsafe_get_16
  let get_32 = unsafe_get_32
  let get_64 = unsafe_get_64
  let set_16 = unsafe_set_16
  let set_32 = unsafe_set_32
  let set_64 = unsafe_set_64

#include "ne_ocaml_401.ml"
#include "common_float.ml"

end
