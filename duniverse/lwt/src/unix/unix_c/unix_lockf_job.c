/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>

#include "lwt_unix.h"

struct job_lockf {
    struct lwt_unix_job job;
    int fd;
    int command;
    long length;
    int result;
    int error_code;
};

#if defined(F_GETLK) && defined(F_SETLK) && defined(F_SETLKW)

static void worker_lockf(struct job_lockf *job)
{
    struct flock l;

    l.l_whence = 1;
    if (job->length < 0) {
        l.l_start = job->length;
        l.l_len = -job->length;
    } else {
        l.l_start = 0L;
        l.l_len = job->length;
    }
    switch (job->command) {
        case 0: /* F_ULOCK */
            l.l_type = F_UNLCK;
            job->result = fcntl(job->fd, F_SETLK, &l);
            job->error_code = errno;
            break;
        case 1: /* F_LOCK */
            l.l_type = F_WRLCK;
            job->result = fcntl(job->fd, F_SETLKW, &l);
            job->error_code = errno;
            break;
        case 2: /* F_TLOCK */
            l.l_type = F_WRLCK;
            job->result = fcntl(job->fd, F_SETLK, &l);
            job->error_code = errno;
            break;
        case 3: /* F_TEST */
            l.l_type = F_WRLCK;
            job->result = fcntl(job->fd, F_GETLK, &l);
            if (job->result != -1) {
                if (l.l_type == F_UNLCK) {
                    job->result = 0;
                } else {
                    job->result = -1;
                    job->error_code = EACCES;
                }
            }
            break;
        case 4: /* F_RLOCK */
            l.l_type = F_RDLCK;
            job->result = fcntl(job->fd, F_SETLKW, &l);
            job->error_code = errno;
            break;
        case 5: /* F_TRLOCK */
            l.l_type = F_RDLCK;
            job->result = fcntl(job->fd, F_SETLK, &l);
            job->error_code = errno;
            break;
        default:
            job->result = -1;
            job->error_code = EINVAL;
    }
}

#else

static int lock_command_table[] = {F_ULOCK, F_LOCK, F_TLOCK,
                                   F_TEST,  F_LOCK, F_TLOCK};

static void worker_lockf(struct job_lockf *job)
{
    job->result = lockf(job->fd, lock_command_table[job->command], job->length);
    job->error_code = errno;
}

#endif

static value result_lockf(struct job_lockf *job)
{
    LWT_UNIX_CHECK_JOB(job, job->result < 0, "lockf");
    lwt_unix_free_job(&job->job);
    return Val_unit;
}

CAMLprim value lwt_unix_lockf_job(value val_fd, value val_command,
                                  value val_length)
{
    LWT_UNIX_INIT_JOB(job, lockf, 0);
    job->fd = Int_val(val_fd);
    job->command = Int_val(val_command);
    job->length = Long_val(val_length);
    return lwt_unix_alloc_job(&job->job);
}
#endif
