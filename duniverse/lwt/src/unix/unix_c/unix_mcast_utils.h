/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#pragma once
#include "lwt_config.h"

/*
 * Included in:
 * - unix_mcast_modify_membership.c
 * - unix_mcast_set_loop.c
 * - unix_mcast_set_ttl.c
 * - unix_mcast_utils.c
 */

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>

int socket_domain(int fd);
#endif
