/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if defined(LWT_ON_WINDOWS)

#include <caml/mlvalues.h>
#include <caml/unixsupport.h>

#include "lwt_unix.h"

struct job_system {
    struct lwt_unix_job job;
    HANDLE handle;
};

static void worker_system(struct job_system *job)
{
    WaitForSingleObject(job->handle, INFINITE);
}

static value result_system(struct job_system *job)
{
    HANDLE handle = job->handle;
    DWORD code;
    DWORD err;
    lwt_unix_free_job(&job->job);
    if (!GetExitCodeProcess(handle, &code)) {
        err = GetLastError();
        CloseHandle(handle);
        win32_maperr(err);
        uerror("GetExitCodeProcess", Nothing);
    }
    CloseHandle(handle);
    return Val_int(code);
}

CAMLprim value lwt_unix_system_job(value cmdline)
{
    STARTUPINFO si;
    PROCESS_INFORMATION pi;

    ZeroMemory(&si, sizeof(si));
    ZeroMemory(&pi, sizeof(pi));
    si.cb = sizeof(si);
    if (!CreateProcess(NULL, String_val(cmdline), NULL, NULL, TRUE, 0, NULL,
                       NULL, &si, &pi)) {
        win32_maperr(GetLastError());
        uerror("CreateProcess", Nothing);
    } else {
        LWT_UNIX_INIT_JOB(job, system, 0);
        CloseHandle(pi.hThread);
        job->handle = pi.hProcess;
        return lwt_unix_alloc_job(&(job->job));
    }
}
#endif
