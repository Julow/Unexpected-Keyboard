/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <poll.h>

CAMLprim value lwt_unix_writable(value fd)
{
    struct pollfd pollfd;
    pollfd.fd = Int_val(fd);
    pollfd.events = POLLOUT;
    pollfd.revents = 0;
    if (poll(&pollfd, 1, 0) < 0) uerror("writable", Nothing);
    return (Val_bool(pollfd.revents & POLLOUT));
}
#endif
