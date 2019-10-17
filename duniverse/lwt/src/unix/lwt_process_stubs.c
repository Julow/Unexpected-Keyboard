/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include <lwt_config.h>

#if defined(LWT_ON_WINDOWS)

#include <lwt_unix.h>

#define CAML_NAME_SPACE

#include <caml/alloc.h>
#include <caml/fail.h>
#include <caml/memory.h>

static HANDLE get_handle(value opt) {
  value fd;
  if (Is_block(opt)) {
    fd = Field(opt, 0);
    if (Descr_kind_val(fd) == KIND_SOCKET) {
      win32_maperr(ERROR_INVALID_HANDLE);
      uerror("CreateProcess", Nothing);
      return NULL;
    } else
      return Handle_val(fd);
  } else
    return INVALID_HANDLE_VALUE;
}

#define string_option(opt) (Is_block(opt) ? String_val(Field(opt, 0)) : NULL)

CAMLprim value lwt_process_create_process(value prog, value cmdline, value env,
                                          value cwd, value fds) {
  CAMLparam5(prog, cmdline, env, cwd, fds);
  CAMLlocal1(result);

  STARTUPINFO si;
  PROCESS_INFORMATION pi;

  ZeroMemory(&si, sizeof(si));
  ZeroMemory(&pi, sizeof(pi));
  si.cb = sizeof(si);
  si.dwFlags = STARTF_USESTDHANDLES;
  si.hStdInput = get_handle(Field(fds, 0));
  si.hStdOutput = get_handle(Field(fds, 1));
  si.hStdError = get_handle(Field(fds, 2));

  if (!CreateProcess(string_option(prog), String_val(cmdline), NULL, NULL, TRUE,
                     0, string_option(env), string_option(cwd), &si, &pi)) {
    win32_maperr(GetLastError());
    uerror("CreateProcess", Nothing);
  }

  CloseHandle(pi.hThread);

  result = caml_alloc_tuple(2);
  Store_field(result, 0, Val_int(pi.dwProcessId));
  Store_field(result, 1, win_alloc_handle(pi.hProcess));
  CAMLreturn(result);
}

struct job_wait {
  struct lwt_unix_job job;
  HANDLE handle;
};

static void worker_wait(struct job_wait *job) {
  WaitForSingleObject(job->handle, INFINITE);
}

static value result_wait(struct job_wait *job) {
  DWORD code, error;
  if (!GetExitCodeProcess(job->handle, &code)) {
    error = GetLastError();
    CloseHandle(job->handle);
    lwt_unix_free_job(&job->job);
    win32_maperr(error);
    uerror("GetExitCodeProcess", Nothing);
  }
  CloseHandle(job->handle);
  lwt_unix_free_job(&job->job);
  return Val_int(code);
}

CAMLprim value lwt_process_wait_job(value handle) {
  LWT_UNIX_INIT_JOB(job, wait, 0);
  job->handle = Handle_val(handle);
  return lwt_unix_alloc_job(&(job->job));
}

CAMLprim value lwt_process_terminate_process(value handle, value code) {
  if (!TerminateProcess(Handle_val(handle), Int_val(code))) {
    win32_maperr(GetLastError());
    uerror("TerminateProcess", Nothing);
  }
  return Val_unit;
}

#else /* defined(LWT_ON_WINDOWS) */

/* This is used to suppress a warning from ranlib about the object file having
   no symbols. */
void lwt_process_dummy_symbol() {}

#endif /* defined(LWT_ON_WINDOWS) */
