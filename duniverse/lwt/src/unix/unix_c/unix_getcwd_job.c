/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/mlvalues.h>
#include <caml/unixsupport.h>
#include <errno.h>
#include <unistd.h>

#include "lwt_unix.h"

/** Lwt C stubs come in two varieties, depending on whether the underlying C
    call is blocking or non-blocking. In all cases, the Lwt wrapper around the C
    call must be made to appear *non*-blocking.

    1. The simple case is when the underlying C call is already non-blocking. An
       example of this is `lwt_unix_read`, which is used by Lwt to perform reads
       from file descriptors that are in non-blocking mode. This stub is a
       simple wrapper around `read(2)`. It converts its arguments from OCaml
       runtime representation to normal C, machine representation, passes them
       to `read(2)`, converts the result back to OCaml, and returns.

    2. In case the underlying C call is blocking, as `getcwd(3)` is, Lwt
       "converts" it to a non-blocking call by running it inside a worker
       thread. The rest of this comment is concerned with such blocking calls.

    For background on writing C stubs in OCaml, see

      http://caml.inria.fr/pub/docs/manual-ocaml/intfc.html


    Each Lwt stub for a blocking C call defines a *job* for the Lwt worker
    thread pool. The actual thread pool is implemented in `lwt_unix_stubs.c` and
    is beyond the scope of this comment. It is not necessary to understand it to
    implement Lwt jobs (indeed, the author currently doesn't remember exactly
    how it works!).

    The thread pool expects jobs in a fixed format (the `struct` below) and
    accompanying functions with fixed names. You *MUST* follow this naming
    conventions. Specifically, for a job "`FOO`", you must define

    - the struct `struct job_FOO`
    - the function `lwt_unix_FOO_job`
    - the function `worker_FOO`
    - the function `result_FOO`

    The struct `struct job_FOO`: This is the representation of the job that will
    be manipulated by the thread pool. It has several purposes:

    - Store the pointers to `worker_FOO` and `result_FOO`, so the thread pool is
      able to run them.
    - Store the C call arguments, or references to them, so they can be accessed
      by `worker_FOO` when it runs in the worker thread.
    - Store the C call results, so they can be accessed by `result_FOO` in the
      main thread.
    - Be something that can be placed in queues, or manipulated otherwise.

    The function `lwt_unix_FOO_job` allocates the job's struct.

    The function `worker_FOO` is later called in a worker thread to actually run
    the job.

    The function `result_FOO` is, even later, called in the main thread to return
    the result of the job to OCaml, and deallocate the job.

    It is also possible to define additional helper functions and/or types, as
    needed.


    Many stubs are defined on Unix-like systems, but not Windows, and vice
    versa. However, Lwt's OCaml code lacks conditional compilation for this, and
    expects all the C symbols to be available on all platforms during linking.
    It just doesn't call the ones that don't have a real implementation.

    The unimplemented symbols are defined using the `LWT_NOT_AVAILABLEx` macros.
    The `getcwd` job currently takes one argument, and is not implemented on
    Windows. For this reason, `lwt_unix_windows.h` has
    `LWT_NOT_AVAILABLE1(unix_getcwd_job)`. The `lwt_` prefix is left off.

    In case this macro is forgotten, Lwt's CI builds should detect that when you
    open a PR against the Lwt repo. Don't worry if this happens – it's a typical
    oversight for all contributors and maintainers.


    See inline comments in the implementation of the `getcwd` job below for
    other details. */

/** The first field of a job `struct` must always be `struct lwt_unix_job job`:

    - The `struct lwt_unix_job` contains the data the thread pool needs to
      manage the job: function pointers, the total size of the job `struct`,
      etc. Placing it at the start of each job `struct` type ensures that the
      offsets to these fields are the same between all kinds of jobs.
    - The `struct lwt_unix_job` must be called `job`, because that is what the
      job helper macros expect.

    The job `struct` should also contain a field `error_code`. This is a
    snapshot of `errno` from the worker thread, right after the C call ran.
    `errno` is a notorious source of pitfalls; see comments in `worker_getcwd`
    and `result_getcwd`.

    The rest of the `struct` is free-form, but typically it contains

    - One field per argument to the C call.
    - One field for the return value of the C call. */
struct job_getcwd {
    struct lwt_unix_job job;
    char buf[4096];
    char *result;
    int error_code;
};
/*
In the OCaml sources, getcwd's buffer size is set as either
- 4096 (in asmrun/spacetime.c, byterun/sys.c)
- PATH_MAX, MAXPATHLEN, or 512 (in otherlibs/unix/getcwd.c)
*/


/* Runs in the worker thread. This function is `static` (not visible outside
   this C file) because it is called only through the function pointer that is
   stored inside `job_getcwd::job` when the job is allocated. `static` is why
   the name is not prefixed with `lwt_unix_`. */
static void worker_getcwd(struct job_getcwd *job)
{
    /* Run the C call. We don't perform any checking in the worker thread,
       because we typically don't want to take any other action here – we want
       to take action in the main thread. In more complex calls, pre-checks on
       the arguments are done in the `lwt_unix_FOO_job` job-allocating
       function, and post-checks on the results are done in the `result_FOO`
       function. */
    job->result = getcwd(job->buf, sizeof(job->buf));

    /* Store the current value of `errno`. Note that if the C call succeeded, it
       did not reset `errno` to zero. In that case, `errno` still contains the
       error code from the last C call to fail in this worker thread. This means
       that `errno`/`job->error_code` *cannot* be used to determine whether the
       C call succeeded or not. */
    job->error_code = errno;
}

/* Runs in the main thread. This function is `static` for the same reason as
   `worker_getcwd`. */
static value result_getcwd(struct job_getcwd *job)
{
    /* This macro is defined in `lwt_unix.h`. The arguments are used as follows:

       - The first argument is the name of the job variable.
       - If the check in the second argument *succeeds*, the C call, and job,
         failed (confusing!). Note that this check must *not* be based solely on
         `job->error_code`; see comment in `worker_getcwd` above.
       - The last argument is the name of the C call, used in a
         `Unix.Unix_error` exception raised if the job failed.

       If the check succeeds/job failed, this macro deallocates the job, raises
       the exception, and does *not* "return" to the rest of `result_getcwd`.
       Otherwise, if the job succeeded, the job is *not* deallocated, and
       execution continues in the rest of `result_getcwd`.

       `job->error_code` is used internally by the macro in creating the
       `Unix.Unix_error`. If this is incorrect (i.e., some job does not set
       `errno` on failure), it is necessary to replace the macro by its
       expansion, and modify the behavior. */
    LWT_UNIX_CHECK_JOB(job, job->result == NULL, "getcwd");

    /* Convert the job result to an OCaml value.

       In this case, create an OCaml string from the temporary buffer into
       which `getcwd(3)` wrote the current directory. This copies the string.

       Throughout Lwt, blocking C calls that run in worker threads can't write
       directly into OCaml strings, because the OCaml garbage collector might
       move the strings after the pointer has already been passed to the call,
       but while the call is still blocked. Bigarrays don't have this problem,
       so pointers into them are passed to blocking C calls, avoiding a copy.

       In addition to worker threads not being able to write into OCaml strings,
       they typically cannot *allocate* any OCaml strings (or other values)
       either, because the worker threads do not try to take OCaml's global
       runtime lock. This sometimes results in extra data copies. For an
       example, see the implementation of `readdir_n`. At the time of this
       writing, that implementation copied each string returned by `readdir`
       twice.

       For jobs that return integers or other kinds of values, it is necessary
       to use the various `Int_val`, `Long_val` macros, etc. See

         http://caml.inria.fr/pub/docs/manual-ocaml/intfc.html#sec415 */
    value result = caml_copy_string(job->result);

    /* Have to free the job manually! */
    lwt_unix_free_job(&job->job);

    return result;
}

/* In the case of `Lwt_unix.getcwd`, the argument is `()`, which is represented
   in C by one argument, which we conventually call `unit`. OCaml always passes
   the same value for this argument, and we don't use it. */
CAMLprim value lwt_unix_getcwd_job(value unit)
{
    /* Allocate the `job_getcwd` on the OCaml heap. Inside it, store its size,
       and pointers to `worker_getcwd` and `result_getcwd`. Arguments must be
       stored manually after the macro is called, but in the case of `getcwd`,
       there are no arguments to initialize.

       For an example of a job that has arguments, see `lwt_unix_read_job`.

       The first argument is the name of the variable to be created to store
       the pointer to the job `struct`, i.e.

         struct job_getcwd *job = ...

       The last argument is the number of bytes of storage to reserve in memory
       immediately following the `struct`. This is for fields such as
       `char data[]` at the end of the struct. It is typically zero. For an
       example where it is not zero, see `lwt_unix_read_job` again.

       If the additional data is stored inline in the job struct, it is
       deallocated with `lwt_unix_free_job`. If the additional data is for
       pointers to additional structure, you must remember to deallocate it
       yourself. For an example of this, see `readdir_n`.*/
    LWT_UNIX_INIT_JOB(job, getcwd, 0);

    /* Allocate a corresponding object in the OCaml heap. `&job->job` is the
       same numeric address as `job`, but has type `struct lwt_unix_job`. */
    return lwt_unix_alloc_job(&job->job);
}
#endif
