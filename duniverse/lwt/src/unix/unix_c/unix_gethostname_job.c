/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>

#include <errno.h>
#include <stdlib.h>
#include <string.h>

#include "lwt_unix.h"

#include "unix_get_network_information_utils.h"

struct job_gethostname {
    struct lwt_unix_job job;
    char *buffer;
    int result;
    int error_code;
};

static void worker_gethostname(struct job_gethostname *job)
{
    int buffer_size = 64;
    int err;

    for (;;) {
        job->buffer = lwt_unix_malloc(buffer_size + 1);

        err = gethostname(job->buffer, buffer_size);

        if (err == -1 && errno == ENAMETOOLONG) {
            free(job->buffer);
            buffer_size *= 2;
        } else if (err == -1) {
            free(job->buffer);
            job->result = -1;
            job->error_code = errno;
            return;
        } else {
            job->buffer[buffer_size] = 0;
            job->result = 0;
            return;
        }
    }
}

static value result_gethostname(struct job_gethostname *job)
{
    LWT_UNIX_CHECK_JOB(job, job->result < 0, "gethostname");
    value result = caml_copy_string(job->buffer);
    free(job->buffer);
    lwt_unix_free_job(&job->job);
    return result;
}

CAMLprim value lwt_unix_gethostname_job(value Unit)
{
    LWT_UNIX_INIT_JOB(job, gethostname, 0);
    return lwt_unix_alloc_job(&(job->job));
}
#endif
