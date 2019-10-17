/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <winsock2.h>

CAMLprim value lwt_unix_somaxconn(value Unit)
{
    return Val_int(SOMAXCONN);
}

#endif
