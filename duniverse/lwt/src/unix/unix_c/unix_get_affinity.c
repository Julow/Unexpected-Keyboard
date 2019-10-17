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

CAMLprim value lwt_unix_get_affinity(value val_pid)
{
    CAMLparam1(val_pid);
    CAMLlocal2(list, node);
    cpu_set_t cpus;
    if (sched_getaffinity(Int_val(val_pid), sizeof(cpu_set_t), &cpus) < 0)
        uerror("sched_getaffinity", Nothing);
    int i;
    list = Val_int(0);
    for (i = sizeof(cpu_set_t) * 8 - 1; i >= 0; i--) {
        if (CPU_ISSET(i, &cpus)) {
            node = caml_alloc_tuple(2);
            Field(node, 0) = Val_int(i);
            Field(node, 1) = list;
            list = node;
        }
    }
    CAMLreturn(list);
}

#else

LWT_NOT_AVAILABLE1(unix_get_affinity)

#endif
#endif
