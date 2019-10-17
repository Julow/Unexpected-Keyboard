/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>

#include "lwt_unix.h"
#include "unix_stat_job_utils.h"

CAMLprim value lwt_unix_fstat_64_job(value val_fd)
{
    LWT_UNIX_INIT_JOB(job, fstat, 0);
    job->job.result = (lwt_unix_job_result)result_fstat_64;
    job->fd = Int_val(val_fd);
    return lwt_unix_alloc_job(&(job->job));
}
#endif
