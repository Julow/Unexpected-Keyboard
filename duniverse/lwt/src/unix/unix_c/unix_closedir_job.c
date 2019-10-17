/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <errno.h>
#include <dirent.h>
#include <sys/types.h>

#include "lwt_unix.h"

struct job_closedir {
    struct lwt_unix_job job;
    int result;
    int error_code;
    DIR *dir;
};

static void worker_closedir(struct job_closedir *job)
{
    job->result = closedir(job->dir);
    job->error_code = errno;
}

static value result_closedir(struct job_closedir *job)
{
    LWT_UNIX_CHECK_JOB(job, job->dir < 0, "closedir");
    lwt_unix_free_job(&job->job);
    return Val_unit;
}

CAMLprim value lwt_unix_closedir_job(value dir)
{
    LWT_UNIX_INIT_JOB(job, closedir, 0);
    job->dir = DIR_Val(dir);
    return lwt_unix_alloc_job(&job->job);
}
#endif
