/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

#include "lwt_unix.h"

struct job_guess_blocking {
    struct lwt_unix_job job;
    int fd;
    int result;
};

static void worker_guess_blocking(struct job_guess_blocking *job)
{
    struct stat stat;
    if (fstat(job->fd, &stat) == 0)
        job->result = !(S_ISFIFO(stat.st_mode) || S_ISSOCK(stat.st_mode));
    else
        job->result = 1;
}

static value result_guess_blocking(struct job_guess_blocking *job)
{
    value result = Val_bool(job->result);
    lwt_unix_free_job(&job->job);
    return result;
}

CAMLprim value lwt_unix_guess_blocking_job(value val_fd)
{
    LWT_UNIX_INIT_JOB(job, guess_blocking, 0);
    job->fd = Int_val(val_fd);
    return lwt_unix_alloc_job(&(job->job));
}
#endif
