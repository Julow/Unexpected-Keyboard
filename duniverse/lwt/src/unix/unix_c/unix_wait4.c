/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/signals.h>
#include <caml/unixsupport.h>

#include <sys/types.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/wait.h>

/* Some code duplicated from OCaml's otherlibs/unix/wait.c */

CAMLextern int caml_convert_signal_number(int);
CAMLextern int caml_rev_convert_signal_number(int);

#if !defined(__ANDROID__)

#if !(defined(WIFEXITED) && defined(WEXITSTATUS) && defined(WIFSTOPPED) && \
      defined(WSTOPSIG) && defined(WTERMSIG))
/* Assume old-style V7 status word */
#define WIFEXITED(status) (((status)&0xFF) == 0)
#define WEXITSTATUS(status) (((status) >> 8) & 0xFF)
#define WIFSTOPPED(status) (((status)&0xFF) == 0xFF)
#define WSTOPSIG(status) (((status) >> 8) & 0xFF)
#define WTERMSIG(status) ((status)&0x3F)
#endif

#define TAG_WEXITED 0
#define TAG_WSIGNALED 1
#define TAG_WSTOPPED 2

static value alloc_process_status(int status)
{
    value st;

    if (WIFEXITED(status)) {
        st = caml_alloc_small(1, TAG_WEXITED);
        Field(st, 0) = Val_int(WEXITSTATUS(status));
    } else if (WIFSTOPPED(status)) {
        st = caml_alloc_small(1, TAG_WSTOPPED);
        Field(st, 0) =
            Val_int(caml_rev_convert_signal_number(WSTOPSIG(status)));
    } else {
        st = caml_alloc_small(1, TAG_WSIGNALED);
        Field(st, 0) =
            Val_int(caml_rev_convert_signal_number(WTERMSIG(status)));
    }
    return st;
}

static int wait_flag_table[] = {WNOHANG, WUNTRACED};

value lwt_unix_wait4(value flags, value pid_req)
{
    CAMLparam1(flags);
    CAMLlocal2(times, res);

    int pid, status, cv_flags;
    cv_flags = caml_convert_flag_list(flags, wait_flag_table);

    struct rusage ru;

    caml_enter_blocking_section();
    pid = wait4(Int_val(pid_req), &status, cv_flags, &ru);
    caml_leave_blocking_section();
    if (pid == -1) uerror("wait4", Nothing);

    times = caml_alloc_small(2 * Double_wosize, Double_array_tag);
    Store_double_field(times, 0,
                       ru.ru_utime.tv_sec + ru.ru_utime.tv_usec / 1e6);
    Store_double_field(times, 1,
                       ru.ru_stime.tv_sec + ru.ru_stime.tv_usec / 1e6);

    res = caml_alloc_tuple(3);
    Store_field(res, 0, Val_int(pid));
    Store_field(res, 1, alloc_process_status(status));
    Store_field(res, 2, times);
    CAMLreturn(res);
}

#else

#include "lwt_unix.h"

LWT_NOT_AVAILABLE2(unix_wait4)

#endif
#endif
