/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



/* Informations:

   - this is the expected prototype of the C function [access]:

       int access(char* path, int mode)

   - these are the expected ocaml externals for this job:

       external access_job : string -> Unix.access_permission list -> unit Lwt_unix.job = "lwt_unix_access_job"
       external access_sync : string -> Unix.access_permission list -> unit = "lwt_unix_access_sync"
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

/* +-----------------------------------------------------------------+
   | Converters                                                      |
   +-----------------------------------------------------------------+ */

/* Table mapping constructors of ocaml type Unix.access_permission to C values. */
static int access_permission_table[] = {
  /* Constructor R_OK. */
  R_OK,
  /* Constructor W_OK. */
  W_OK,
  /* Constructor X_OK. */
  X_OK,
  /* Constructor F_OK. */
  F_OK
};

/* Convert ocaml values of type Unix.access_permission to a C int. */
static int int_of_access_permissions(value list)
{
  int result = 0;
  while (Is_block(list)) {
    result |= access_permission_table[Int_val(Field(list, 0))];
    list = Field(list, 1);
  };
  return result;
}

/* +-----------------------------------------------------------------+
   | Asynchronous job                                                |
   +-----------------------------------------------------------------+ */

/* Structure holding informations for calling [access]. */
struct job_access {
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

/* The function calling [access]. */
static void worker_access(struct job_access* job)
{
  /* Perform the blocking call. */
  job->result = access(job->path, job->mode);
  /* Save the value of errno. */
  job->errno_copy = errno;
}

/* The function building the caml result. */
static value result_access(struct job_access* job)
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
    unix_error(error, "access", string_argument);
  }
  /* Free the job structure. */
  lwt_unix_free_job(&job->job);
  /* Return the result. */
  return Val_unit;
}

/* The stub creating the job structure. */
CAMLprim value lwt_unix_access_job(value path, value mode)
{
  /* Get the length of the path parameter. */
  mlsize_t len_path = caml_string_length(path) + 1;
  /* Allocate a new job. */
  struct job_access* job = lwt_unix_new_plus(struct job_access, len_path);
  /* Set the offset of the path parameter inside the job structure. */
  job->path = job->data;
  /* Copy the path parameter inside the job structure. */
  memcpy(job->path, String_val(path), len_path);
  /* Initializes function fields. */
  job->job.worker = (lwt_unix_job_worker)worker_access;
  job->job.result = (lwt_unix_job_result)result_access;
  /* Copy the mode parameter. */
  job->mode = int_of_access_permissions(mode);
  /* Wrap the structure into a caml value. */
  return lwt_unix_alloc_job(&job->job);
}

#else /* !defined(LWT_ON_WINDOWS) */

CAMLprim value lwt_unix_access_job(value Unit)
{
  lwt_unix_not_available("access");
  return Val_unit;
}

#endif /* !defined(LWT_ON_WINDOWS) */
