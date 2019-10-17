/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#pragma once
#include "lwt_config.h"

/*
 * header included in:
 * unix_writev
 * unix_writec_job
 * unix_readv
 * unix_readv_job
 */

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <sys/uio.h>

/* For blocking readv calls, arrays of this struct associate temporary buffers
   which are passed to the readv system call with the OCaml bytes buffers into
   which the data must ultimately be copied. */
struct readv_copy_to {
    /* Length of the temporary buffer. */
    size_t length;
    /* Offset into the OCaml buffer to which the temporary buffer must be
       copied. */
    size_t offset;
    value caml_buffer;
    char *temporary_buffer;
};

/* Tags for each of the constructors of type Lwt_unix.IO_vectors._buffer. The
   order must correspond to that in lwt_unix.ml. */
enum { IO_vectors_bytes, IO_vectors_bigarray };

/* Given an uninitialized array of iovec structures `iovecs`, and an OCaml value
   `io_vectors` of type Lwt_unix.IO_vectors._io_vector list, writes pointers to
   the first `count` buffer slices in `io_vectors` to `iovecs`. Each buffer
   slice may be a bytes buffer or a Bigarray buffer.

   In case `buffer_copies` is not NULL, a fresh buffer is allocated on the heap
   for each bytes buffer, and the contents of the bytes buffer are copied there.
   Pointers to these copies are written to `iovecs`, instead of pointers to the
   original buffers. The pointers are also stored as an array at
   `buffer_copies`, so that they can be freed later. This mechanism is used when
   `iovecs` will be passed to a blocking writev call, which is run by Lwt in a
   worker thread. In that case, the original, uncopied bytes buffers may be
   moved by the garbage collector before the I/O call runs, or while it is
   running.

   Similarly, in case `read_buffers` is not NULL, `flatten_io_vectors` allocates
   a temporary buffer for each OCaml bytes buffer. Pointers to the buffers are
   stored in `read_buffers`, together with GC roots for the corresponding OCaml
   buffers. */

void flatten_io_vectors(struct iovec *iovecs, value io_vectors, size_t count,
                        char **buffer_copies, struct readv_copy_to *read_buffers);
#endif
