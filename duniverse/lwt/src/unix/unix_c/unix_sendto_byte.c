/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>

extern value lwt_unix_sendto(value fd, value buf, value ofs, value len,
                             value flags, value dest);

CAMLprim value lwt_unix_sendto_byte(value *argv, int argc)
{
    return lwt_unix_sendto(argv[0], argv[1], argv[2], argv[3], argv[4],
                           argv[5]);
}
#endif
