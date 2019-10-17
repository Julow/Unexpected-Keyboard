/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



/* Informations:

   - this is the expected prototype of the C function [tcflow]:

       int tcflow(int fd, int action)

   - these are the expected ocaml externals for this job:

       external tcflow_job : Unix.file_descr -> Unix.flow_action -> unit Lwt_unix.job = "lwt_unix_tcflow_job"
       external tcflow_sync : Unix.file_descr -> Unix.flow_action -> unit = "lwt_unix_tcflow_sync"
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

/* Table mapping constructors of ocaml type Unix.flow_action to C values. */
static int flow_action_table[] = {
  /* Constructor TCOOFF. */
  TCOOFF,
  /* Constructor TCOON. */
  TCOON,
  /* Constructor TCIOFF. */
  TCIOFF,
  /* Constructor TCION. */
  TCION
};

/* +-----------------------------------------------------------------+
   | Asynchronous job                                                |
   +-----------------------------------------------------------------+ */

/* Structure holding informations for calling [tcflow]. */
struct job_tcflow {
  /* Informations used by lwt. It must be the first field of the structure. */
  struct lwt_unix_job job;
  /* This field store the result of the call. */
  int result;
  /* This field store the value of [errno] after the call. */
  int errno_copy;
  /* in parameter. */
  int fd;
  /* in parameter. */
  int action;
};

/* The function calling [tcflow]. */
static void worker_tcflow(struct job_tcflow* job)
{
  /* Perform the blocking call. */
  job->result = tcflow(job->fd, job->action);
  /* Save the value of errno. */
  job->errno_copy = errno;
}

/* The function building the caml result. */
static value result_tcflow(struct job_tcflow* job)
{
  /* Check for errors. */
  if (job->result < 0) {
    /* Save the value of errno so we can use it once the job has been freed. */
    int error = job->errno_copy;
    /* Free the job structure. */
    lwt_unix_free_job(&job->job);
    /* Raise the error. */
    unix_error(error, "tcflow", Nothing);
  }
  /* Free the job structure. */
  lwt_unix_free_job(&job->job);
  /* Return the result. */
  return Val_unit;
}

/* The stub creating the job structure. */
CAMLprim value lwt_unix_tcflow_job(value fd, value action)
{
  /* Allocate a new job. */
  struct job_tcflow* job = lwt_unix_new(struct job_tcflow);
  /* Initializes function fields. */
  job->job.worker = (lwt_unix_job_worker)worker_tcflow;
  job->job.result = (lwt_unix_job_result)result_tcflow;
  /* Copy the fd parameter. */
  job->fd = Int_val(fd);
  /* Copy the action parameter. */
  job->action = flow_action_table[Int_val(action)];
  /* Wrap the structure into a caml value. */
  return lwt_unix_alloc_job(&job->job);
}

#else /* !defined(LWT_ON_WINDOWS) */

CAMLprim value lwt_unix_tcflow_job(value Unit)
{
  lwt_unix_not_available("tcflow");
  return Val_unit;
}

#endif /* !defined(LWT_ON_WINDOWS) */
