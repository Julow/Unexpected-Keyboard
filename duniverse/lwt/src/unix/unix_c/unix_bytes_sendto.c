/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/bigarray.h>
#include <caml/mlvalues.h>
#include <caml/socketaddr.h>
#include <caml/unixsupport.h>
#include <sys/types.h>
#include <sys/socket.h>

#include "unix_recv_send_utils.h"

value lwt_unix_bytes_sendto(value fd, value buf, value ofs, value len,
                            value flags, value dest)
{
    union sock_addr_union addr;
    socklen_t addr_len;
    int ret;
    get_sockaddr(dest, &addr, &addr_len);
    ret = sendto(Int_val(fd), (char *)Caml_ba_data_val(buf) + Long_val(ofs),
                 Long_val(len), caml_convert_flag_list(flags, msg_flag_table),
                 &addr.s_gen, addr_len);
    if (ret == -1) uerror("send", Nothing);
    return Val_int(ret);
}
#endif
