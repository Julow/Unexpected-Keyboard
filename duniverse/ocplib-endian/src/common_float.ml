
let get_float buff i = Int32.float_of_bits (get_int32 buff i)
let get_double buff i = Int64.float_of_bits (get_int64 buff i)
let set_float buff i v = set_int32 buff i (Int32.bits_of_float v)
let set_double buff i v = set_int64 buff i (Int64.bits_of_float v)
