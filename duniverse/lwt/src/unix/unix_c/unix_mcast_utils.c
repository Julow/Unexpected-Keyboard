/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/fail.h>
#include <caml/mlvalues.h>
#include <caml/socketaddr.h>
#include <caml/unixsupport.h>

#include "unix_mcast_utils.h"

int socket_domain(int fd)
{
    /* Return the socket domain, PF_INET or PF_INET6. Fails for non-IP
       protos.
       fd must be a socket!
    */
    union sock_addr_union addr;
    socklen_t l;

    l = sizeof(addr);
    if (getsockname(fd, &addr.s_gen, &l) == -1) uerror("getsockname", Nothing);

    switch (addr.s_gen.sa_family) {
        case AF_INET:
            return PF_INET;
        case AF_INET6:
            return PF_INET6;
        default:
            caml_invalid_argument("Not an Internet socket");
    }

    return 0;
}
#endif
