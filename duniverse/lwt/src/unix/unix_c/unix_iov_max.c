/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#define _GNU_SOURCE

#include <caml/mlvalues.h>
#include <limits.h>

CAMLprim value lwt_unix_iov_max(value unit) { return Val_int(IOV_MAX); }
#endif
