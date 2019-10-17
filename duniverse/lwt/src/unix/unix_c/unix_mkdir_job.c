/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



/* Informations:

   - this is the expected prototype of the C function [mkdir]:

       int mkdir(char* path, int mode)

   - these are the expected ocaml externals for this job:

       external mkdir_job : string -> int -> unit Lwt_unix.job = "lwt_unix_mkdir_job"
       external mkdir_sync : string -> int -> unit = "lwt_unix_mkdir_sync"
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
#include <sys/stat.h>
#include <sys/types.h>

/* +-----------------------------------------------------------------+
   | Asynchronous job                                                |
   +-----------------------------------------------------------------+ */

/* Structure holding informations for calling [mkdir]. */
struct job_mkdir {
  /* Informations used by lwt. It must be the first field of the structure. */
  struct lwt_unix_job job;
  /* This field store the result of the call. */
  int result;
  /* This field store the value of [errno] after the call. */
  int errno_copy;
  /* in parameter. */
  char* path;
  /* in parameter. */
  int mode;
  /* Buffer for string parameters. */
  char data[];
};

/* The function calling [mkdir]. */
static void worker_mkdir(struct job_mkdir* job)
{
  /* Perform the blocking call. */
  job->result = mkdir(job->path, job->mode);
  /* Save the value of errno. */
  job->errno_copy = errno;
}

/* The function building the caml result. */
static value result_mkdir(struct job_mkdir* job)
{
  /* Check for errors. */
  if (job->result < 0) {
    /* Save the value of errno so we can use it once the job has been freed. */
    int error = job->errno_copy;
    /* Copy the contents of job->path into a caml string. */
    value string_argument = caml_copy_string(job->path);
    /* Free the job structure. */
    lwt_unix_free_job(&job->job);
    /* Raise the error. */
    unix_error(error, "mkdir", string_argument);
  }
  /* Free the job structure. */
  lwt_unix_free_job(&job->job);
  /* Return the result. */
  return Val_unit;
}

/* The stub creating the job structure. */
CAMLprim value lwt_unix_mkdir_job(value path, value mode)
{
  /* Get the length of the path parameter. */
  mlsize_t len_path = caml_string_length(path) + 1;
  /* Allocate a new job. */
  struct job_mkdir* job = lwt_unix_new_plus(struct job_mkdir, len_path);
  /* Set the offset of the path parameter inside the job structure. */
  job->path = job->data;
  /* Copy the path parameter inside the job structure. */
  memcpy(job->path, String_val(path), len_path);
  /* Initializes function fields. */
  job->job.worker = (lwt_unix_job_worker)worker_mkdir;
  job->job.result = (lwt_unix_job_result)result_mkdir;
  /* Copy the mode parameter. */
  job->mode = Int_val(mode);
  /* Wrap the structure into a caml value. */
  return lwt_unix_alloc_job(&job->job);
}

#else /* !defined(LWT_ON_WINDOWS) */

CAMLprim value lwt_unix_mkdir_job(value Unit)
{
  lwt_unix_not_available("mkdir");
  return Val_unit;
}

#endif /* !defined(LWT_ON_WINDOWS) */
