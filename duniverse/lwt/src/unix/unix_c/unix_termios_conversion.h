/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#pragma once

#include "lwt_config.h"

/* Header included in:
 * - unix_tcsetattr_job.c
 */

#if !defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <termios.h>
#include <unistd.h>

#include "lwt_unix.h"

/* Number of fields in the terminal_io record field. Cf. unix.mli */

#define NFIELDS 38

void encode_terminal_status(struct termios *terminal_status, value *dst);
void decode_terminal_status(struct termios *terminal_status, value *src);
#endif
