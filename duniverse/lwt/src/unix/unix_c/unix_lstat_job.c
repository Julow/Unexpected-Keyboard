/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>

#include "lwt_unix.h"
#include "unix_stat_job_utils.h"

CAMLprim value lwt_unix_lstat_job(value name)
{
    LWT_UNIX_INIT_JOB_STRING(job, lstat, 0, name);
    return lwt_unix_alloc_job(&(job->job));
}
#endif
