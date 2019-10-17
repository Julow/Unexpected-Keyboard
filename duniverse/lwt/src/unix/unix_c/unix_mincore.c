/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <caml/bigarray.h>
#include <unistd.h>
#include <sys/mman.h>

#include "lwt_unix.h"

#ifdef __CYGWIN__
LWT_NOT_AVAILABLE4(unix_mincore)
#elif defined __OpenBSD__
#include <sys/syscall.h>
#if !defined SYS_mincore
LWT_NOT_AVAILABLE4(unix_mincore)
#endif
#else

#ifdef HAVE_BSD_MINCORE
#define MINCORE_VECTOR_TYPE char
#else
#define MINCORE_VECTOR_TYPE unsigned char
#endif

CAMLprim value lwt_unix_mincore(value val_buffer, value val_offset,
                                value val_length, value val_states)
{
    long len = Wosize_val(val_states);
    MINCORE_VECTOR_TYPE vec[len];
    mincore((char *)Caml_ba_data_val(val_buffer) + Long_val(val_offset),
            Long_val(val_length), vec);
    long i;
    for (i = 0; i < len; i++) Field(val_states, i) = Val_bool(vec[i] & 1);
    return Val_unit;
}

#undef MINCORE_VECTOR_TYPE

#endif

#endif
