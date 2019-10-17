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

module type EndianStringSig = sig
  (** Functions reading according to Big Endian byte order *)

  val get_char : string -> int -> char
  (** [get_char buff i] reads 1 byte at offset i as a char *)

  val get_uint8 : string -> int -> int
  (** [get_uint8 buff i] reads 1 byte at offset i as an unsigned int of 8
  bits. i.e. It returns a value between 0 and 2^8-1 *)

  val get_int8 : string -> int -> int
  (** [get_int8 buff i] reads 1 byte at offset i as a signed int of 8
  bits. i.e. It returns a value between -2^7 and 2^7-1 *)

  val get_uint16 : string -> int -> int
  (** [get_uint16 buff i] reads 2 bytes at offset i as an unsigned int
  of 16 bits. i.e. It returns a value between 0 and 2^16-1 *)

  val get_int16 : string -> int -> int
  (** [get_int16 buff i] reads 2 byte at offset i as a signed int of
  16 bits. i.e. It returns a value between -2^15 and 2^15-1 *)

  val get_int32 : string -> int -> int32
  (** [get_int32 buff i] reads 4 bytes at offset i as an int32. *)

  val get_int64 : string -> int -> int64
  (** [get_int64 buff i] reads 8 bytes at offset i as an int64. *)

  val get_float : string -> int -> float
  (** [get_float buff i] is equivalent to
      [Int32.float_of_bits (get_int32 buff i)] *)

  val get_double : string -> int -> float
  (** [get_double buff i] is equivalent to
      [Int64.float_of_bits (get_int64 buff i)] *)

  val set_char : Bytes.t -> int -> char -> unit
  (** @deprecated This is a deprecated alias of {!endianBytes.set_char}. *)

  val set_int8 : Bytes.t -> int -> int -> unit
  (** @deprecated This is a deprecated alias of {!endianBytes.set_int8}. *)

  val set_int16 : Bytes.t -> int -> int -> unit
  (** @deprecated This is a deprecated alias of {!endianBytes.set_int16}. *)

  val set_int32 : Bytes.t -> int -> int32 -> unit
  (** @deprecated This is a deprecated alias of {!endianBytes.set_int32}. *)

  val set_int64 : Bytes.t -> int -> int64 -> unit
  (** @deprecated This is a deprecated alias of {!endianBytes.set_int64}. *)

  val set_float : Bytes.t -> int -> float -> unit
  (** @deprecated This is a deprecated alias of {!endianBytes.set_float}. *)

  val set_double : Bytes.t -> int -> float -> unit
  (** @deprecated This is a deprecated alias of {!endianBytes.set_double}. *)

end

let get_char (s:string) off =
  String.get s off
let set_char (s:Bytes.t) off v =
  Bytes.set s off v
let unsafe_get_char (s:string) off =
  String.unsafe_get s off
let unsafe_set_char (s:Bytes.t) off v =
  Bytes.unsafe_set s off v

#include "common.ml"

external unsafe_get_16 : string -> int -> int = "%caml_string_get16u"
external unsafe_get_32 : string -> int -> int32 = "%caml_string_get32u"
external unsafe_get_64 : string -> int -> int64 = "%caml_string_get64u"

external unsafe_set_16 : Bytes.t -> int -> int -> unit = "%caml_string_set16u"
external unsafe_set_32 : Bytes.t -> int -> int32 -> unit = "%caml_string_set32u"
external unsafe_set_64 : Bytes.t -> int -> int64 -> unit = "%caml_string_set64u"

external get_16 : string -> int -> int = "%caml_string_get16"
external get_32 : string -> int -> int32 = "%caml_string_get32"
external get_64 : string -> int -> int64 = "%caml_string_get64"

external set_16 : Bytes.t -> int -> int -> unit = "%caml_string_set16"
external set_32 : Bytes.t -> int -> int32 -> unit = "%caml_string_set32"
external set_64 : Bytes.t -> int -> int64 -> unit = "%caml_string_set64"

#include "common_401.ml"
