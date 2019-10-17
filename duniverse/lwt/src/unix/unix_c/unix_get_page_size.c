/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <unistd.h>

CAMLprim value lwt_unix_get_page_size(value Unit)
{
    long page_size = sysconf(_SC_PAGESIZE);
    if (page_size < 0) page_size = 4096;
    return Val_long(page_size);
}
#endif
