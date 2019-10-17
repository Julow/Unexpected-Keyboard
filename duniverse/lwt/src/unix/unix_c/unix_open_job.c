/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <fcntl.h>

#include "lwt_unix.h"

#ifndef O_NONBLOCK
#define O_NONBLOCK O_NDELAY
#endif
#ifndef O_DSYNC
#define O_DSYNC 0
#endif
#ifndef O_SYNC
#define O_SYNC 0
#endif
#ifndef O_RSYNC
#define O_RSYNC 0
#endif

static int open_flag_table[] = {
    O_RDONLY, O_WRONLY, O_RDWR,  O_NONBLOCK, O_APPEND, O_CREAT, O_TRUNC,
    O_EXCL,   O_NOCTTY, O_DSYNC, O_SYNC,     O_RSYNC,  0, /* O_SHARE_DELETE,
                                                             Windows-only */
    0, /* O_CLOEXEC, treated specially */
    0  /* O_KEEPEXEC, treated specially */
};

enum { CLOEXEC = 1, KEEPEXEC = 2 };

static int open_cloexec_table[15] = {0, 0, 0, 0, 0, 0,       0,       0,
                                     0, 0, 0, 0, 0, CLOEXEC, KEEPEXEC};

struct job_open {
    struct lwt_unix_job job;
    int flags;
    int perms;
    int fd; /* will have value CLOEXEC or KEEPEXEC on entry to worker_open */
    int blocking;
    int error_code;
    char *name;
    char data[];
};

static void worker_open(struct job_open *job)
{
    int fd;
    int cloexec;

    if (job->fd & CLOEXEC)
        cloexec = 1;
    else if (job->fd & KEEPEXEC)
        cloexec = 0;
    else
#if OCAML_VERSION_MAJOR >= 4 && OCAML_VERSION_MINOR >= 5
        cloexec = unix_cloexec_default;
#else
        cloexec = 0;
#endif

#if defined(O_CLOEXEC)
    if (cloexec) job->flags |= O_CLOEXEC;
#endif

    fd = open(job->name, job->flags, job->perms);
#if !defined(O_CLOEXEC) && defined(FD_CLOEXEC)
    if (fd >= 0 && cloexec) {
        int flags = fcntl(fd, F_GETFD, 0);

        if (flags == -1 || fcntl(fd, F_SETFD, flags | FD_CLOEXEC) == -1) {
            int serrno = errno;
            close(fd);
            errno = serrno;
            fd = -1;
        }
    }
#endif
    job->fd = fd;
    job->error_code = errno;
    if (fd >= 0) {
        struct stat stat;
        if (fstat(fd, &stat) < 0)
            job->blocking = 1;
        else
            job->blocking = !(S_ISFIFO(stat.st_mode) || S_ISSOCK(stat.st_mode));
    }
}

static value result_open(struct job_open *job)
{
    int fd = job->fd;
    LWT_UNIX_CHECK_JOB_ARG(job, fd < 0, "open", job->name);
    value result = caml_alloc_tuple(2);
    Field(result, 0) = Val_int(fd);
    Field(result, 1) = Val_bool(job->blocking);
    lwt_unix_free_job(&job->job);
    return result;
}

CAMLprim value lwt_unix_open_job(value name, value flags, value perms)
{
    LWT_UNIX_INIT_JOB_STRING(job, open, 0, name);
    job->fd = caml_convert_flag_list(flags, open_cloexec_table);
    job->flags = caml_convert_flag_list(flags, open_flag_table);
    job->perms = Int_val(perms);
    return lwt_unix_alloc_job(&(job->job));
}
#endif
