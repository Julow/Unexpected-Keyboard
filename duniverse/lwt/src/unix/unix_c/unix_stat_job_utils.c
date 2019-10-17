/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/memory.h>
#include <errno.h>

#include "unix_stat_job_utils.h"

value copy_stat(int use_64, struct stat *buf)
{
    CAMLparam0();
    CAMLlocal5(atime, mtime, ctime, offset, v);

    atime = caml_copy_double((double)buf->st_atime +
                             (NANOSEC(buf, a) / 1000000000.0));
    mtime = caml_copy_double((double)buf->st_mtime +
                             (NANOSEC(buf, m) / 1000000000.0));
    ctime = caml_copy_double((double)buf->st_ctime +
                             (NANOSEC(buf, c) / 1000000000.0));
    offset = use_64 ? caml_copy_int64(buf->st_size) : Val_int(buf->st_size);
    v = caml_alloc_small(12, 0);
    Field(v, 0) = Val_int(buf->st_dev);
    Field(v, 1) = Val_int(buf->st_ino);
    switch (buf->st_mode & S_IFMT) {
        case S_IFREG:
            Field(v, 2) = Val_int(0);
            break;
        case S_IFDIR:
            Field(v, 2) = Val_int(1);
            break;
        case S_IFCHR:
            Field(v, 2) = Val_int(2);
            break;
        case S_IFBLK:
            Field(v, 2) = Val_int(3);
            break;
        case S_IFLNK:
            Field(v, 2) = Val_int(4);
            break;
        case S_IFIFO:
            Field(v, 2) = Val_int(5);
            break;
        case S_IFSOCK:
            Field(v, 2) = Val_int(6);
            break;
        default:
            Field(v, 2) = Val_int(0);
            break;
    }
    Field(v, 3) = Val_int(buf->st_mode & 07777);
    Field(v, 4) = Val_int(buf->st_nlink);
    Field(v, 5) = Val_int(buf->st_uid);
    Field(v, 6) = Val_int(buf->st_gid);
    Field(v, 7) = Val_int(buf->st_rdev);
    Field(v, 8) = offset;
    Field(v, 9) = atime;
    Field(v, 10) = mtime;
    Field(v, 11) = ctime;
    CAMLreturn(v);
}

void worker_stat(struct job_stat *job)
{
    job->result = stat(job->name, &job->stat);
    job->error_code = errno;
}

value result_stat(struct job_stat *job)
{
    LWT_UNIX_CHECK_JOB_ARG(job, job->result < 0, "stat", job->name);
    value result = copy_stat(0, &job->stat);
    lwt_unix_free_job(&job->job);
    return result;
}

value result_stat_64(struct job_stat *job)
{
    LWT_UNIX_CHECK_JOB_ARG(job, job->result < 0, "stat", job->name);
    value result = copy_stat(1, &job->stat);
    lwt_unix_free_job(&job->job);
    return result;
}

void worker_lstat(struct job_lstat *job)
{
    job->result = lstat(job->name, &job->lstat);
    job->error_code = errno;
}

value result_lstat(struct job_lstat *job)
{
    LWT_UNIX_CHECK_JOB_ARG(job, job->result < 0, "lstat", job->name);
    value result = copy_stat(0, &(job->lstat));
    lwt_unix_free_job(&job->job);
    return result;
}

value result_lstat_64(struct job_lstat *job)
{
    LWT_UNIX_CHECK_JOB_ARG(job, job->result < 0, "lstat", job->name);
    value result = copy_stat(1, &(job->lstat));
    lwt_unix_free_job(&job->job);
    return result;
}

void worker_fstat(struct job_fstat *job)
{
    job->result = fstat(job->fd, &(job->fstat));
    job->error_code = errno;
}

value result_fstat(struct job_fstat *job)
{
    LWT_UNIX_CHECK_JOB(job, job->result < 0, "fstat");
    value result = copy_stat(0, &(job->fstat));
    lwt_unix_free_job(&job->job);
    return result;
}

value result_fstat_64(struct job_fstat *job)
{
    LWT_UNIX_CHECK_JOB(job, job->result < 0, "fstat");
    value result = copy_stat(1, &(job->fstat));
    lwt_unix_free_job(&job->job);
    return result;
}
#endif
