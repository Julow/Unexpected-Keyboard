/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <errno.h>

#include "lwt_unix.h"
#include "unix_readv_writev_utils.h"

/* writev primitive for non-blocking file descriptors. */
CAMLprim value lwt_unix_writev(value fd, value io_vectors, value val_count)
{
    CAMLparam3(fd, io_vectors, val_count);

    size_t count = Long_val(val_count);

    /* Assemble iovec structures on the stack. No data is copied. */
    struct iovec iovecs[count];
    flatten_io_vectors(iovecs, io_vectors, count, NULL, NULL);

    ssize_t result = writev(Int_val(fd), iovecs, count);

    if (result == -1) uerror("writev", Nothing);

    CAMLreturn(Val_long(result));
}
#endif
