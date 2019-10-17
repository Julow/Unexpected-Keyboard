/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <errno.h>
#include <string.h>

#include "lwt_unix.h"

struct job_write {
    struct lwt_unix_job job;
    int fd;
    long length;
    long result;
    int error_code;
    char buffer[];
};

static void worker_write(struct job_write *job)
{
    job->result = write(job->fd, job->buffer, job->length);
    job->error_code = errno;
}

static value result_write(struct job_write *job)
{
    long result = job->result;
    LWT_UNIX_CHECK_JOB(job, result < 0, "write");
    lwt_unix_free_job(&job->job);
    return Val_long(result);
}

CAMLprim value lwt_unix_write_job(value val_fd, value val_string,
                                  value val_offset, value val_length)
{
    long length = Long_val(val_length);
    LWT_UNIX_INIT_JOB(job, write, length);
    job->fd = Int_val(val_fd);
    job->length = length;
    memcpy(job->buffer, String_val(val_string) + Long_val(val_offset), length);
    return lwt_unix_alloc_job(&(job->job));
}
#endif
