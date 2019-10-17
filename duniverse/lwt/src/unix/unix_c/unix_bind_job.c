/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/socketaddr.h>
#include <caml/unixsupport.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>

#include "lwt_unix.h"

struct job_bind {
    struct lwt_unix_job job;
    int fd;
    union sock_addr_union addr;
    socklen_param_type addr_len;
    int result;
    int error_code;
};

static void worker_bind(struct job_bind *job)
{
    job->result = bind(job->fd, &job->addr.s_gen, job->addr_len);
    job->error_code = errno;
}

static value result_bind(struct job_bind *job)
{
    LWT_UNIX_CHECK_JOB(job, job->result != 0, "bind");
    lwt_unix_free_job(&job->job);
    return Val_unit;
}

CAMLprim value lwt_unix_bind_job(value fd, value address)
{
    LWT_UNIX_INIT_JOB(job, bind, 0);
    job->fd = Int_val(fd);
    get_sockaddr(address, &job->addr, &job->addr_len);

    return lwt_unix_alloc_job(&job->job);
}
#endif
