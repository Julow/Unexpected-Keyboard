/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if defined(LWT_ON_WINDOWS)

#include <caml/fail.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>

#include "lwt_unix.h"

struct job_fsync {
    struct lwt_unix_job job;
    HANDLE handle;
    DWORD error_code;
};

static void worker_fsync(struct job_fsync *job)
{
    if (!FlushFileBuffers(job->handle)) job->error_code = GetLastError();
}

static value result_fsync(struct job_fsync *job)
{
    DWORD error = job->error_code;
    if (error) {
        lwt_unix_free_job(&job->job);
        win32_maperr(error);
        uerror("fsync", Nothing);
    }
    lwt_unix_free_job(&job->job);
    return Val_unit;
}

CAMLprim value lwt_unix_fsync_job(value val_fd)
{
    struct filedescr *fd = (struct filedescr *)Data_custom_val(val_fd);
    if (fd->kind != KIND_HANDLE) {
        caml_invalid_argument("Lwt_unix.fsync");
    } else {
        LWT_UNIX_INIT_JOB(job, fsync, 0);
        job->handle = fd->fd.handle;
        job->error_code = 0;
        return lwt_unix_alloc_job(&(job->job));
    }
}
#endif
