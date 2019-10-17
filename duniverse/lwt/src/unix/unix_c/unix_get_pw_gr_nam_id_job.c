/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/fail.h>
#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <grp.h>
#include <pwd.h>
#include <sys/types.h>
#include <unistd.h>

#include "lwt_unix.h"

#if !defined(__ANDROID__)

static value alloc_passwd_entry(struct passwd *entry)
{
    value res;
    value name = Val_unit, passwd = Val_unit, gecos = Val_unit;
    value dir = Val_unit, shell = Val_unit;

    Begin_roots5(name, passwd, gecos, dir, shell);
    name = caml_copy_string(entry->pw_name);
    passwd = caml_copy_string(entry->pw_passwd);
#if !defined(__BEOS__)
    gecos = caml_copy_string(entry->pw_gecos);
#else
    gecos = caml_copy_string("");
#endif
    dir = caml_copy_string(entry->pw_dir);
    shell = caml_copy_string(entry->pw_shell);
    res = caml_alloc_small(7, 0);
    Field(res, 0) = name;
    Field(res, 1) = passwd;
    Field(res, 2) = Val_int(entry->pw_uid);
    Field(res, 3) = Val_int(entry->pw_gid);
    Field(res, 4) = gecos;
    Field(res, 5) = dir;
    Field(res, 6) = shell;
    End_roots();
    return res;
}

static value alloc_group_entry(struct group *entry)
{
    value res;
    value name = Val_unit, pass = Val_unit, mem = Val_unit;

    Begin_roots3(name, pass, mem);
    name = caml_copy_string(entry->gr_name);
    pass = caml_copy_string(entry->gr_passwd);
    mem = caml_copy_string_array((const char **)entry->gr_mem);
    res = caml_alloc_small(4, 0);
    Field(res, 0) = name;
    Field(res, 1) = pass;
    Field(res, 2) = Val_int(entry->gr_gid);
    Field(res, 3) = mem;
    End_roots();
    return res;
}

#define JOB_GET_ENTRY(INIT, FUNC, CONF, TYPE, ARG, ARG_DECL, FAIL_ARG) \
    struct job_##FUNC {                                                \
        struct lwt_unix_job job;                                       \
        struct TYPE entry;                                             \
        struct TYPE *ptr;                                              \
        char *buffer;                                                  \
        int result;                                                    \
        ARG_DECL;                                                      \
    };                                                                 \
                                                                       \
    static void worker_##FUNC(struct job_##FUNC *job)                  \
    {                                                                  \
        size_t buffer_size = sysconf(_SC_##CONF##_R_SIZE_MAX);         \
        if (buffer_size == (size_t)-1) buffer_size = 16384;            \
        job->buffer = (char *)lwt_unix_malloc(buffer_size);            \
        job->result = FUNC##_r(job->ARG, &job->entry, job->buffer,     \
                               buffer_size, &job->ptr);                \
    }                                                                  \
                                                                       \
    static value result_##FUNC(struct job_##FUNC *job)                 \
    {                                                                  \
        int result = job->result;                                      \
        if (result) {                                                  \
            value arg = FAIL_ARG;                                      \
            free(job->buffer);                                         \
            lwt_unix_free_job(&job->job);                              \
            unix_error(result, #FUNC, arg);                            \
        } else if (job->ptr == NULL) {                                 \
            free(job->buffer);                                         \
            lwt_unix_free_job(&job->job);                              \
            caml_raise_not_found();                                    \
        } else {                                                       \
            value entry = alloc_##TYPE##_entry(&job->entry);           \
            free(job->buffer);                                         \
            lwt_unix_free_job(&job->job);                              \
            return entry;                                              \
        }                                                              \
    }                                                                  \
                                                                       \
    CAMLprim value lwt_unix_##FUNC##_job(value ARG)                    \
    {                                                                  \
        INIT;                                                          \
        return lwt_unix_alloc_job(&job->job);                          \
    }

JOB_GET_ENTRY(LWT_UNIX_INIT_JOB_STRING(job, getpwnam, 0, name), getpwnam, GETPW,
              passwd, name, char *name;
              char data[], caml_copy_string(job->name))
JOB_GET_ENTRY(LWT_UNIX_INIT_JOB_STRING(job, getgrnam, 0, name), getgrnam, GETGR,
              group, name, char *name;
              char data[], caml_copy_string(job->name))
JOB_GET_ENTRY(LWT_UNIX_INIT_JOB(job, getpwuid, 0); job->uid = Int_val(uid),
                                                   getpwuid, GETPW, passwd, uid,
                                                   int uid, Nothing)
JOB_GET_ENTRY(LWT_UNIX_INIT_JOB(job, getgrgid, 0); job->gid = Int_val(gid),
                                                   getgrgid, GETGR, group, gid,
                                                   int gid, Nothing)

#else

LWT_NOT_AVAILABLE1(unix_getpwnam_job)
LWT_NOT_AVAILABLE1(unix_getgrnam_job)
LWT_NOT_AVAILABLE1(unix_getpwuid_job)
LWT_NOT_AVAILABLE1(unix_getgrgid_job)

#endif
#endif
