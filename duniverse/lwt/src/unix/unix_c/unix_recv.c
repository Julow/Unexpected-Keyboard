/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <sys/types.h>
#include <sys/socket.h>

#include "unix_recv_send_utils.h"

value lwt_unix_recv(value fd, value buf, value ofs, value len, value flags)
{
    int ret;
    ret = recv(Int_val(fd), &Byte(String_val(buf), Long_val(ofs)),
               Long_val(len), caml_convert_flag_list(flags, msg_flag_table));
    if (ret == -1) uerror("recv", Nothing);
    return Val_int(ret);
}
#endif
