/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <errno.h>
#include <sys/time.h>

#include "lwt_unix.h"

struct job_utimes {
    struct lwt_unix_job job;
    char *path;
    const struct timeval *times_pointer;
    struct timeval times[2];
    int result;
    int error_code;
    char data[];
};

static void worker_utimes(struct job_utimes *job)
{
    job->result = utimes(job->path, job->times_pointer);
    job->error_code = errno;
}

static value result_utimes(struct job_utimes *job)
{
    LWT_UNIX_CHECK_JOB_ARG(job, job->result != 0, "utimes", job->path);
    lwt_unix_free_job(&job->job);
    return Val_unit;
}

CAMLprim value lwt_unix_utimes_job(value path, value val_atime, value val_mtime)
{
    LWT_UNIX_INIT_JOB_STRING(job, utimes, 0, path);

    double atime = Double_val(val_atime);
    double mtime = Double_val(val_mtime);

    if (atime == 0.0 && mtime == 0.0)
        job->times_pointer = NULL;
    else {
        job->times[0].tv_sec = atime;
        job->times[0].tv_usec = (atime - job->times[0].tv_sec) * 1000000;

        job->times[1].tv_sec = mtime;
        job->times[1].tv_usec = (mtime - job->times[1].tv_sec) * 1000000;

        job->times_pointer = job->times;
    }

    return lwt_unix_alloc_job(&(job->job));
}
#endif
