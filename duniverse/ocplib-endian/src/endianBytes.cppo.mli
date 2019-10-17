(************************************************************************)
(*  ocplib-endian                                                       *)
(*                                                                      *)
(*    Copyright 2014 OCamlPro                                           *)
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

module type EndianBytesSig = sig
  (** Functions reading according to Big Endian byte order *)

  val get_char : Bytes.t -> int -> char
  (** [get_char buff i] reads 1 byte at offset i as a char *)

  val get_uint8 : Bytes.t -> int -> int
  (** [get_uint8 buff i] reads 1 byte at offset i as an unsigned int of 8
  bits. i.e. It returns a value between 0 and 2^8-1 *)

  val get_int8 : Bytes.t -> int -> int
  (** [get_int8 buff i] reads 1 byte at offset i as a signed int of 8
  bits. i.e. It returns a value between -2^7 and 2^7-1 *)

  val get_uint16 : Bytes.t -> int -> int
  (** [get_uint16 buff i] reads 2 bytes at offset i as an unsigned int
  of 16 bits. i.e. It returns a value between 0 and 2^16-1 *)

  val get_int16 : Bytes.t -> int -> int
  (** [get_int16 buff i] reads 2 byte at offset i as a signed int of
  16 bits. i.e. It returns a value between -2^15 and 2^15-1 *)

  val get_int32 : Bytes.t -> int -> int32
  (** [get_int32 buff i] reads 4 bytes at offset i as an int32. *)

  val get_int64 : Bytes.t -> int -> int64
  (** [get_int64 buff i] reads 8 bytes at offset i as an int64. *)

  val get_float : Bytes.t -> int -> float
  (** [get_float buff i] is equivalent to
      [Int32.float_of_bits (get_int32 buff i)] *)

  val get_double : Bytes.t -> int -> float
  (** [get_double buff i] is equivalent to
      [Int64.float_of_bits (get_int64 buff i)] *)

  val set_char : Bytes.t -> int -> char -> unit
  (** [set_char buff i v] writes [v] to [buff] at offset [i] *)

  val set_int8 : Bytes.t -> int -> int -> unit
  (** [set_int8 buff i v] writes the least significant 8 bits of [v]
  to [buff] at offset [i] *)

  val set_int16 : Bytes.t -> int -> int -> unit
  (** [set_int16 buff i v] writes the least significant 16 bits of [v]
  to [buff] at offset [i] *)

  val set_int32 : Bytes.t -> int -> int32 -> unit
  (** [set_int32 buff i v] writes [v] to [buff] at offset [i] *)

  val set_int64 : Bytes.t -> int -> int64 -> unit
  (** [set_int64 buff i v] writes [v] to [buff] at offset [i] *)

  val set_float : Bytes.t -> int -> float -> unit
  (** [set_float buff i v] is equivalent to
      [set_int32 buff i (Int32.bits_of_float v)] *)

  val set_double : Bytes.t -> int -> float -> unit
  (** [set_double buff i v] is equivalent to
      [set_int64 buff i (Int64.bits_of_float v)] *)

end

module BigEndian : sig
  (** Functions reading according to Big Endian byte order without
  checking for overflow *)

  include EndianBytesSig

end

module BigEndian_unsafe : sig
  (** Functions reading according to Big Endian byte order without
  checking for overflow *)

  include EndianBytesSig

end

module LittleEndian : sig
  (** Functions reading according to Little Endian byte order *)

  include EndianBytesSig

end

module LittleEndian_unsafe : sig
  (** Functions reading according to Big Endian byte order without
  checking for overflow *)

  include EndianBytesSig

end

module NativeEndian : sig
  (** Functions reading according to machine endianness *)

  include EndianBytesSig

end

module NativeEndian_unsafe : sig
  (** Functions reading according to machine endianness without
  checking for overflow *)

  include EndianBytesSig

end
