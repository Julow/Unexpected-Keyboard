/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <errno.h>
#include <string.h>

#include "lwt_unix.h"
#include "unix_termios_conversion.h"

struct job_tcgetattr {
    struct lwt_unix_job job;
    int fd;
    struct termios termios;
    int result;
    int error_code;
};

static void worker_tcgetattr(struct job_tcgetattr *job)
{
    job->result = tcgetattr(job->fd, &job->termios);
    job->error_code = errno;
}

static value result_tcgetattr(struct job_tcgetattr *job)
{
    LWT_UNIX_CHECK_JOB(job, job->result < 0, "tcgetattr");
    value res = caml_alloc_tuple(NFIELDS);
    encode_terminal_status(&job->termios, &Field(res, 0));
    lwt_unix_free_job(&job->job);
    return res;
}

CAMLprim value lwt_unix_tcgetattr_job(value fd)
{
    LWT_UNIX_INIT_JOB(job, tcgetattr, 0);
    job->fd = Int_val(fd);
    return lwt_unix_alloc_job(&job->job);
}
#endif
