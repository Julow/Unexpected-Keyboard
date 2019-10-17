/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include "lwt_unix.h"

LWT_NOT_AVAILABLE1(unix_is_socket)
LWT_NOT_AVAILABLE1(unix_system_job)
LWT_NOT_AVAILABLE1(unix_socketpair_stub)
LWT_NOT_AVAILABLE4(process_create_process)
LWT_NOT_AVAILABLE1(process_wait_job)
LWT_NOT_AVAILABLE2(process_terminate_process)
#endif
