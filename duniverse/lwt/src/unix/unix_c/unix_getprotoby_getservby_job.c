/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#define ARGS(args...) args

#include <caml/alloc.h>
#include <caml/fail.h>
#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>

#include "lwt_unix.h"

#include "unix_get_network_information_utils.h"

static value alloc_protoent(struct protoent *entry)
{
    value res;
    value name = Val_unit, aliases = Val_unit;

    Begin_roots2(name, aliases);
    name = caml_copy_string(entry->p_name);
    aliases = caml_copy_string_array((const char **)entry->p_aliases);
    res = caml_alloc_small(3, 0);
    Field(res, 0) = name;
    Field(res, 1) = aliases;
    Field(res, 2) = Val_int(entry->p_proto);
    End_roots();
    return res;
}

static value alloc_servent(struct servent *entry)
{
    value res;
    value name = Val_unit, aliases = Val_unit, proto = Val_unit;

    Begin_roots3(name, aliases, proto);
    name = caml_copy_string(entry->s_name);
    aliases = caml_copy_string_array((const char **)entry->s_aliases);
    proto = caml_copy_string(entry->s_proto);
    res = caml_alloc_small(4, 0);
    Field(res, 0) = name;
    Field(res, 1) = aliases;
    Field(res, 2) = Val_int(ntohs(entry->s_port));
    Field(res, 3) = proto;
    End_roots();
    return res;
}

#if defined(HAVE_NETDB_REENTRANT)

#define JOB_GET_ENTRY2(INIT, FUNC, TYPE, ARGS_VAL, ARGS_DECL, ARGS_CALL)     \
    struct job_##FUNC {                                                      \
        struct lwt_unix_job job;                                             \
        struct TYPE entry;                                                   \
        struct TYPE *ptr;                                                    \
        char *buffer;                                                        \
        ARGS_DECL;                                                           \
    };                                                                       \
                                                                             \
    static void worker_##FUNC(struct job_##FUNC *job)                        \
    {                                                                        \
        size_t size = 1024;                                                  \
        for (;;) {                                                           \
            job->buffer = (char *)lwt_unix_malloc(size);                     \
                                                                             \
            int result = FUNC##_r(ARGS_CALL, &job->entry, job->buffer, size, \
                                  &job->ptr);                                \
                                                                             \
            switch (result) {                                                \
                case 0:                                                      \
                    return;                                                  \
                case ERANGE:                                                 \
                    free(job->buffer);                                       \
                    size += 1024;                                            \
                    break;                                                   \
                case ENOENT:                                                 \
                default:                                                     \
                    job->ptr = NULL;                                         \
                    return;                                                  \
            }                                                                \
        }                                                                    \
    }                                                                        \
                                                                             \
    static value result_##FUNC(struct job_##FUNC *job)                       \
    {                                                                        \
        if (job->ptr == NULL) {                                              \
            free(job->buffer);                                               \
            lwt_unix_free_job(&job->job);                                    \
            caml_raise_not_found();                                          \
        } else {                                                             \
            value res = alloc_##TYPE(&job->entry);                           \
            free(job->buffer);                                               \
            lwt_unix_free_job(&job->job);                                    \
            return res;                                                      \
        }                                                                    \
    }                                                                        \
                                                                             \
    CAMLprim value lwt_unix_##FUNC##_job(ARGS_VAL)                           \
    {                                                                        \
        INIT;                                                                \
        return lwt_unix_alloc_job(&(job->job));                              \
    }

#else /* defined(HAVE_NETDB_REENTRANT) */

static struct servent *servent_dup(const struct servent *serv)
{
    struct servent *s;
    if (!serv) {
        return NULL;
    }
    s = malloc(sizeof *s);
    if (s == NULL) {
        goto nomem1;
    }
    s->s_name = s_strdup(serv->s_name);
    if (s->s_name == NULL) {
        goto nomem2;
    }
    s->s_proto = s_strdup(serv->s_proto);
    if (s->s_proto == NULL) {
        goto nomem3;
    }
    s->s_aliases = c_copy_string_array(serv->s_aliases);
    if (s->s_aliases == NULL && serv->s_aliases != NULL) {
        goto nomem4;
    }
    s->s_port = serv->s_port;
    return s;
nomem4:
    free(s->s_proto);
nomem3:
    free(s->s_name);
nomem2:
    free(s);
nomem1:
    return NULL;
}

static void servent_free(struct servent *s)
{
    if (!s) {
        return;
    }
    free(s->s_proto);
    free(s->s_name);
    c_free_string_array(s->s_aliases);
    free(s);
}

static struct protoent *protoent_dup(const struct protoent *proto)
{
    if (!proto) {
        return NULL;
    }
    struct protoent *p = malloc(sizeof *p);
    if (p == NULL) {
        return NULL;
    }
    p->p_name = s_strdup(proto->p_name);
    if (p->p_name == NULL) {
        goto nomem1;
    }
    p->p_aliases = c_copy_string_array(proto->p_aliases);
    if (p->p_aliases == NULL && proto->p_aliases != NULL) {
        goto nomem2;
    }
    p->p_proto = proto->p_proto;
    return p;
nomem2:
    free(p->p_name);
nomem1:
    free(p);
    return NULL;
}

static void protoent_free(struct protoent *p)
{
    if (p) {
        free(p->p_name);
        c_free_string_array(p->p_aliases);
        free(p);
    }
}

#define JOB_GET_ENTRY2(INIT, FUNC, TYPE, ARGS_VAL, ARGS_DECL, ARGS_CALL) \
    struct job_##FUNC {                                                  \
        struct lwt_unix_job job;                                         \
        struct TYPE *entry;                                              \
        ARGS_DECL;                                                       \
    };                                                                   \
                                                                         \
    static void worker_##FUNC(struct job_##FUNC *job)                    \
    {                                                                    \
        job->entry = FUNC(ARGS_CALL);                                    \
        if (job->entry) {                                                \
            job->entry = TYPE##_dup(job->entry);                         \
            if (!job->entry) {                                           \
            }                                                            \
        }                                                                \
    }                                                                    \
                                                                         \
    static value result_##FUNC(struct job_##FUNC *job)                   \
    {                                                                    \
        if (job->entry == NULL) {                                        \
            lwt_unix_free_job(&job->job);                                \
            caml_raise_not_found();                                      \
        } else {                                                         \
            value res = alloc_##TYPE(job->entry);                        \
            TYPE##_free(job->entry);                                     \
            lwt_unix_free_job(&job->job);                                \
            return res;                                                  \
        }                                                                \
    }                                                                    \
                                                                         \
    CAMLprim value lwt_unix_##FUNC##_job(ARGS_VAL)                       \
    {                                                                    \
        INIT;                                                            \
        return lwt_unix_alloc_job(&(job->job));                          \
    }

#endif /* defined(HAVE_NETDB_REENTRANT) */

JOB_GET_ENTRY2(LWT_UNIX_INIT_JOB_STRING(job, getprotobyname, 0, name),
               getprotobyname, protoent, value name, char *name;
               char data[], job->name)
JOB_GET_ENTRY2(LWT_UNIX_INIT_JOB(job, getprotobynumber, 0);
               job->num = Int_val(num), getprotobynumber, protoent, value num,
               int num, job->num)
JOB_GET_ENTRY2(LWT_UNIX_INIT_JOB_STRING2(job, getservbyname, 0, name, proto),
               getservbyname, servent, ARGS(value name, value proto),
               char *name;
               char *proto; char data[], ARGS(job->name, job->proto))
JOB_GET_ENTRY2(LWT_UNIX_INIT_JOB_STRING(job, getservbyport, 0, proto);
               job->port = htons(Int_val(port)), getservbyport, servent,
               ARGS(value port, value proto), int port;
               char *proto; char data[], ARGS(job->port, job->proto))

#endif
