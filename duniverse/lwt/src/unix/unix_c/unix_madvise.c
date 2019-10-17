/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <caml/bigarray.h>
#include <sys/mman.h>

static int advise_table[] = {
    MADV_NORMAL, MADV_RANDOM, MADV_SEQUENTIAL, MADV_WILLNEED, MADV_DONTNEED,
#if defined(MADV_MERGEABLE)
	MADV_MERGEABLE,
#else
	0,
#endif
#if defined(MADV_UNMERGEABLE)
	MADV_UNMERGEABLE,
#else
	0,
#endif
#if defined(MADV_HUGEPAGE)
	MADV_HUGEPAGE,
#else
	0,
#endif
#if defined(MADV_NOHUGEPAGE)
	MADV_NOHUGEPAGE,
#else
	0,
#endif
};

CAMLprim value lwt_unix_madvise(value val_buffer, value val_offset,
                                value val_length, value val_advice)
{
    int ret =
        madvise((char *)Caml_ba_data_val(val_buffer) + Long_val(val_offset),
                Long_val(val_length), advise_table[Int_val(val_advice)]);
    if (ret == -1) uerror("madvise", Nothing);
    return Val_unit;
}
#endif
