/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if defined(LWT_ON_WINDOWS)

#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>

#include "lwt_unix.h"

struct job_read {
    struct lwt_unix_job job;
    union {
        HANDLE handle;
        SOCKET socket;
    } fd;
    int kind;
    DWORD length;
    DWORD result;
    DWORD error_code;
    value string;
    DWORD offset;
    char buffer[];
};

static void worker_read(struct job_read *job)
{
    if (job->kind == KIND_SOCKET) {
        int ret;
        ret = recv(job->fd.socket, job->buffer, job->length, 0);
        if (ret == SOCKET_ERROR) job->error_code = WSAGetLastError();
        job->result = ret;
    } else {
        if (!ReadFile(job->fd.handle, job->buffer, job->length, &(job->result),
                      NULL))
            job->error_code = GetLastError();
    }
}

static value result_read(struct job_read *job)
{
    value result;
    DWORD error = job->error_code;
    if (error) {
        caml_remove_generational_global_root(&job->string);
        lwt_unix_free_job(&job->job);
        win32_maperr(error);
        uerror("read", Nothing);
    }
    memcpy(String_val(job->string) + job->offset, job->buffer, job->result);
    result = Val_long(job->result);
    caml_remove_generational_global_root(&job->string);
    lwt_unix_free_job(&job->job);
    return result;
}

CAMLprim value lwt_unix_read_job(value val_fd, value val_string,
                                 value val_offset, value val_length)
{
    struct filedescr *fd = (struct filedescr *)Data_custom_val(val_fd);
    long length = Long_val(val_length);
    LWT_UNIX_INIT_JOB(job, read, length);
    job->kind = fd->kind;
    if (fd->kind == KIND_HANDLE)
        job->fd.handle = fd->fd.handle;
    else
        job->fd.socket = fd->fd.socket;
    job->length = length;
    job->error_code = 0;
    job->string = val_string;
    job->offset = Long_val(val_offset);
    caml_register_generational_global_root(&(job->string));
    return lwt_unix_alloc_job(&(job->job));
}
#endif
