/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/bigarray.h>
#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <string.h>

#include "lwt_unix.h"

#include "unix_readv_writev_utils.h"

void flatten_io_vectors(struct iovec *iovecs, value io_vectors,
                               size_t count, char **buffer_copies,
                               struct readv_copy_to *read_buffers)
{
    CAMLparam1(io_vectors);
    CAMLlocal3(node, io_vector, buffer);

    size_t index;
    size_t copy_index = 0;

    for (node = io_vectors, index = 0; index < count;
         node = Field(node, 1), ++index) {
        io_vector = Field(node, 0);

        intnat offset = Long_val(Field(io_vector, 1));
        intnat length = Long_val(Field(io_vector, 2));

        iovecs[index].iov_len = length;

        buffer = Field(Field(io_vector, 0), 0);
        if (Tag_val(Field(io_vector, 0)) == IO_vectors_bytes) {
            if (buffer_copies != NULL) {
                buffer_copies[copy_index] = lwt_unix_malloc(length);
                memcpy(buffer_copies[copy_index],
                       &Byte(String_val(buffer), offset), length);

                iovecs[index].iov_base = buffer_copies[copy_index];
                ++copy_index;
            } else if (read_buffers != NULL) {
                read_buffers[copy_index].temporary_buffer =
                    lwt_unix_malloc(length);
                read_buffers[copy_index].length = length;
                read_buffers[copy_index].offset = offset;
                read_buffers[copy_index].caml_buffer = buffer;
                caml_register_generational_global_root(
                    &read_buffers[copy_index].caml_buffer);

                iovecs[index].iov_base =
                    read_buffers[copy_index].temporary_buffer;
                ++copy_index;
            } else
                iovecs[index].iov_base = &Byte(String_val(buffer), offset);
        } else
            iovecs[index].iov_base =
                &((char *)Caml_ba_data_val(buffer))[offset];
    }

    if (buffer_copies != NULL) buffer_copies[copy_index] = NULL;
    if (read_buffers != NULL) read_buffers[copy_index].temporary_buffer = NULL;

    CAMLreturn0;
}
#endif
