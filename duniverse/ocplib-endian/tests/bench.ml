
let buffer_size = 10000
let loops = 10000

let allocdiff =
  let stat1 = Gc.quick_stat () in
  let stat2 = Gc.quick_stat () in
  (stat2.Gc.minor_words -. stat1.Gc.minor_words)

let test_fun s f =
  let t1 = Unix.gettimeofday () in
  let stat1 = Gc.quick_stat () in
  f ();
  let stat2 = Gc.quick_stat () in
  let t2 = Unix.gettimeofday () in
  Printf.printf "%s: time %f alloc: %f\n%!" s (t2 -. t1)
    (stat2.Gc.minor_words -. stat1.Gc.minor_words -. allocdiff)

module Bytes_test = struct
  open EndianBytes
  module BE = BigEndian
  module LE = LittleEndian

  let buffer = Bytes.create buffer_size

  let loop_read_uint16_be () =
    for i = 0 to Bytes.length buffer - 2 do
      ignore(BE.get_uint16 buffer i)
    done

  let loop_read_uint16_le () =
    for i = 0 to Bytes.length buffer - 2 do
      ignore(LE.get_uint16 buffer i)
    done

  let loop_read_int16_be () =
    for i = 0 to Bytes.length buffer - 2 do
      ignore(BE.get_int16 buffer i)
    done

  let loop_read_int16_le () =
    for i = 0 to Bytes.length buffer - 2 do
      ignore(LE.get_int16 buffer i)
    done

  let loop_read_int32_be () =
    for i = 0 to Bytes.length buffer - 4 do
      ignore(Int32.to_int (BE.get_int32 buffer i))
    done

  let loop_read_int32_le () =
    for i = 0 to Bytes.length buffer - 4 do
      ignore(Int32.to_int (LE.get_int32 buffer i))
    done

  let loop_read_int64_be () =
    for i = 0 to Bytes.length buffer - 8 do
      ignore(Int64.to_int (BE.get_int64 buffer i))
    done

  let loop_read_int64_le () =
    for i = 0 to Bytes.length buffer - 8 do
      ignore(Int64.to_int (LE.get_int64 buffer i))
    done

  let loop_write_int16_be () =
    for i = 0 to Bytes.length buffer - 2 do
      ignore(BE.set_int16 buffer i 10)
    done

  let loop_write_int16_le () =
    for i = 0 to Bytes.length buffer - 2 do
      ignore(LE.set_int16 buffer i 10)
    done

  let loop_write_int32_be () =
    for i = 0 to Bytes.length buffer - 4 do
      ignore((BE.set_int32 buffer i) 10l)
    done

  let loop_write_int32_le () =
    for i = 0 to Bytes.length buffer - 4 do
      ignore((LE.set_int32 buffer i) 10l)
    done

  let loop_write_int64_be () =
    for i = 0 to Bytes.length buffer - 8 do
      ignore((BE.set_int64 buffer i) 10L)
    done

  let loop_write_int64_le () =
    for i = 0 to Bytes.length buffer - 8 do
      ignore((LE.set_int64 buffer i) 10L)
    done

  let do_loop f () =
    for i = 0 to loops - 1 do
      f ()
    done

  let run s f = test_fun s (do_loop f)

  let run_test () =
    run "loop_read_uint16_be" loop_read_uint16_be;
    run "loop_read_uint16_le" loop_read_uint16_le;
    run "loop_read_int16_be" loop_read_int16_be;
    run "loop_read_int16_le" loop_read_int16_le;
    run "loop_read_int32_be" loop_read_int32_be;
    run "loop_read_int32_le" loop_read_int32_le;
    run "loop_read_int64_be" loop_read_int64_be;
    run "loop_read_int64_le" loop_read_int64_le;
    run "loop_write_int16_be" loop_write_int16_be;
    run "loop_write_int16_le" loop_write_int16_le;
    run "loop_write_int32_be" loop_write_int32_be;
    run "loop_write_int32_le" loop_write_int32_le;
    run "loop_write_int64_be" loop_write_int64_be;
    run "loop_write_int64_le" loop_write_int64_le

end

module Bytes_unsafe_test = struct
  open EndianBytes
  module BE = BigEndian_unsafe
  module LE = LittleEndian_unsafe

  let buffer = Bytes.create buffer_size

  let loop_read_uint16_be () =
    for i = 0 to Bytes.length buffer - 2 do
      ignore(BE.get_uint16 buffer i)
    done

  let loop_read_uint16_le () =
    for i = 0 to Bytes.length buffer - 2 do
      ignore(LE.get_uint16 buffer i)
    done

  let loop_read_int16_be () =
    for i = 0 to Bytes.length buffer - 2 do
      ignore(BE.get_int16 buffer i)
    done

  let loop_read_int16_le () =
    for i = 0 to Bytes.length buffer - 2 do
      ignore(LE.get_int16 buffer i)
    done

  let loop_read_int32_be () =
    for i = 0 to Bytes.length buffer - 4 do
      ignore(Int32.to_int (BE.get_int32 buffer i))
    done

  let loop_read_int32_le () =
    for i = 0 to Bytes.length buffer - 4 do
      ignore(Int32.to_int (LE.get_int32 buffer i))
    done

  let loop_read_int64_be () =
    for i = 0 to Bytes.length buffer - 8 do
      ignore(Int64.to_int (BE.get_int64 buffer i))
    done

  let loop_read_int64_le () =
    for i = 0 to Bytes.length buffer - 8 do
      ignore(Int64.to_int (LE.get_int64 buffer i))
    done

  let loop_write_int16_be () =
    for i = 0 to Bytes.length buffer - 2 do
      ignore(BE.set_int16 buffer i 10)
    done

  let loop_write_int16_le () =
    for i = 0 to Bytes.length buffer - 2 do
      ignore(LE.set_int16 buffer i 10)
    done

  let loop_write_int32_be () =
    for i = 0 to Bytes.length buffer - 4 do
      ignore((BE.set_int32 buffer i) 10l)
    done

  let loop_write_int32_le () =
    for i = 0 to Bytes.length buffer - 4 do
      ignore((LE.set_int32 buffer i) 10l)
    done

  let loop_write_int64_be () =
    for i = 0 to Bytes.length buffer - 8 do
      ignore((BE.set_int64 buffer i) 10L)
    done

  let loop_write_int64_le () =
    for i = 0 to Bytes.length buffer - 8 do
      ignore((LE.set_int64 buffer i) 10L)

   done

  let do_loop f () =
    for i = 0 to loops - 1 do
      f ()
    done

  let run s f = test_fun s (do_loop f)

  let run_test () =
    run "loop_read_uint16_be" loop_read_uint16_be;
    run "loop_read_uint16_le" loop_read_uint16_le;
    run "loop_read_int16_be" loop_read_int16_be;
    run "loop_read_int16_le" loop_read_int16_le;
    run "loop_read_int32_be" loop_read_int32_be;
    run "loop_read_int32_le" loop_read_int32_le;
    run "loop_read_int64_be" loop_read_int64_be;
    run "loop_read_int64_le" loop_read_int64_le;
    run "loop_write_int16_be" loop_write_int16_be;
    run "loop_write_int16_le" loop_write_int16_le;
    run "loop_write_int32_be" loop_write_int32_be;
    run "loop_write_int32_le" loop_write_int32_le;
    run "loop_write_int64_be" loop_write_int64_be;
    run "loop_write_int64_le" loop_write_int64_le

end

module Bigstring_test = struct
  open EndianBigstring
  module BE = BigEndian
  module LE = LittleEndian
  open Bigarray
  let buffer = Array1.create char c_layout buffer_size

  let loop_read_uint16_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 2 do
      ignore(BE.get_uint16 buffer i)
    done

  let loop_read_uint16_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 2 do
      ignore(LE.get_uint16 buffer i)
    done

  let loop_read_int16_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 2 do
      ignore(BE.get_int16 buffer i)
    done

  let loop_read_int16_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 2 do
      ignore(LE.get_int16 buffer i)
    done

  let loop_read_int32_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 4 do
      ignore(Int32.to_int (BE.get_int32 buffer i))
    done

  let loop_read_int32_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 4 do
      ignore(Int32.to_int (LE.get_int32 buffer i))
    done

  let loop_read_int64_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 8 do
      ignore(Int64.to_int (BE.get_int64 buffer i))
    done

  let loop_read_int64_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 8 do
      ignore(Int64.to_int (LE.get_int64 buffer i))
    done

  let loop_write_int16_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 2 do
      ignore(BE.set_int16 buffer i 10)
    done

  let loop_write_int16_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 2 do
      ignore(LE.set_int16 buffer i 10)
    done

  let loop_write_int32_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 4 do
      ignore((BE.set_int32 buffer i) 10l)
    done

  let loop_write_int32_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 4 do
      ignore((LE.set_int32 buffer i) 10l)
    done

  let loop_write_int64_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 8 do
      ignore((BE.set_int64 buffer i) 10L)
    done

  let loop_write_int64_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 8 do
      ignore((LE.set_int64 buffer i) 10L)
    done

  let do_loop f () =
    for i = 0 to loops - 1 do
      f ()
    done

  let run s f = test_fun s (do_loop f)

  let run_test () =
    run "loop_read_uint16_be" loop_read_uint16_be;
    run "loop_read_uint16_le" loop_read_uint16_le;
    run "loop_read_int16_be" loop_read_int16_be;
    run "loop_read_int16_le" loop_read_int16_le;
    run "loop_read_int32_be" loop_read_int32_be;
    run "loop_read_int32_le" loop_read_int32_le;
    run "loop_read_int64_be" loop_read_int64_be;
    run "loop_read_int64_le" loop_read_int64_le;
    run "loop_write_int16_be" loop_write_int16_be;
    run "loop_write_int16_le" loop_write_int16_le;
    run "loop_write_int32_be" loop_write_int32_be;
    run "loop_write_int32_le" loop_write_int32_le;
    run "loop_write_int64_be" loop_write_int64_be;
    run "loop_write_int64_le" loop_write_int64_le

end

module Bigstring_unsafe_test = struct
  open EndianBigstring
  module BE = BigEndian_unsafe
  module LE = LittleEndian_unsafe
  open Bigarray
  let buffer = Array1.create char c_layout buffer_size

  let loop_read_uint16_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 2 do
      ignore(BE.get_uint16 buffer i)
    done

  let loop_read_uint16_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 2 do
      ignore(LE.get_uint16 buffer i)
    done

  let loop_read_int16_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 2 do
      ignore(BE.get_int16 buffer i)
    done

  let loop_read_int16_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 2 do
      ignore(LE.get_int16 buffer i)
    done

  let loop_read_int32_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 4 do
      ignore(Int32.to_int (BE.get_int32 buffer i))
    done

  let loop_read_int32_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 4 do
      ignore(Int32.to_int (LE.get_int32 buffer i))
    done

  let loop_read_int64_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 8 do
      ignore(Int64.to_int (BE.get_int64 buffer i))
    done

  let loop_read_int64_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 8 do
      ignore(Int64.to_int (LE.get_int64 buffer i))
    done

  let loop_write_int16_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 2 do
      ignore(BE.set_int16 buffer i 10)
    done

  let loop_write_int16_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 2 do
      ignore(LE.set_int16 buffer i 10)
    done

  let loop_write_int32_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 4 do
      ignore((BE.set_int32 buffer i) 10l)
    done

  let loop_write_int32_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 4 do
      ignore((LE.set_int32 buffer i) 10l)
    done

  let loop_write_int64_be () =
    for i = 0 to Bigarray.Array1.dim buffer - 8 do
      ignore((BE.set_int64 buffer i) 10L)
    done

  let loop_write_int64_le () =
    for i = 0 to Bigarray.Array1.dim buffer - 8 do
      ignore((LE.set_int64 buffer i) 10L)
    done

  let do_loop f () =
    for i = 0 to loops - 1 do
      f ()
    done

  let run s f = test_fun s (do_loop f)

  let run_test () =
    run "loop_read_uint16_be" loop_read_uint16_be;
    run "loop_read_uint16_le" loop_read_uint16_le;
    run "loop_read_int16_be" loop_read_int16_be;
    run "loop_read_int16_le" loop_read_int16_le;
    run "loop_read_int32_be" loop_read_int32_be;
    run "loop_read_int32_le" loop_read_int32_le;
    run "loop_read_int64_be" loop_read_int64_be;
    run "loop_read_int64_le" loop_read_int64_le;
    run "loop_write_int16_be" loop_write_int16_be;
    run "loop_write_int16_le" loop_write_int16_le;
    run "loop_write_int32_be" loop_write_int32_be;
    run "loop_write_int32_le" loop_write_int32_le;
    run "loop_write_int64_be" loop_write_int64_be;
    run "loop_write_int64_le" loop_write_int64_le

end

let () =
  Printf.printf "safe bytes:\n%!";
  Bytes_test.run_test ();
  Printf.printf "unsafe bytes:\n%!";
  Bytes_unsafe_test.run_test ();
  Printf.printf "safe bigstring:\n%!";
  Bigstring_test.run_test ();
  Printf.printf "unsafe bigstring:\n%!";
  Bigstring_unsafe_test.run_test ()
