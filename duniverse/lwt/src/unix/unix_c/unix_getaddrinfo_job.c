/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <netdb.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>

#include "lwt_unix.h"
#include "unix_recv_send_utils.h"

struct job_getaddrinfo {
    struct lwt_unix_job job;
    char *node;
    char *service;
    struct addrinfo hints;
    struct addrinfo *info;
    int result;
    char data[];
};

static value cst_to_constr(int n, int *tbl, int size, int deflt)
{
    int i;
    for (i = 0; i < size; i++)
        if (n == tbl[i]) return Val_int(i);
    return Val_int(deflt);
}

static value convert_addrinfo(struct addrinfo *a)
{
    CAMLparam0();
    CAMLlocal3(vres, vaddr, vcanonname);
    union sock_addr_union sa;
    socklen_t len;

    len = a->ai_addrlen;
    if (len > sizeof(sa)) len = sizeof(sa);
    memcpy(&sa.s_gen, a->ai_addr, len);
    vaddr = alloc_sockaddr(&sa, len, -1);
    vcanonname =
        caml_copy_string(a->ai_canonname == NULL ? "" : a->ai_canonname);
    vres = caml_alloc_small(5, 0);
    Field(vres, 0) = cst_to_constr(a->ai_family, socket_domain_table, 3, 0);
    Field(vres, 1) = cst_to_constr(a->ai_socktype, socket_type_table, 4, 0);
    Field(vres, 2) = Val_int(a->ai_protocol);
    Field(vres, 3) = vaddr;
    Field(vres, 4) = vcanonname;
    CAMLreturn(vres);
}

static void worker_getaddrinfo(struct job_getaddrinfo *job)
{
    job->result = getaddrinfo(job->node[0] ? job->node : NULL,
                              job->service[0] ? job->service : NULL,
                              &job->hints, &job->info);
}

static value result_getaddrinfo(struct job_getaddrinfo *job)
{
    CAMLparam0();
    CAMLlocal3(vres, e, v);
    vres = Val_int(0);
    if (job->result == 0) {
        struct addrinfo *r;
        for (r = job->info; r; r = r->ai_next) {
            e = convert_addrinfo(r);
            v = caml_alloc_small(2, 0);
            Field(v, 0) = e;
            Field(v, 1) = vres;
            vres = v;
        }
    }
    if (job->info != NULL) freeaddrinfo(job->info);
    lwt_unix_free_job(&job->job);
    CAMLreturn(vres);
}

CAMLprim value lwt_unix_getaddrinfo_job(value node, value service, value hints)
{
    LWT_UNIX_INIT_JOB_STRING2(job, getaddrinfo, 0, node, service);
    job->info = NULL;
    memset(&job->hints, 0, sizeof(struct addrinfo));
    job->hints.ai_family = PF_UNSPEC;
    for (/*nothing*/; Is_block(hints); hints = Field(hints, 1)) {
        value v = Field(hints, 0);
        if (Is_block(v)) switch (Tag_val(v)) {
                case 0: /* AI_FAMILY of socket_domain */
                    job->hints.ai_family =
                        socket_domain_table[Int_val(Field(v, 0))];
                    break;
                case 1: /* AI_SOCKTYPE of socket_type */
                    job->hints.ai_socktype =
                        socket_type_table[Int_val(Field(v, 0))];
                    break;
                case 2: /* AI_PROTOCOL of int */
                    job->hints.ai_protocol = Int_val(Field(v, 0));
                    break;
            }
        else
            switch (Int_val(v)) {
                case 0: /* AI_NUMERICHOST */
                    job->hints.ai_flags |= AI_NUMERICHOST;
                    break;
                case 1: /* AI_CANONNAME */
                    job->hints.ai_flags |= AI_CANONNAME;
                    break;
                case 2: /* AI_PASSIVE */
                    job->hints.ai_flags |= AI_PASSIVE;
                    break;
            }
    }
    return lwt_unix_alloc_job(&job->job);
}
#endif
