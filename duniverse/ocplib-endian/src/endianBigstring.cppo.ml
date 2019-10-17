(************************************************************************)
(*  ocplib-endian                                                       *)
(*                                                                      *)
(*    Copyright 2012 OCamlPro                                           *)
(*                                                                      *)
(*  This file is distributed under the terms of the GNU Lesser General  *)
(*  Public License as published by the Free Software Foundation; either *)
(*  version 2.1 of the License, or (at your option) any later version,  *)
(*  with the OCaml static compilation exception.                        *)
(*                                                                      *)
(*  ocplib-endian is distributed in the hope that it will be useful,    *)
(*  but WITHOUT ANY WARRANTY; without even the implied warranty of      *)
(*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the       *)
(*  GNU General Public License for more details.                        *)
(*                                                                      *)
(************************************************************************)

open Bigarray_compat

type bigstring = (char, int8_unsigned_elt, c_layout) Array1.t

module type EndianBigstringSig = sig
  (** Functions reading according to Big Endian byte order *)

  val get_char : bigstring -> int -> char
  (** [get_char buff i] reads 1 byte at offset i as a char *)

  val get_uint8 : bigstring -> int -> int
  (** [get_uint8 buff i] reads 1 byte at offset i as an unsigned int of 8
  bits. i.e. It returns a value between 0 and 2^8-1 *)

  val get_int8 : bigstring -> int -> int
  (** [get_int8 buff i] reads 1 byte at offset i as a signed int of 8
  bits. i.e. It returns a value between -2^7 and 2^7-1 *)

  val get_uint16 : bigstring -> int -> int
  (** [get_uint16 buff i] reads 2 bytes at offset i as an unsigned int
  of 16 bits. i.e. It returns a value between 0 and 2^16-1 *)

  val get_int16 : bigstring -> int -> int
  (** [get_int16 buff i] reads 2 byte at offset i as a signed int of
  16 bits. i.e. It returns a value between -2^15 and 2^15-1 *)

  val get_int32 : bigstring -> int -> int32
  (** [get_int32 buff i] reads 4 bytes at offset i as an int32. *)

  val get_int64 : bigstring -> int -> int64
  (** [get_int64 buff i] reads 8 bytes at offset i as an int64. *)

  val get_float : bigstring -> int -> float
  (** [get_float buff i] is equivalent to
      [Int32.float_of_bits (get_int32 buff i)] *)

  val get_double : bigstring -> int -> float
  (** [get_double buff i] is equivalent to
      [Int64.float_of_bits (get_int64 buff i)] *)

  val set_char : bigstring -> int -> char -> unit
  (** [set_char buff i v] writes [v] to [buff] at offset [i] *)

  val set_int8 : bigstring -> int -> int -> unit
  (** [set_int8 buff i v] writes the least significant 8 bits of [v]
  to [buff] at offset [i] *)

  val set_int16 : bigstring -> int -> int -> unit
  (** [set_int16 buff i v] writes the least significant 16 bits of [v]
  to [buff] at offset [i] *)

  val set_int32 : bigstring -> int -> int32 -> unit
  (** [set_int32 buff i v] writes [v] to [buff] at offset [i] *)

  val set_int64 : bigstring -> int -> int64 -> unit
  (** [set_int64 buff i v] writes [v] to [buff] at offset [i] *)

  val set_float : bigstring -> int -> float -> unit
  (** [set_float buff i v] is equivalent to
      [set_int32 buff i (Int32.bits_of_float v)] *)

  val set_double : bigstring -> int -> float -> unit
  (** [set_double buff i v] is equivalent to
      [set_int64 buff i (Int64.bits_of_float v)] *)

end

let get_char (s:bigstring) off =
  Array1.get s off
let set_char (s:bigstring) off v =
  Array1.set s off v
let unsafe_get_char (s:bigstring) off =
  Array1.unsafe_get s off
let unsafe_set_char (s:bigstring) off v =
  Array1.unsafe_set s off v

#include "common.ml"

external unsafe_get_16 : bigstring -> int -> int = "%caml_bigstring_get16u"
external unsafe_get_32 : bigstring -> int -> int32 = "%caml_bigstring_get32u"
external unsafe_get_64 : bigstring -> int -> int64 = "%caml_bigstring_get64u"

external unsafe_set_16 : bigstring -> int -> int -> unit = "%caml_bigstring_set16u"
external unsafe_set_32 : bigstring -> int -> int32 -> unit = "%caml_bigstring_set32u"
external unsafe_set_64 : bigstring -> int -> int64 -> unit = "%caml_bigstring_set64u"

external get_16 : bigstring -> int -> int = "%caml_bigstring_get16"
external get_32 : bigstring -> int -> int32 = "%caml_bigstring_get32"
external get_64 : bigstring -> int -> int64 = "%caml_bigstring_get64"

external set_16 : bigstring -> int -> int -> unit = "%caml_bigstring_set16"
external set_32 : bigstring -> int -> int32 -> unit = "%caml_bigstring_set32"
external set_64 : bigstring -> int -> int64 -> unit = "%caml_bigstring_set64"

#include "common_401.ml"
