/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <errno.h>

#include "lwt_unix.h"
#include "unix_termios_conversion.h"

struct job_tcsetattr {
    struct lwt_unix_job job;
    int fd;
    int when;
    /* This array contains only non-allocated values. */
    value termios[NFIELDS];
    int result;
    int error_code;
};

static int when_flag_table[] = {TCSANOW, TCSADRAIN, TCSAFLUSH};

static void worker_tcsetattr(struct job_tcsetattr *job)
{
    struct termios termios;
    int result = tcgetattr(job->fd, &termios);
    if (result < 0) {
        job->result = result;
        job->error_code = errno;
    } else {
        decode_terminal_status(&termios, &(job->termios[0]));
        job->result = tcsetattr(job->fd, job->when, &termios);
        job->error_code = errno;
    }
}

static value result_tcsetattr(struct job_tcsetattr *job)
{
    LWT_UNIX_CHECK_JOB(job, job->result < 0, "tcsetattr");
    lwt_unix_free_job(&job->job);
    return Val_unit;
}

CAMLprim value lwt_unix_tcsetattr_job(value fd, value when, value termios)
{
    LWT_UNIX_INIT_JOB(job, tcsetattr, 0);
    job->fd = Int_val(fd);
    job->when = when_flag_table[Int_val(when)];
    memcpy(&job->termios, &Field(termios, 0), NFIELDS * sizeof(value));
    return lwt_unix_alloc_job(&job->job);
}
#endif
