  let get_uint16 s off =
    if not Sys.big_endian
    then swap16 (get_16 s off)
    else get_16 s off

  let get_int16 s off =
   ((get_uint16 s off) lsl ( Sys.word_size - 17 )) asr ( Sys.word_size - 17 )

  let get_int32 s off =
    if not Sys.big_endian
    then swap32 (get_32 s off)
    else get_32 s off

  let get_int64 s off =
    if not Sys.big_endian
    then swap64 (get_64 s off)
    else get_64 s off

  let set_int16 s off v =
    if not Sys.big_endian
    then (set_16 s off (swap16 v))
    else set_16 s off v

  let set_int32 s off v =
    if not Sys.big_endian
    then set_32 s off (swap32 v)
    else set_32 s off v

  let set_int64 s off v =
    if not Sys.big_endian
    then set_64 s off (swap64 v)
    else set_64 s off v
