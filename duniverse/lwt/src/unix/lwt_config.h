/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#ifndef _LWT_CONFIG_H_
#define _LWT_CONFIG_H_

#include "lwt_features.h"

#if defined(HAVE_GET_CREDENTIALS_LINUX) || \
    defined(HAVE_GET_CREDENTIALS_NETBSD) || \
    defined(HAVE_GET_CREDENTIALS_OPENBSD) || \
    defined(HAVE_GET_CREDENTIALS_FREEBSD) || \
    defined(HAVE_GETPEEREID)
#define HAVE_GET_CREDENTIALS
#endif

#if defined(HAVE_ST_MTIM_TV_NSEC)
#define NANOSEC(buf, field) buf->st_##field##tim.tv_nsec
#elif defined(HAVE_ST_MTIMESPEC_TV_NSEC)
#define NANOSEC(buf, field) buf->st_##field##timespec.tv_nsec
#elif defined(HAVE_ST_MTIMENSEC)
#define NANOSEC(buf, field) buf->st_##field##timensec
#else
#define NANOSEC(buf, field) 0.0
#endif

#endif // #ifndef _LWT_CONFIG_H_
