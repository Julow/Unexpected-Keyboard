/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if defined(LWT_ON_WINDOWS)

#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>

CAMLprim value lwt_unix_is_socket(value fd)
{
    return (Val_bool(Descr_kind_val(fd) == KIND_SOCKET));
}
#endif
