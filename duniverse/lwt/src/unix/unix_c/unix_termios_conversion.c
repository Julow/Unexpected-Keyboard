/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <errno.h>
#include "unix_termios_conversion.h"

/* TODO: make it reentrant. */

enum { Bool, Enum, Speed, Char, End };

enum { Input, Output };

enum { Iflags, Oflags, Cflags, Lflags };

/* Structure of the terminal_io record. Cf. unix.mli */

static long terminal_io_descr[] = {
    /* Input modes */
    Bool, Iflags, IGNBRK, Bool, Iflags, BRKINT, Bool, Iflags, IGNPAR, Bool,
    Iflags, PARMRK, Bool, Iflags, INPCK, Bool, Iflags, ISTRIP, Bool, Iflags,
    INLCR, Bool, Iflags, IGNCR, Bool, Iflags, ICRNL, Bool, Iflags, IXON, Bool,
    Iflags, IXOFF,
    /* Output modes */
    Bool, Oflags, OPOST,
    /* Control modes */
    Speed, Output, Speed, Input, Enum, Cflags, 5, 4, CSIZE, CS5, CS6, CS7, CS8,
    Enum, Cflags, 1, 2, CSTOPB, 0, CSTOPB, Bool, Cflags, CREAD, Bool, Cflags,
    PARENB, Bool, Cflags, PARODD, Bool, Cflags, HUPCL, Bool, Cflags, CLOCAL,
    /* Local modes */
    Bool, Lflags, ISIG, Bool, Lflags, ICANON, Bool, Lflags, NOFLSH, Bool,
    Lflags, ECHO, Bool, Lflags, ECHOE, Bool, Lflags, ECHOK, Bool, Lflags,
    ECHONL,
    /* Control characters */
    Char, VINTR, Char, VQUIT, Char, VERASE, Char, VKILL, Char, VEOF, Char, VEOL,
    Char, VMIN, Char, VTIME, Char, VSTART, Char, VSTOP, End};

static struct {
    speed_t speed;
    int baud;
} speedtable[] = {{B50, 50},
                  {B75, 75},
                  {B110, 110},
                  {B134, 134},
                  {B150, 150},
#ifdef B200
                  {B200, 200},
#endif
                  {B300, 300},
                  {B600, 600},
                  {B1200, 1200},
                  {B1800, 1800},
                  {B2400, 2400},
                  {B4800, 4800},
                  {B9600, 9600},
                  {B19200, 19200},
                  {B38400, 38400},
#ifdef B57600
                  {B57600, 57600},
#endif
#ifdef B115200
                  {B115200, 115200},
#endif
#ifdef B230400
                  {B230400, 230400},
#endif
                  {B0, 0},

                  /* Linux extensions */
#ifdef B460800
                  {B460800, 460800},
#endif
#ifdef B500000
                  {B500000, 500000},
#endif
#ifdef B576000
                  {B576000, 576000},
#endif
#ifdef B921600
                  {B921600, 921600},
#endif
#ifdef B1000000
                  {B1000000, 1000000},
#endif
#ifdef B1152000
                  {B1152000, 1152000},
#endif
#ifdef B1500000
                  {B1500000, 1500000},
#endif
#ifdef B2000000
                  {B2000000, 2000000},
#endif
#ifdef B2500000
                  {B2500000, 2500000},
#endif
#ifdef B3000000
                  {B3000000, 3000000},
#endif
#ifdef B3500000
                  {B3500000, 3500000},
#endif
#ifdef B4000000
                  {B4000000, 4000000},
#endif

                  /* MacOS extensions */
#ifdef B7200
                  {B7200, 7200},
#endif
#ifdef B14400
                  {B14400, 14400},
#endif
#ifdef B28800
                  {B28800, 28800},
#endif
#ifdef B76800
                  {B76800, 76800},
#endif

  /* Cygwin extensions (in addition to the Linux ones) */
#ifdef B128000
                  {B128000, 128000},
#endif
#ifdef B256000
                  {B256000, 256000},
#endif
};

#define NSPEEDS (sizeof(speedtable) / sizeof(speedtable[0]))

static tcflag_t *choose_field(struct termios *terminal_status, long field)
{
    switch (field) {
        case Iflags:
            return &terminal_status->c_iflag;
        case Oflags:
            return &terminal_status->c_oflag;
        case Cflags:
            return &terminal_status->c_cflag;
        case Lflags:
            return &terminal_status->c_lflag;
        default:
            return 0;
    }
}

void encode_terminal_status(struct termios *terminal_status, value *dst)
{
    long *pc;
    int i;

    for (pc = terminal_io_descr; *pc != End; dst++) {
        switch (*pc++) {
            case Bool: {
                tcflag_t *src = choose_field(terminal_status, *pc++);
                tcflag_t msk = *pc++;
                *dst = Val_bool(*src & msk);
                break;
            }
            case Enum: {
                tcflag_t *src = choose_field(terminal_status, *pc++);
                int ofs = *pc++;
                int num = *pc++;
                tcflag_t msk = *pc++;
                for (i = 0; i < num; i++) {
                    if ((*src & msk) == pc[i]) {
                        *dst = Val_int(i + ofs);
                        break;
                    }
                }
                pc += num;
                break;
            }
            case Speed: {
                int which = *pc++;
                speed_t speed = 0;
                *dst =
                    Val_int(9600); /* in case no speed in speedtable matches */
                switch (which) {
                    case Output:
                        speed = cfgetospeed(terminal_status);
                        break;
                    case Input:
                        speed = cfgetispeed(terminal_status);
                        break;
                }
                for (i = 0; i < NSPEEDS; i++) {
                    if (speed == speedtable[i].speed) {
                        *dst = Val_int(speedtable[i].baud);
                        break;
                    }
                }
                break;
            }
            case Char: {
                int which = *pc++;
                *dst = Val_int(terminal_status->c_cc[which]);
                break;
            }
        }
    }
}

void decode_terminal_status(struct termios *terminal_status, value *src)
{
    long *pc;
    int i;

    for (pc = terminal_io_descr; *pc != End; src++) {
        switch (*pc++) {
            case Bool: {
                tcflag_t *dst = choose_field(terminal_status, *pc++);
                tcflag_t msk = *pc++;
                if (Bool_val(*src))
                    *dst |= msk;
                else
                    *dst &= ~msk;
                break;
            }
            case Enum: {
                tcflag_t *dst = choose_field(terminal_status, *pc++);
                int ofs = *pc++;
                int num = *pc++;
                tcflag_t msk = *pc++;
                i = Int_val(*src) - ofs;
                if (i >= 0 && i < num) {
                    *dst = (*dst & ~msk) | pc[i];
                } else {
                    unix_error(EINVAL, "tcsetattr", Nothing);
                }
                pc += num;
                break;
            }
            case Speed: {
                int which = *pc++;
                int baud = Int_val(*src);
                int res = 0;
                for (i = 0; i < NSPEEDS; i++) {
                    if (baud == speedtable[i].baud) {
                        switch (which) {
                            case Output:
                                res = cfsetospeed(terminal_status,
                                                  speedtable[i].speed);
                                break;
                            case Input:
                                res = cfsetispeed(terminal_status,
                                                  speedtable[i].speed);
                                break;
                        }
                        if (res == -1) uerror("tcsetattr", Nothing);
                        goto ok;
                    }
                }
                unix_error(EINVAL, "tcsetattr", Nothing);
            ok:
                break;
            }
            case Char: {
                int which = *pc++;
                terminal_status->c_cc[which] = Int_val(*src);
                break;
            }
        }
    }
}
#endif
