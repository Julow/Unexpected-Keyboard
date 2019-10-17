/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if defined(LWT_ON_WINDOWS)

#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>

CAMLprim value lwt_unix_read(value fd, value buf, value vofs, value vlen)
{
    intnat ofs, len, written;
    DWORD numbytes, numwritten;
    DWORD err = 0;

    Begin_root(buf);
    ofs = Long_val(vofs);
    len = Long_val(vlen);
    written = 0;
    if (len > 0) {
        numbytes = len;
        if (Descr_kind_val(fd) == KIND_SOCKET) {
            int ret;
            SOCKET s = Socket_val(fd);
            ret = recv(s, &Byte(buf, ofs), numbytes, 0);
            if (ret == SOCKET_ERROR) err = WSAGetLastError();
            numwritten = ret;
        } else {
            HANDLE h = Handle_val(fd);
            if (!ReadFile(h, &Byte(buf, ofs), numbytes, &numwritten, NULL))
                err = GetLastError();
        }
        if (err) {
            win32_maperr(err);
            uerror("write", Nothing);
        }
        written = numwritten;
    }
    End_roots();
    return Val_long(written);
}
#endif
