/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>

CAMLprim value lwt_unix_get_page_size(value Unit)
{
    SYSTEM_INFO si;
    GetSystemInfo(&si);
    return Val_long(si.dwPageSize);
}
#endif
