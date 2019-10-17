/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



/* Informations:

   - this is the expected prototype of the C function [tcflush]:

       int tcflush(int fd, int queue)

   - these are the expected ocaml externals for this job:

       external tcflush_job : Unix.file_descr -> Unix.flush_queue -> unit Lwt_unix.job = "lwt_unix_tcflush_job"
       external tcflush_sync : Unix.file_descr -> Unix.flush_queue -> unit = "lwt_unix_tcflush_sync"
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
   | Converters                                                      |
   +-----------------------------------------------------------------+ */

/* Table mapping constructors of ocaml type Unix.flush_queue to C values. */
static int flush_queue_table[] = {
  /* Constructor TCIFLUSH. */
  TCIFLUSH,
  /* Constructor TCOFLUSH. */
  TCOFLUSH,
  /* Constructor TCIOFLUSH. */
  TCIOFLUSH
};

/* +-----------------------------------------------------------------+
   | Asynchronous job                                                |
   +-----------------------------------------------------------------+ */

/* Structure holding informations for calling [tcflush]. */
struct job_tcflush {
  /* Informations used by lwt. It must be the first field of the structure. */
  struct lwt_unix_job job;
  /* This field store the result of the call. */
  int result;
  /* This field store the value of [errno] after the call. */
  int errno_copy;
  /* in parameter. */
  int fd;
  /* in parameter. */
  int queue;
};

/* The function calling [tcflush]. */
static void worker_tcflush(struct job_tcflush* job)
{
  /* Perform the blocking call. */
  job->result = tcflush(job->fd, job->queue);
  /* Save the value of errno. */
  job->errno_copy = errno;
}

/* The function building the caml result. */
static value result_tcflush(struct job_tcflush* job)
{
  /* Check for errors. */
  if (job->result < 0) {
    /* Save the value of errno so we can use it once the job has been freed. */
    int error = job->errno_copy;
    /* Free the job structure. */
    lwt_unix_free_job(&job->job);
    /* Raise the error. */
    unix_error(error, "tcflush", Nothing);
  }
  /* Free the job structure. */
  lwt_unix_free_job(&job->job);
  /* Return the result. */
  return Val_unit;
}

/* The stub creating the job structure. */
CAMLprim value lwt_unix_tcflush_job(value fd, value queue)
{
  /* Allocate a new job. */
  struct job_tcflush* job = lwt_unix_new(struct job_tcflush);
  /* Initializes function fields. */
  job->job.worker = (lwt_unix_job_worker)worker_tcflush;
  job->job.result = (lwt_unix_job_result)result_tcflush;
  /* Copy the fd parameter. */
  job->fd = Int_val(fd);
  /* Copy the queue parameter. */
  job->queue = flush_queue_table[Int_val(queue)];
  /* Wrap the structure into a caml value. */
  return lwt_unix_alloc_job(&job->job);
}

#else /* !defined(LWT_ON_WINDOWS) */

CAMLprim value lwt_unix_tcflush_job(value Unit)
{
  lwt_unix_not_available("tcflush");
  return Val_unit;
}

#endif /* !defined(LWT_ON_WINDOWS) */
