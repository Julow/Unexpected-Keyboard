/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#define _GNU_SOURCE

#include <caml/alloc.h>
#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <sched.h>

#include "lwt_unix.h"

#if defined(HAVE_AFFINITY)

CAMLprim value lwt_unix_set_affinity(value val_pid, value val_cpus)
{
    cpu_set_t cpus;
    CPU_ZERO(&cpus);
    for (; Is_block(val_cpus); val_cpus = Field(val_cpus, 1))
        CPU_SET(Int_val(Field(val_cpus, 0)), &cpus);
    if (sched_setaffinity(Int_val(val_pid), sizeof(cpu_set_t), &cpus) < 0)
        uerror("sched_setaffinity", Nothing);
    return Val_unit;
}

#else

LWT_NOT_AVAILABLE2(unix_set_affinity)

#endif
#endif
