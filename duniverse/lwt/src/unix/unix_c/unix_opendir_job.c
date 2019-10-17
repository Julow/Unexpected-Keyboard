/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <errno.h>
#include <dirent.h>
#include <sys/types.h>

#include "lwt_unix.h"

struct job_opendir {
    struct lwt_unix_job job;
    DIR *result;
    int error_code;
    char *path;
    char data[];
};

static void worker_opendir(struct job_opendir *job)
{
    job->result = opendir(job->path);
    job->error_code = errno;
}

static value result_opendir(struct job_opendir *job)
{
    LWT_UNIX_CHECK_JOB_ARG(job, job->result == NULL, "opendir", job->path);
    value result = caml_alloc_small(1, Abstract_tag);
    DIR_Val(result) = job->result;
    lwt_unix_free_job(&job->job);
    return result;
}

CAMLprim value lwt_unix_opendir_job(value path)
{
    LWT_UNIX_INIT_JOB_STRING(job, opendir, 0, path);
    return lwt_unix_alloc_job(&job->job);
}
#endif
