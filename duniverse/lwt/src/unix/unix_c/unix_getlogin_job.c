/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <unistd.h>

#include "lwt_unix.h"

#if !defined(__ANDROID__)

struct job_getlogin {
    struct lwt_unix_job job;
    char buffer[1024];
    int result;
};

static void worker_getlogin(struct job_getlogin *job)
{
    job->result = getlogin_r(job->buffer, 1024);
}

static value result_getlogin(struct job_getlogin *job)
{
    int result = job->result;
    if (result) {
        lwt_unix_free_job(&job->job);
        unix_error(result, "getlogin", Nothing);
    } else {
        value v = caml_copy_string(job->buffer);
        lwt_unix_free_job(&job->job);
        return v;
    }
}

CAMLprim value lwt_unix_getlogin_job(value Unit)
{
    LWT_UNIX_INIT_JOB(job, getlogin, 0);
    return lwt_unix_alloc_job(&job->job);
}

#else

LWT_NOT_AVAILABLE1(unix_getlogin_job)

#endif
#endif
