/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#define _GNU_SOURCE

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <sched.h>

#include "lwt_unix.h"

#if defined(HAVE_GETCPU)

CAMLprim value lwt_unix_get_cpu(value Unit)
{
    int cpu = sched_getcpu();
    if (cpu < 0) uerror("sched_getcpu", Nothing);
    return Val_int(cpu);
}

#else

LWT_NOT_AVAILABLE1(unix_get_cpu)

#endif
#endif
