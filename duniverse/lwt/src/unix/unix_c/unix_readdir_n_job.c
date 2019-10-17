/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <dirent.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>

#include "lwt_unix.h"

struct job_readdir_n {
    struct lwt_unix_job job;
    DIR *dir;
    /* count serves two purpose:
       1. Transmit the maximum number of entries requested by the programmer.
          See `readdir_n`'s OCaml side documentation.
       2. Transmit the number of actually read entries from the `worker` to
          the result parser.
          See below `worker_readdir_n` and `result_readdir_n`
    */
    long count;
    int error_code;
    char *entries[];
};

static void worker_readdir_n(struct job_readdir_n *job)
{
    long i;
    for (i = 0; i < job->count; i++) {
        errno = 0;
        struct dirent *entry = readdir(job->dir);

        /* An error happened. */
        if (entry == NULL && errno != 0) {
            job->count = i;
            job->error_code = errno;
            return;
        }

        /* End of directory reached */
        if (entry == NULL && errno == 0) break;

        /* readdir is good */
        char *name = strdup(entry->d_name);
        if (name == NULL) {
            job->count = i;
            job->error_code = errno;
            return;
        }

        /* All is good */
        job->entries[i] = name;
    }
    job->count = i;
    job->error_code = 0;
}

static value result_readdir_n(struct job_readdir_n *job)
{
    CAMLparam0();
    CAMLlocal1(result);
    int error_code = job->error_code;
    if (error_code) {
        long i;
        for (i = 0; i < job->count; i++) free(job->entries[i]);
        lwt_unix_free_job(&job->job);
        unix_error(error_code, "readdir", Nothing);
    } else {
        result = caml_alloc(job->count, 0);
        long i;
        for (i = 0; i < job->count; i++) {
            Store_field(result, i, caml_copy_string(job->entries[i]));
        }
        for (i = 0; i < job->count; i++) free(job->entries[i]);
        lwt_unix_free_job(&job->job);
        CAMLreturn(result);
    }
}

CAMLprim value lwt_unix_readdir_n_job(value val_dir, value val_count)
{
    long count = Long_val(val_count);
    DIR *dir = DIR_Val(val_dir);

    LWT_UNIX_INIT_JOB(job, readdir_n, sizeof(char *) * count);
    job->dir = dir;
    job->count = count;

    return lwt_unix_alloc_job(&job->job);
}
#endif
