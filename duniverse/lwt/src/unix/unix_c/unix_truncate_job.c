/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



/* Informations:

   - this is the expected prototype of the C function [truncate]:

       int truncate(char* path, off_t offset)

   - these are the expected ocaml externals for this job:

       external truncate_job : string -> int -> unit Lwt_unix.job = "lwt_unix_truncate_job"
       external truncate_sync : string -> int -> unit = "lwt_unix_truncate_sync"
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
#include <unistd.h>
#include <sys/types.h>

/* +-----------------------------------------------------------------+
   | Asynchronous job                                                |
   +-----------------------------------------------------------------+ */

/* Structure holding informations for calling [truncate]. */
struct job_truncate {
  /* Informations used by lwt. It must be the first field of the structure. */
  struct lwt_unix_job job;
  /* This field store the result of the call. */
  int result;
  /* This field store the value of [errno] after the call. */
  int errno_copy;
  /* in parameter. */
  char* path;
  /* in parameter. */
  off_t offset;
  /* Buffer for string parameters. */
  char data[];
};

/* The function calling [truncate]. */
static void worker_truncate(struct job_truncate* job)
{
  /* Perform the blocking call. */
  job->result = truncate(job->path, job->offset);
  /* Save the value of errno. */
  job->errno_copy = errno;
}

/* The function building the caml result. */
static value result_truncate(struct job_truncate* job)
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
    unix_error(error, "truncate", string_argument);
  }
  /* Free the job structure. */
  lwt_unix_free_job(&job->job);
  /* Return the result. */
  return Val_unit;
}

/* The stub creating the job structure. */
CAMLprim value lwt_unix_truncate_job(value path, value offset)
{
  /* Get the length of the path parameter. */
  mlsize_t len_path = caml_string_length(path) + 1;
  /* Allocate a new job. */
  struct job_truncate* job = lwt_unix_new_plus(struct job_truncate, len_path);
  /* Set the offset of the path parameter inside the job structure. */
  job->path = job->data;
  /* Copy the path parameter inside the job structure. */
  memcpy(job->path, String_val(path), len_path);
  /* Initializes function fields. */
  job->job.worker = (lwt_unix_job_worker)worker_truncate;
  job->job.result = (lwt_unix_job_result)result_truncate;
  /* Copy the offset parameter. */
  job->offset = Long_val(offset);
  /* Wrap the structure into a caml value. */
  return lwt_unix_alloc_job(&job->job);
}

/* The stub creating the job structure. */
CAMLprim value lwt_unix_truncate_64_job(value path, value offset)
{
  /* Get the length of the path parameter. */
  mlsize_t len_path = caml_string_length(path) + 1;
  /* Allocate a new job. */
  struct job_truncate* job = lwt_unix_new_plus(struct job_truncate, len_path);
  /* Set the offset of the path parameter inside the job structure. */
  job->path = job->data;
  /* Copy the path parameter inside the job structure. */
  memcpy(job->path, String_val(path), len_path);
  /* Initializes function fields. */
  job->job.worker = (lwt_unix_job_worker)worker_truncate;
  job->job.result = (lwt_unix_job_result)result_truncate;
  /* Copy the offset parameter. */
  job->offset = Int64_val(offset);
  /* Wrap the structure into a caml value. */
  return lwt_unix_alloc_job(&job->job);
}

#else /* !defined(LWT_ON_WINDOWS) */

CAMLprim value lwt_unix_truncate_job(value Unit)
{
  lwt_unix_not_available("truncate");
  return Val_unit;
}

CAMLprim value lwt_unix_truncate_64_job(value Unit)
{
  lwt_unix_not_available("truncate");
  return Val_unit;
}

#endif /* !defined(LWT_ON_WINDOWS) */
