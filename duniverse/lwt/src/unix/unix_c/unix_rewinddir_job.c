/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <dirent.h>
#include <sys/types.h>

#include "lwt_unix.h"

struct job_rewinddir {
    struct lwt_unix_job job;
    DIR *dir;
};

static void worker_rewinddir(struct job_rewinddir *job) { rewinddir(job->dir); }

static value result_rewinddir(struct job_rewinddir *job)
{
    lwt_unix_free_job(&job->job);
    return Val_unit;
}

CAMLprim value lwt_unix_rewinddir_job(value dir)
{
    LWT_UNIX_INIT_JOB(job, rewinddir, 0);
    job->dir = DIR_Val(dir);
    return lwt_unix_alloc_job(&(job->job));
}
#endif
