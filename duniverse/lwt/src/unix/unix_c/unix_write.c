/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <unistd.h>

CAMLprim value lwt_unix_write(value val_fd, value val_buf, value val_ofs,
                              value val_len)
{
    long ret;
    ret = write(Int_val(val_fd), &Byte(String_val(val_buf), Long_val(val_ofs)),
                Long_val(val_len));
    if (ret == -1) uerror("write", Nothing);
    return Val_long(ret);
}
#endif
