/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if defined(LWT_ON_WINDOWS)

#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>

#include "lwt_unix.h"

struct job_write {
    struct lwt_unix_job job;
    union {
        HANDLE handle;
        SOCKET socket;
    } fd;
    int kind;
    DWORD length;
    DWORD result;
    DWORD error_code;
    char buffer[];
};

static void worker_write(struct job_write *job)
{
    if (job->kind == KIND_SOCKET) {
        int ret;
        ret = send(job->fd.socket, job->buffer, job->length, 0);
        if (ret == SOCKET_ERROR) job->error_code = WSAGetLastError();
        job->result = ret;
    } else {
        if (!WriteFile(job->fd.handle, job->buffer, job->length, &(job->result),
                       NULL))
            job->error_code = GetLastError();
    }
}

static value result_write(struct job_write *job)
{
    value result;
    DWORD error = job->error_code;
    if (error) {
        lwt_unix_free_job(&job->job);
        win32_maperr(error);
        uerror("write", Nothing);
    }
    result = Val_long(job->result);
    lwt_unix_free_job(&job->job);
    return result;
}

CAMLprim value lwt_unix_write_job(value val_fd, value val_string,
                                  value val_offset, value val_length)
{
    struct filedescr *fd = (struct filedescr *)Data_custom_val(val_fd);
    long length = Long_val(val_length);
    LWT_UNIX_INIT_JOB(job, write, length);
    job->kind = fd->kind;
    if (fd->kind == KIND_HANDLE)
        job->fd.handle = fd->fd.handle;
    else
        job->fd.socket = fd->fd.socket;
    memcpy(job->buffer, String_val(val_string) + Long_val(val_offset), length);
    job->length = length;
    job->error_code = 0;
    return lwt_unix_alloc_job(&(job->job));
}
#endif
