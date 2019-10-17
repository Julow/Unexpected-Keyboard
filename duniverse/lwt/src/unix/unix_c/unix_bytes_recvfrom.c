/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/bigarray.h>
#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/socketaddr.h>
#include <caml/unixsupport.h>
#include <sys/types.h>
#include <sys/socket.h>

#include "unix_recv_send_utils.h"

value lwt_unix_bytes_recvfrom(value fd, value buf, value ofs, value len,
                              value flags)
{
    CAMLparam5(fd, buf, ofs, len, flags);
    CAMLlocal2(result, address);
    int ret;
    union sock_addr_union addr;
    socklen_t addr_len;
    addr_len = sizeof(addr);
    ret = recvfrom(Int_val(fd), (char *)Caml_ba_data_val(buf) + Long_val(ofs),
                   Long_val(len), caml_convert_flag_list(flags, msg_flag_table),
                   &addr.s_gen, &addr_len);
    if (ret == -1) uerror("recvfrom", Nothing);
    address = alloc_sockaddr(&addr, addr_len, -1);
    result = caml_alloc_tuple(2);
    Field(result, 0) = Val_int(ret);
    Field(result, 1) = address;
    CAMLreturn(result);
}
#endif
