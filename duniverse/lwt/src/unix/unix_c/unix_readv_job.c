/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <errno.h>
#include <string.h>

#include "lwt_unix.h"
#include "unix_readv_writev_utils.h"

/* Job and readv primitives for blocking file descriptors. */
struct job_readv {
    struct lwt_unix_job job;
    int fd;
    int error_code;
    ssize_t result;
    size_t count;
    /* Heap-allocated iovec structures. */
    struct iovec *iovecs;
    /* Data to be read into bytes buffers is first read into temporary buffers
       on the C heap. This is an array of descriptors for copying that data into
       the actual bytes buffers. The array is terminated by a descriptor whose
       temporary_buffer member is NULL. */
    struct readv_copy_to buffers[];
};

static void worker_readv(struct job_readv *job)
{
    job->result = readv(job->fd, job->iovecs, job->count);
    job->error_code = errno;
}

static value result_readv(struct job_readv *job)
{
    struct readv_copy_to *read_buffer;

    /* If the read is successful, copy data to the OCaml buffers. */
    if (job->result != -1) {
        for (read_buffer = job->buffers; read_buffer->temporary_buffer != NULL;
             ++read_buffer) {
            memcpy(&Byte(String_val(read_buffer->caml_buffer),
                         read_buffer->offset),
                   read_buffer->temporary_buffer, read_buffer->length);
        }
    }

    /* Free heap-allocated structures and buffers. */
    for (read_buffer = job->buffers; read_buffer->temporary_buffer != NULL;
         ++read_buffer) {
        free(read_buffer->temporary_buffer);
        caml_remove_generational_global_root(&read_buffer->caml_buffer);
    }
    free(job->iovecs);

    /* Decide on the actual result. */
    ssize_t result = job->result;
    LWT_UNIX_CHECK_JOB(job, result < 0, "readv");
    lwt_unix_free_job(&job->job);
    return Val_long(result);
}

CAMLprim value lwt_unix_readv_job(value fd, value io_vectors, value val_count)
{
    CAMLparam3(fd, io_vectors, val_count);

    size_t count = Long_val(val_count);

    /* The extra struct readv_copy_to (+ 1) is for the final terminator, in case
       all buffer slices are in bytes buffers. */
    LWT_UNIX_INIT_JOB(job, readv, sizeof(struct readv_copy_to) * (count + 1));
    job->fd = Int_val(fd);
    job->count = count;

    /* Assemble iovec structures on the heap. */
    job->iovecs = lwt_unix_malloc(sizeof(struct iovec) * count);
    flatten_io_vectors(job->iovecs, io_vectors, count, NULL, job->buffers);

    CAMLreturn(lwt_unix_alloc_job(&job->job));
}
#endif
