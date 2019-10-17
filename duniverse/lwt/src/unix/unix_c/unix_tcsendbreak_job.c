/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



/* Informations:

   - this is the expected prototype of the C function [tcsendbreak]:

       int tcsendbreak(int fd, int duration)

   - these are the expected ocaml externals for this job:

       external tcsendbreak_job : Unix.file_descr -> int -> unit Lwt_unix.job = "lwt_unix_tcsendbreak_job"
       external tcsendbreak_sync : Unix.file_descr -> int -> unit = "lwt_unix_tcsendbreak_sync"
*/

/* Caml headers. */
#define CAML_NAME_SPACE
#include <lwt_unix.h>
#include <caml/memory.h>
#include <caml/alloc.h>
#include <caml/fail.h>
#include <caml/signals.h>

#if !defined(LWT_ON_WINDOWS)

/* Specific headers. */
#include <errno.h>
#include <string.h>
#include <termios.h>
#include <unistd.h>

/* +-----------------------------------------------------------------+
   | Asynchronous job                                                |
   +-----------------------------------------------------------------+ */

/* Structure holding informations for calling [tcsendbreak]. */
struct job_tcsendbreak {
  /* Informations used by lwt. It must be the first field of the structure. */
  struct lwt_unix_job job;
  /* This field store the result of the call. */
  int result;
  /* This field store the value of [errno] after the call. */
  int errno_copy;
  /* in parameter. */
  int fd;
  /* in parameter. */
  int duration;
};

/* The function calling [tcsendbreak]. */
static void worker_tcsendbreak(struct job_tcsendbreak* job)
{
  /* Perform the blocking call. */
  job->result = tcsendbreak(job->fd, job->duration);
  /* Save the value of errno. */
  job->errno_copy = errno;
}

/* The function building the caml result. */
static value result_tcsendbreak(struct job_tcsendbreak* job)
{
  /* Check for errors. */
  if (job->result < 0) {
    /* Save the value of errno so we can use it once the job has been freed. */
    int error = job->errno_copy;
    /* Free the job structure. */
    lwt_unix_free_job(&job->job);
    /* Raise the error. */
    unix_error(error, "tcsendbreak", Nothing);
  }
  /* Free the job structure. */
  lwt_unix_free_job(&job->job);
  /* Return the result. */
  return Val_unit;
}

/* The stub creating the job structure. */
CAMLprim value lwt_unix_tcsendbreak_job(value fd, value duration)
{
  /* Allocate a new job. */
  struct job_tcsendbreak* job = lwt_unix_new(struct job_tcsendbreak);
  /* Initializes function fields. */
  job->job.worker = (lwt_unix_job_worker)worker_tcsendbreak;
  job->job.result = (lwt_unix_job_result)result_tcsendbreak;
  /* Copy the fd parameter. */
  job->fd = Int_val(fd);
  /* Copy the duration parameter. */
  job->duration = Int_val(duration);
  /* Wrap the structure into a caml value. */
  return lwt_unix_alloc_job(&job->job);
}

#else /* !defined(LWT_ON_WINDOWS) */

CAMLprim value lwt_unix_tcsendbreak_job(value Unit)
{
  lwt_unix_not_available("tcsendbreak");
  return Val_unit;
}

#endif /* !defined(LWT_ON_WINDOWS) */
