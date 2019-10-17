/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#ifndef __LWT_UNIX_H
#define __LWT_UNIX_H

#define CAML_NAME_SPACE

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <lwt_config.h>

#include <caml/socketaddr.h>
#include <string.h>

/* The macro to get the file-descriptor from a value. */
#if defined(LWT_ON_WINDOWS)
#define FD_val(value) win_CRT_fd_of_filedescr(value)
#else
#define FD_val(value) Int_val(value)
#endif

/* Macro to extract a libev loop from a caml value. */
#define Ev_loop_val(value) *(struct ev_loop **)Data_custom_val(value)

/* +-----------------------------------------------------------------+
   | Utils                                                           |
   +-----------------------------------------------------------------+ */

/* Allocate the given amount of memory and abort the program if there
   is no free memory left. */
void *lwt_unix_malloc(size_t size);
void *lwt_unix_realloc(void *ptr, size_t size);

/* Same as [strdup] and abort the program if there is not memory
   left. */
char *lwt_unix_strdup(char *string);

/* Helpers for allocating structures. */
#define lwt_unix_new(type) (type *)lwt_unix_malloc(sizeof(type))
#define lwt_unix_new_plus(type, size) \
    (type *)lwt_unix_malloc(sizeof(type) + size)

/* Raise [Lwt_unix.Not_available]. */
void lwt_unix_not_available(char const *feature) Noreturn;

#define LWT_NOT_AVAILABLE1(prim) \
    CAMLprim value lwt_##prim(value a1) { lwt_unix_not_available(#prim); }
#define LWT_NOT_AVAILABLE2(prim)                  \
    CAMLprim value lwt_##prim(value a1, value a2) \
    {                                             \
        lwt_unix_not_available(#prim);            \
    }
#define LWT_NOT_AVAILABLE3(prim)                            \
    CAMLprim value lwt_##prim(value a1, value a2, value a3) \
    {                                                       \
        lwt_unix_not_available(#prim);                      \
    }
#define LWT_NOT_AVAILABLE4(prim)                                      \
    CAMLprim value lwt_##prim(value a1, value a2, value a3, value a4) \
    {                                                                 \
        lwt_unix_not_available(#prim);                                \
    }
#define LWT_NOT_AVAILABLE5(prim)                                      \
    CAMLprim value lwt_##prim(value a1, value a2, value a3, value a4, \
                              value a5)                               \
    {                                                                 \
        lwt_unix_not_available(#prim);                                \
    }
#define LWT_NOT_AVAILABLE6(prim)                                      \
    CAMLprim value lwt_##prim(value a1, value a2, value a3, value a4, \
                              value a5, value a6)                     \
    {                                                                 \
        lwt_unix_not_available(#prim);                                \
    }

/* +-----------------------------------------------------------------+
   | Notifications                                                   |
   +-----------------------------------------------------------------+ */

/* Sends a notification for the given id. */
void lwt_unix_send_notification(intnat id);

/* +-----------------------------------------------------------------+
   | Threading                                                       |
   +-----------------------------------------------------------------+ */

#if defined(HAVE_PTHREAD)

#include <pthread.h>

typedef pthread_t lwt_unix_thread;
typedef pthread_mutex_t lwt_unix_mutex;
typedef pthread_cond_t lwt_unix_condition;

#else

typedef DWORD lwt_unix_thread;
typedef CRITICAL_SECTION lwt_unix_mutex;
typedef struct lwt_unix_condition lwt_unix_condition;

#endif

/* Launch a thread in detached mode. */
int lwt_unix_launch_thread(void *(*start)(void *), void *data);

/* Return a handle to the currently running thread. */
lwt_unix_thread lwt_unix_thread_self();

/* Returns whether two thread handles refer to the same thread. */
int lwt_unix_thread_equal(lwt_unix_thread thread1, lwt_unix_thread thread2);

/* Initialises a mutex. */
void lwt_unix_mutex_init(lwt_unix_mutex *mutex);

/* Destroy a mutex. */
void lwt_unix_mutex_destroy(lwt_unix_mutex *mutex);

/* Lock a mutex. */
void lwt_unix_mutex_lock(lwt_unix_mutex *mutex);

/* Unlock a mutex. */
void lwt_unix_mutex_unlock(lwt_unix_mutex *mutex);

/* Initialises a condition variable. */
void lwt_unix_condition_init(lwt_unix_condition *condition);

/* Destroy a condition variable. */
void lwt_unix_condition_destroy(lwt_unix_condition *condition);

/* Signal a condition variable. */
void lwt_unix_condition_signal(lwt_unix_condition *condition);

/* Broadcast a signal on a condition variable. */
void lwt_unix_condition_broadcast(lwt_unix_condition *condition);

/* Wait for a signal on a condition variable. */
void lwt_unix_condition_wait(lwt_unix_condition *condition,
                             lwt_unix_mutex *mutex);

/* +-----------------------------------------------------------------+
   | Detached jobs                                                   |
   +-----------------------------------------------------------------+ */

/* How job are executed. */
enum lwt_unix_async_method {
    /* Synchronously. */
    LWT_UNIX_ASYNC_METHOD_NONE = 0,

    /* Asynchronously, on another thread. */
    LWT_UNIX_ASYNC_METHOD_DETACH = 1,

    /* Asynchronously, on the main thread, switcing to another thread if
       necessary. */
    LWT_UNIX_ASYNC_METHOD_SWITCH = 2
};

/* Type of job execution modes. */
typedef enum lwt_unix_async_method lwt_unix_async_method;

/* State of a job. */
enum lwt_unix_job_state {
    /* The job has not yet started. */
    LWT_UNIX_JOB_STATE_PENDING,

    /* The job is running. */
    LWT_UNIX_JOB_STATE_RUNNING,

    /* The job is done. */
    LWT_UNIX_JOB_STATE_DONE
};

/* A job descriptor. */
struct lwt_unix_job {
    /* The next job in the queue. */
    struct lwt_unix_job *next;

    /* Id used to notify the main thread in case the job do not
       terminate immediately. */
    intnat notification_id;

    /* The function to call to do the work.

       This function must not:
       - access or allocate OCaml block values (tuples, strings, ...),
       - call OCaml code. */
    void (*worker)(struct lwt_unix_job *job);

    /* The function to call to extract the result and free memory
       allocated by the job.

       Note: if you want to raise an excpetion, be sure to free
       resources before raising it!

       It has been introduced in Lwt 2.3.3. */
    value (*result)(struct lwt_unix_job *job);

    /* State of the job. */
    enum lwt_unix_job_state state;

    /* Is the main thread still waiting for the job ? */
    int fast;

    /* Mutex to protect access to [state] and [fast]. */
    lwt_unix_mutex mutex;

    /* Thread running the job. */
    lwt_unix_thread thread;

    /* The async method in used by the job. */
    lwt_unix_async_method async_method;
};

/* Type of job descriptors. */
typedef struct lwt_unix_job *lwt_unix_job;

/* Type of worker functions. */
typedef void (*lwt_unix_job_worker)(lwt_unix_job job);

/* Type of result functions. */
typedef value (*lwt_unix_job_result)(lwt_unix_job job);

/* Allocate a caml custom value for the given job. */
value lwt_unix_alloc_job(lwt_unix_job job);

/* Free resourecs allocated for this job and free it. */
void lwt_unix_free_job(lwt_unix_job job);

/* +-----------------------------------------------------------------+
   | Helpers for writing jobs                                        |
   +-----------------------------------------------------------------+ */

/* Allocate a job structure and set its worker and result fields.

   - VAR is the name of the job variable. It is usually "job".
   - FUNC is the suffix of the structure name and functions of this job.
     It is usually the name of the function that is wrapped.
   - SIZE is the dynamic size to allocate at the end of the structure,
     in case it ends ends with something of the form: char data[]);
*/
#define LWT_UNIX_INIT_JOB(VAR, FUNC, SIZE)                               \
    struct job_##FUNC *VAR = lwt_unix_new_plus(struct job_##FUNC, SIZE); \
    VAR->job.worker = (lwt_unix_job_worker)worker_##FUNC;                \
    VAR->job.result = (lwt_unix_job_result)result_##FUNC

/* Same as LWT_UNIX_INIT_JOB, but also stores a string argument named
   ARG at the end of the job structure. The offset of the copied
   string is assigned to the field VAR->ARG.

   The structure must ends with: char data[]; */
#define LWT_UNIX_INIT_JOB_STRING(VAR, FUNC, SIZE, ARG) \
    mlsize_t __len = caml_string_length(ARG);          \
    LWT_UNIX_INIT_JOB(VAR, FUNC, SIZE + __len + 1);    \
    VAR->ARG = VAR->data + SIZE;                       \
    memcpy(VAR->ARG, String_val(ARG), __len + 1)

/* Same as LWT_UNIX_INIT_JOB, but also stores two string arguments
   named ARG1 and ARG2 at the end of the job structure. The offsets of
   the copied strings are assigned to the fields VAR->ARG1 and
   VAR->ARG2.

   The structure definition must ends with: char data[]; */
#define LWT_UNIX_INIT_JOB_STRING2(VAR, FUNC, SIZE, ARG1, ARG2) \
    mlsize_t __len1 = caml_string_length(ARG1);                \
    mlsize_t __len2 = caml_string_length(ARG2);                \
    LWT_UNIX_INIT_JOB(VAR, FUNC, SIZE + __len1 + __len2 + 2);  \
    VAR->ARG1 = VAR->data + SIZE;                              \
    VAR->ARG2 = VAR->data + SIZE + __len1 + 1;                 \
    memcpy(VAR->ARG1, String_val(ARG1), __len1 + 1);           \
    memcpy(VAR->ARG2, String_val(ARG2), __len2 + 1)

/* If TEST is true, it frees the job and raises Unix.Unix_error using
   the value of errno stored in the field error_code. */
#define LWT_UNIX_CHECK_JOB(VAR, TEST, NAME)    \
    if (TEST) {                                \
        int error_code = VAR->error_code;      \
        lwt_unix_free_job(&VAR->job);          \
        unix_error(error_code, NAME, Nothing); \
    }

/* If TEST is true, it frees the job and raises Unix.Unix_error using
   the value of errno stored in the field error_code and uses the C
   string ARG for the third field of Unix.Unix_error. */
#define LWT_UNIX_CHECK_JOB_ARG(VAR, TEST, NAME, ARG) \
    if (TEST) {                                      \
        int error_code = VAR->error_code;            \
        value arg = caml_copy_string(ARG);           \
        lwt_unix_free_job(&VAR->job);                \
        unix_error(error_code, NAME, arg);           \
    }

/* +-----------------------------------------------------------------+
   | Deprecated                                                      |
   +-----------------------------------------------------------------+ */

/* Define not implement methods. Deprecated: it is for the old
   mechanism with three externals. */
#define LWT_UNIX_JOB_NOT_IMPLEMENTED(name)              \
    CAMLprim value lwt_unix_##name##_job(value Unit)    \
    {                                                   \
        caml_invalid_argument("not implemented");       \
    }                                                   \
                                                        \
    CAMLprim value lwt_unix_##name##_result(value Unit) \
    {                                                   \
        caml_invalid_argument("not implemented");       \
    }                                                   \
                                                        \
    CAMLprim value lwt_unix_##name##_free(value Unit)   \
    {                                                   \
        caml_invalid_argument("not implemented");       \
    }

#endif /* __LWT_UNIX_H */
