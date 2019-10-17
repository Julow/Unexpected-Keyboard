/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



/* Informations:

   - this is the expected prototype of the C function [lseek]:

       off_t lseek(int fd, off_t offset, int whence)

   - these are the expected ocaml externals for this job:

       external lseek_job : Unix.file_descr -> int -> Unix.seek_command -> int Lwt_unix.job = "lwt_unix_lseek_job"
       external lseek_sync : Unix.file_descr -> int -> Unix.seek_command -> int = "lwt_unix_lseek_sync"
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
#include <sys/types.h>
#include <unistd.h>

/* +-----------------------------------------------------------------+
   | Converters                                                      |
   +-----------------------------------------------------------------+ */

/* Table mapping constructors of ocaml type Unix.seek_command to C values. */
static int seek_command_table[] = {
  /* Constructor SEEK_SET. */
  SEEK_SET,
  /* Constructor SEEK_CUR. */
  SEEK_CUR,
  /* Constructor SEEK_END. */
  SEEK_END
};

/* +-----------------------------------------------------------------+
   | Asynchronous job                                                |
   +-----------------------------------------------------------------+ */

/* Structure holding informations for calling [lseek]. */
struct job_lseek {
  /* Informations used by lwt. It must be the first field of the structure. */
  struct lwt_unix_job job;
  /* This field store the result of the call. */
  off_t result;
  /* This field store the value of [errno] after the call. */
  int errno_copy;
  /* in parameter. */
  int fd;
  /* in parameter. */
  off_t offset;
  /* in parameter. */
  int whence;
};

/* The function calling [lseek]. */
static void worker_lseek(struct job_lseek* job)
{
  /* Perform the blocking call. */
  job->result = lseek(job->fd, job->offset, job->whence);
  /* Save the value of errno. */
  job->errno_copy = errno;
}

/* The function building the caml result. */
static value result_lseek(struct job_lseek* job)
{
  value result;
  /* Check for errors. */
  if (job->result == (off_t)-1) {
    /* Save the value of errno so we can use it once the job has been freed. */
    int error = job->errno_copy;
    /* Free the job structure. */
    lwt_unix_free_job(&job->job);
    /* Raise the error. */
    unix_error(error, "lseek", Nothing);
  }
  /* Build the caml result. */
  result = Val_long(job->result);
  /* Free the job structure. */
  lwt_unix_free_job(&job->job);
  /* Return the result. */
  return result;
}

/* The function building the caml result. */
static value result_lseek_64(struct job_lseek* job)
{
  value result;
  /* Check for errors. */
  if (job->result == (off_t)-1) {
    /* Save the value of errno so we can use it once the job has been freed. */
    int error = job->errno_copy;
    /* Free the job structure. */
    lwt_unix_free_job(&job->job);
    /* Raise the error. */
    unix_error(error, "lseek", Nothing);
  }
  /* Build the caml result. */
  result = caml_copy_int64(job->result);
  /* Free the job structure. */
  lwt_unix_free_job(&job->job);
  /* Return the result. */
  return result;
}

/* The stub creating the job structure. */
CAMLprim value lwt_unix_lseek_job(value fd, value offset, value whence)
{
  /* Allocate a new job. */
  struct job_lseek* job = lwt_unix_new(struct job_lseek);
  /* Initializes function fields. */
  job->job.worker = (lwt_unix_job_worker)worker_lseek;
  job->job.result = (lwt_unix_job_result)result_lseek;
  /* Copy the fd parameter. */
  job->fd = Int_val(fd);
  /* Copy the offset parameter. */
  job->offset = Long_val(offset);
  /* Copy the whence parameter. */
  job->whence = seek_command_table[Int_val(whence)];
  /* Wrap the structure into a caml value. */
  return lwt_unix_alloc_job(&job->job);
}

/* The stub creating the job structure. */
CAMLprim value lwt_unix_lseek_64_job(value fd, value offset, value whence)
{
  /* Allocate a new job. */
  struct job_lseek* job = lwt_unix_new(struct job_lseek);
  /* Initializes function fields. */
  job->job.worker = (lwt_unix_job_worker)worker_lseek;
  job->job.result = (lwt_unix_job_result)result_lseek_64;
  /* Copy the fd parameter. */
  job->fd = Int_val(fd);
  /* Copy the offset parameter. */
  job->offset = Int64_val(offset);
  /* Copy the whence parameter. */
  job->whence = seek_command_table[Int_val(whence)];
  /* Wrap the structure into a caml value. */
  return lwt_unix_alloc_job(&job->job);
}

#else /* !defined(LWT_ON_WINDOWS) */

CAMLprim value lwt_unix_lseek_job(value Unit)
{
  lwt_unix_not_available("lseek");
  return Val_unit;
}

CAMLprim value lwt_unix_lseek_64_job(value Unit)
{
  lwt_unix_not_available("lseek");
  return Val_unit;
}

#endif /* !defined(LWT_ON_WINDOWS) */
