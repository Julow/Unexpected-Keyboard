/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <sys/socket.h>

CAMLprim value lwt_unix_somaxconn(value unit)
{
    return Val_int(SOMAXCONN);
}

#endif
