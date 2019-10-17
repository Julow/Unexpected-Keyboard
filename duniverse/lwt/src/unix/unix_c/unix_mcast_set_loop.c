/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/fail.h>
#include <caml/mlvalues.h>
#include <caml/socketaddr.h>
#include <caml/unixsupport.h>

#include "unix_mcast_utils.h"

CAMLprim value lwt_unix_mcast_set_loop(value fd, value flag)
{
    int t, r, f;

    t = socket_domain(Int_val(fd));
    f = Bool_val(flag);
    r = 0;

    switch (t) {
        case PF_INET:
            r = setsockopt(Int_val(fd), IPPROTO_IP, IP_MULTICAST_LOOP,
                           (void *)&f, sizeof(f));
            break;
        default:
            caml_invalid_argument("lwt_unix_mcast_set_loop");
    };

    if (r == -1) uerror("setsockopt", Nothing);

    return Val_unit;
}
#endif
