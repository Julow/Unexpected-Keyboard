/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#define _GNU_SOURCE
#define _POSIX_PTHREAD_SEMANTICS

#define CAML_NAME_SPACE

#include <caml/alloc.h>
#include <caml/bigarray.h>
#include <caml/callback.h>
#include <caml/config.h>
#include <caml/custom.h>
#include <caml/fail.h>
#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/signals.h>

#include <assert.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>

#if !defined(LWT_ON_WINDOWS) && defined(SIGRTMIN) && defined(SIGRTMAX)
#define LWT_UNIX_SIGNAL_ASYNC_SWITCH SIGRTMIN
#define LWT_UNIX_HAVE_ASYNC_SWITCH
#include <setjmp.h>
#else
#endif

#include "lwt_unix.h"

#if !defined(LWT_ON_WINDOWS)
#include <arpa/inet.h>
#include <dirent.h>
#include <fcntl.h>
#include <grp.h>
#include <netdb.h>
#include <netinet/in.h>
#include <pwd.h>
#include <sched.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/param.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/un.h>
#include <termios.h>
#include <unistd.h>
#endif

#if defined(HAVE_EVENTFD)
#include <sys/eventfd.h>
#endif

//#define DEBUG_MODE

#if defined(DEBUG_MODE)
#include <sys/syscall.h>
#define DEBUG(fmt, ...)                                               \
  {                                                                   \
    fprintf(stderr, "lwt-debug[%d]: %s: " fmt "\n",                   \
            (pid_t)syscall(SYS_gettid), __FUNCTION__, ##__VA_ARGS__); \
    fflush(stderr);                                                   \
  }
#else
#define DEBUG(fmt, ...)
#endif

/* +-----------------------------------------------------------------+
   | Utils                                                           |
   +-----------------------------------------------------------------+ */

void *lwt_unix_malloc(size_t size) {
  void *ptr = malloc(size);
  if (ptr == NULL) {
    perror("cannot allocate memory");
    abort();
  }
  return ptr;
}

void *lwt_unix_realloc(void *ptr, size_t size) {
  void *new_ptr = realloc(ptr, size);
  if (new_ptr == NULL) {
    perror("cannot allocate memory");
    abort();
  }
  return new_ptr;
}

char *lwt_unix_strdup(char *str) {
  char *new_str = strdup(str);
  if (new_str == NULL) {
    perror("cannot allocate memory");
    abort();
  }
  return new_str;
}

void lwt_unix_not_available(char const *feature) {
  caml_raise_with_arg(*caml_named_value("lwt:not-available"),
                      caml_copy_string(feature));
}

/* +-----------------------------------------------------------------+
   | Operation on bigarrays                                          |
   +-----------------------------------------------------------------+ */

CAMLprim value lwt_unix_blit(value val_buf1, value val_ofs1, value val_buf2,
                             value val_ofs2, value val_len) {
  memmove((char *)Caml_ba_data_val(val_buf2) + Long_val(val_ofs2),
          (char *)Caml_ba_data_val(val_buf1) + Long_val(val_ofs1),
          Long_val(val_len));
  return Val_unit;
}

CAMLprim value lwt_unix_blit_from_bytes(value val_buf1, value val_ofs1,
                                        value val_buf2, value val_ofs2,
                                        value val_len) {
  memcpy((char *)Caml_ba_data_val(val_buf2) + Long_val(val_ofs2),
         String_val(val_buf1) + Long_val(val_ofs1), Long_val(val_len));
  return Val_unit;
}

CAMLprim value lwt_unix_blit_to_bytes(value val_buf1, value val_ofs1,
                                      value val_buf2, value val_ofs2,
                                      value val_len) {
  memcpy(String_val(val_buf2) + Long_val(val_ofs2),
         (char *)Caml_ba_data_val(val_buf1) + Long_val(val_ofs1),
         Long_val(val_len));
  return Val_unit;
}

CAMLprim value lwt_unix_fill_bytes(value val_buf, value val_ofs, value val_len,
                                   value val_char) {
  memset((char *)Caml_ba_data_val(val_buf) + Long_val(val_ofs),
         Int_val(val_char), Long_val(val_len));
  return Val_unit;
}

CAMLprim value lwt_unix_mapped(value v_bstr) {
  return Val_bool(Caml_ba_array_val(v_bstr)->flags & CAML_BA_MAPPED_FILE);
}

/* +-----------------------------------------------------------------+
   | Byte order                                                      |
   +-----------------------------------------------------------------+ */

value lwt_unix_system_byte_order() {
#ifdef ARCH_BIG_ENDIAN
  return Val_int(1);
#else
  return Val_int(0);
#endif
}

/* +-----------------------------------------------------------------+
   | Threading                                                       |
   +-----------------------------------------------------------------+ */

#if defined(HAVE_PTHREAD)

int lwt_unix_launch_thread(void *(*start)(void *), void *data) {
  pthread_t thread;
  pthread_attr_t attr;

  pthread_attr_init(&attr);

  /* The thread is created in detached state so we do not have to join
     it when it terminates: */
  pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);

  int zero_if_created_otherwise_errno =
      pthread_create(&thread, &attr, start, data);

  pthread_attr_destroy(&attr);

  return zero_if_created_otherwise_errno;
}

lwt_unix_thread lwt_unix_thread_self() { return pthread_self(); }

int lwt_unix_thread_equal(lwt_unix_thread thread1, lwt_unix_thread thread2) {
  return pthread_equal(thread1, thread2);
}

void lwt_unix_mutex_init(lwt_unix_mutex *mutex) {
  pthread_mutex_init(mutex, NULL);
}

void lwt_unix_mutex_destroy(lwt_unix_mutex *mutex) {
  pthread_mutex_destroy(mutex);
}

void lwt_unix_mutex_lock(lwt_unix_mutex *mutex) { pthread_mutex_lock(mutex); }

void lwt_unix_mutex_unlock(lwt_unix_mutex *mutex) {
  pthread_mutex_unlock(mutex);
}

void lwt_unix_condition_init(lwt_unix_condition *condition) {
  pthread_cond_init(condition, NULL);
}

void lwt_unix_condition_destroy(lwt_unix_condition *condition) {
  pthread_cond_destroy(condition);
}

void lwt_unix_condition_signal(lwt_unix_condition *condition) {
  pthread_cond_signal(condition);
}

void lwt_unix_condition_broadcast(lwt_unix_condition *condition) {
  pthread_cond_broadcast(condition);
}

void lwt_unix_condition_wait(lwt_unix_condition *condition,
                             lwt_unix_mutex *mutex) {
  pthread_cond_wait(condition, mutex);
}

#elif defined(LWT_ON_WINDOWS)

int lwt_unix_launch_thread(void *(*start)(void *), void *data) {
  HANDLE handle =
      CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)start, data, 0, NULL);
  if (handle)
    CloseHandle(handle);
  return 0;
}

lwt_unix_thread lwt_unix_thread_self() { return GetCurrentThreadId(); }

int lwt_unix_thread_equal(lwt_unix_thread thread1, lwt_unix_thread thread2) {
  return thread1 == thread2;
}

void lwt_unix_mutex_init(lwt_unix_mutex *mutex) {
  InitializeCriticalSection(mutex);
}

void lwt_unix_mutex_destroy(lwt_unix_mutex *mutex) {
  DeleteCriticalSection(mutex);
}

void lwt_unix_mutex_lock(lwt_unix_mutex *mutex) { EnterCriticalSection(mutex); }

void lwt_unix_mutex_unlock(lwt_unix_mutex *mutex) {
  LeaveCriticalSection(mutex);
}

struct wait_list {
  HANDLE event;
  struct wait_list *next;
};

struct lwt_unix_condition {
  CRITICAL_SECTION mutex;
  struct wait_list *waiters;
};

void lwt_unix_condition_init(lwt_unix_condition *condition) {
  InitializeCriticalSection(&condition->mutex);
  condition->waiters = NULL;
}

void lwt_unix_condition_destroy(lwt_unix_condition *condition) {
  DeleteCriticalSection(&condition->mutex);
}

void lwt_unix_condition_signal(lwt_unix_condition *condition) {
  struct wait_list *node;
  EnterCriticalSection(&condition->mutex);

  node = condition->waiters;
  if (node) {
    condition->waiters = node->next;
    SetEvent(node->event);
  }
  LeaveCriticalSection(&condition->mutex);
}

void lwt_unix_condition_broadcast(lwt_unix_condition *condition) {
  struct wait_list *node;
  EnterCriticalSection(&condition->mutex);
  for (node = condition->waiters; node; node = node->next)
    SetEvent(node->event);
  condition->waiters = NULL;
  LeaveCriticalSection(&condition->mutex);
}

void lwt_unix_condition_wait(lwt_unix_condition *condition,
                             lwt_unix_mutex *mutex) {
  struct wait_list node;

  /* Create the event for the notification. */
  node.event = CreateEvent(NULL, FALSE, FALSE, NULL);

  /* Add the node to the condition. */
  EnterCriticalSection(&condition->mutex);
  node.next = condition->waiters;
  condition->waiters = &node;
  LeaveCriticalSection(&condition->mutex);

  /* Release the mutex. */
  LeaveCriticalSection(mutex);

  /* Wait for a signal. */
  WaitForSingleObject(node.event, INFINITE);

  /* The event is no more used. */
  CloseHandle(node.event);

  /* Re-acquire the mutex. */
  EnterCriticalSection(mutex);
}

#else

#error "no threading library available!"

#endif

/* +-----------------------------------------------------------------+
   | Socketpair on windows                                           |
   +-----------------------------------------------------------------+ */

#if defined(LWT_ON_WINDOWS)

static void lwt_unix_socketpair(int domain, int type, int protocol,
                                SOCKET sockets[2]) {
  union {
    struct sockaddr_in inaddr;
    struct sockaddr addr;
  } a;
  SOCKET listener;
  int addrlen = sizeof(a.inaddr);
  int reuse = 1;
  DWORD err;

  sockets[0] = INVALID_SOCKET;
  sockets[1] = INVALID_SOCKET;

  listener = socket(domain, type, protocol);
  if (listener == INVALID_SOCKET) goto failure;

  memset(&a, 0, sizeof(a));
  a.inaddr.sin_family = domain;
  a.inaddr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
  a.inaddr.sin_port = 0;

  if (setsockopt(listener, SOL_SOCKET, SO_REUSEADDR, (char *)&reuse,
                 sizeof(reuse)) == -1)
    goto failure;

  if (bind(listener, &a.addr, sizeof(a.inaddr)) == SOCKET_ERROR) goto failure;

  memset(&a, 0, sizeof(a));
  if (getsockname(listener, &a.addr, &addrlen) == SOCKET_ERROR) goto failure;

  a.inaddr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
  a.inaddr.sin_family = AF_INET;

  if (listen(listener, 1) == SOCKET_ERROR) goto failure;

  sockets[0] = socket(domain, type, protocol);
  if (sockets[0] == INVALID_SOCKET) goto failure;

  if (connect(sockets[0], &a.addr, sizeof(a.inaddr)) == SOCKET_ERROR)
    goto failure;

  sockets[1] = accept(listener, NULL, NULL);
  if (sockets[1] == INVALID_SOCKET) goto failure;

  closesocket(listener);
  return;

failure:
  err = WSAGetLastError();
  closesocket(listener);
  closesocket(sockets[0]);
  closesocket(sockets[1]);
  win32_maperr(err);
  uerror("socketpair", Nothing);
}

static int socket_domain_table[] = {PF_UNIX, PF_INET};

static int socket_type_table[] = {SOCK_STREAM, SOCK_DGRAM, SOCK_RAW,
                                  SOCK_SEQPACKET};

CAMLprim value lwt_unix_socketpair_stub(value domain, value type,
                                        value protocol) {
  CAMLparam3(domain, type, protocol);
  CAMLlocal1(result);
  SOCKET sockets[2];
  lwt_unix_socketpair(socket_domain_table[Int_val(domain)],
                      socket_type_table[Int_val(type)], Int_val(protocol),
                      sockets);
  result = caml_alloc_tuple(2);
  Store_field(result, 0, win_alloc_socket(sockets[0]));
  Store_field(result, 1, win_alloc_socket(sockets[1]));
  CAMLreturn(result);
}

#endif

/* +-----------------------------------------------------------------+
   | Notifications                                                   |
   +-----------------------------------------------------------------+ */

/* The mutex used to send and receive notifications. */
static lwt_unix_mutex notification_mutex;

/* All pending notifications. */
static intnat *notifications = NULL;

/* The size of the notification buffer. */
static long notification_count = 0;

/* The index to the next available cell in the notification buffer. */
static long notification_index = 0;

/* The mode currently used for notifications. */
enum notification_mode {
  /* Not yet initialized. */
  NOTIFICATION_MODE_NOT_INITIALIZED,

  /* Initialized but no mode defined. */
  NOTIFICATION_MODE_NONE,

  /* Using an eventfd. */
  NOTIFICATION_MODE_EVENTFD,

  /* Using a pipe. */
  NOTIFICATION_MODE_PIPE,

  /* Using a pair of sockets (only on windows). */
  NOTIFICATION_MODE_WINDOWS
};

/* The current notification mode. */
static enum notification_mode notification_mode =
    NOTIFICATION_MODE_NOT_INITIALIZED;

/* Send one notification. */
static int (*notification_send)();

/* Read one notification. */
static int (*notification_recv)();

static void init_notifications() {
  lwt_unix_mutex_init(&notification_mutex);
  notification_count = 4096;
  notifications =
      (intnat *)lwt_unix_malloc(notification_count * sizeof(intnat));
}

static void resize_notifications() {
  long new_notification_count = notification_count * 2;
  intnat *new_notifications =
      (intnat *)lwt_unix_malloc(new_notification_count * sizeof(intnat));
  memcpy((void *)new_notifications, (void *)notifications,
         notification_count * sizeof(intnat));
  free(notifications);
  notifications = new_notifications;
  notification_count = new_notification_count;
}

void lwt_unix_send_notification(intnat id) {
  int ret;
#if !defined(LWT_ON_WINDOWS)
  sigset_t new_mask;
  sigset_t old_mask;
  int error;
  sigfillset(&new_mask);
  pthread_sigmask(SIG_SETMASK, &new_mask, &old_mask);
#else
  DWORD error;
#endif
  lwt_unix_mutex_lock(&notification_mutex);
  if (notification_index > 0) {
    /* There is already a pending notification in the buffer, no
       need to signal the main thread. */
    if (notification_index == notification_count) resize_notifications();
    notifications[notification_index++] = id;
  } else {
    /* There is none, notify the main thread. */
    notifications[notification_index++] = id;
    ret = notification_send();
#if defined(LWT_ON_WINDOWS)
    if (ret == SOCKET_ERROR) {
      error = WSAGetLastError();
      if (error != WSANOTINITIALISED) {
        lwt_unix_mutex_unlock(&notification_mutex);
        win32_maperr(error);
        uerror("send_notification", Nothing);
      } /* else we're probably shutting down, so ignore the error */
    }
#else
    if (ret < 0) {
      error = errno;
      lwt_unix_mutex_unlock(&notification_mutex);
      pthread_sigmask(SIG_SETMASK, &old_mask, NULL);
      unix_error(error, "send_notification", Nothing);
    }
#endif
  }
  lwt_unix_mutex_unlock(&notification_mutex);
#if !defined(LWT_ON_WINDOWS)
  pthread_sigmask(SIG_SETMASK, &old_mask, NULL);
#endif
}

value lwt_unix_send_notification_stub(value id) {
  lwt_unix_send_notification(Long_val(id));
  return Val_unit;
}

value lwt_unix_recv_notifications() {
  int ret, i, current_index;
  value result;
#if !defined(LWT_ON_WINDOWS)
  sigset_t new_mask;
  sigset_t old_mask;
  int error;
  sigfillset(&new_mask);
  pthread_sigmask(SIG_SETMASK, &new_mask, &old_mask);
#else
  DWORD error;
#endif
  lwt_unix_mutex_lock(&notification_mutex);
  /* Receive the signal. */
  ret = notification_recv();
#if defined(LWT_ON_WINDOWS)
  if (ret == SOCKET_ERROR) {
    error = WSAGetLastError();
    lwt_unix_mutex_unlock(&notification_mutex);
    win32_maperr(error);
    uerror("recv_notifications", Nothing);
  }
#else
  if (ret < 0) {
    error = errno;
    lwt_unix_mutex_unlock(&notification_mutex);
    pthread_sigmask(SIG_SETMASK, &old_mask, NULL);
    unix_error(error, "recv_notifications", Nothing);
  }
#endif

  do {
    /*
     release the mutex while calling caml_alloc,
     which may call gc and switch the thread,
     resulting in a classical deadlock,
     when thread in question tries another send
    */
    current_index = notification_index;
    lwt_unix_mutex_unlock(&notification_mutex);
    result = caml_alloc_tuple(current_index);
    lwt_unix_mutex_lock(&notification_mutex);
    /* check that no new notifications appeared meanwhile (rare) */
  } while (current_index != notification_index);

  /* Read all pending notifications. */
  for (i = 0; i < notification_index; i++)
    Field(result, i) = Val_long(notifications[i]);
  /* Reset the index. */
  notification_index = 0;
  lwt_unix_mutex_unlock(&notification_mutex);
#if !defined(LWT_ON_WINDOWS)
  pthread_sigmask(SIG_SETMASK, &old_mask, NULL);
#endif
  return result;
}

#if defined(LWT_ON_WINDOWS)

static SOCKET set_close_on_exec(SOCKET socket) {
  SOCKET new_socket;
  if (!DuplicateHandle(GetCurrentProcess(), (HANDLE)socket, GetCurrentProcess(),
                       (HANDLE *)&new_socket, 0L, FALSE,
                       DUPLICATE_SAME_ACCESS)) {
    win32_maperr(GetLastError());
    uerror("set_close_on_exec", Nothing);
  }
  closesocket(socket);
  return new_socket;
}

static SOCKET socket_r, socket_w;

static int windows_notification_send() {
  char buf = '!';
  return send(socket_w, &buf, 1, 0);
}

static int windows_notification_recv() {
  char buf;
  return recv(socket_r, &buf, 1, 0);
}

value lwt_unix_init_notification() {
  SOCKET sockets[2];

  switch (notification_mode) {
    case NOTIFICATION_MODE_NOT_INITIALIZED:
      notification_mode = NOTIFICATION_MODE_NONE;
      init_notifications();
      break;
    case NOTIFICATION_MODE_WINDOWS:
      notification_mode = NOTIFICATION_MODE_NONE;
      closesocket(socket_r);
      closesocket(socket_w);
      break;
    case NOTIFICATION_MODE_NONE:
      break;
    default:
      caml_failwith("notification system in unknown state");
  }

  /* Since pipes do not works with select, we need to use a pair of
     sockets. */
  lwt_unix_socketpair(AF_INET, SOCK_STREAM, IPPROTO_TCP, sockets);

  socket_r = set_close_on_exec(sockets[0]);
  socket_w = set_close_on_exec(sockets[1]);
  notification_mode = NOTIFICATION_MODE_WINDOWS;
  notification_send = windows_notification_send;
  notification_recv = windows_notification_recv;
  return win_alloc_socket(socket_r);
}

#else /* defined(LWT_ON_WINDOWS) */

static void set_close_on_exec(int fd) {
  int flags = fcntl(fd, F_GETFD, 0);
  if (flags == -1 || fcntl(fd, F_SETFD, flags | FD_CLOEXEC) == -1)
    uerror("set_close_on_exec", Nothing);
}

#if defined(HAVE_EVENTFD)

static int notification_fd;

static int eventfd_notification_send() {
  uint64_t buf = 1;
  return write(notification_fd, (char *)&buf, 8);
}

static int eventfd_notification_recv() {
  uint64_t buf;
  return read(notification_fd, (char *)&buf, 8);
}

#endif /* defined(HAVE_EVENTFD) */

static int notification_fds[2];

static int pipe_notification_send() {
  char buf;
  return write(notification_fds[1], &buf, 1);
}

static int pipe_notification_recv() {
  char buf;
  return read(notification_fds[0], &buf, 1);
}

value lwt_unix_init_notification() {
  switch (notification_mode) {
#if defined(HAVE_EVENTFD)
    case NOTIFICATION_MODE_EVENTFD:
      notification_mode = NOTIFICATION_MODE_NONE;
      if (close(notification_fd) == -1) uerror("close", Nothing);
      break;
#endif
    case NOTIFICATION_MODE_PIPE:
      notification_mode = NOTIFICATION_MODE_NONE;
      if (close(notification_fds[0]) == -1) uerror("close", Nothing);
      if (close(notification_fds[1]) == -1) uerror("close", Nothing);
      break;
    case NOTIFICATION_MODE_NOT_INITIALIZED:
      notification_mode = NOTIFICATION_MODE_NONE;
      init_notifications();
      break;
    case NOTIFICATION_MODE_NONE:
      break;
    default:
      caml_failwith("notification system in unknown state");
  }

#if defined(HAVE_EVENTFD)
  notification_fd = eventfd(0, 0);
  if (notification_fd != -1) {
    notification_mode = NOTIFICATION_MODE_EVENTFD;
    notification_send = eventfd_notification_send;
    notification_recv = eventfd_notification_recv;
    set_close_on_exec(notification_fd);
    return Val_int(notification_fd);
  }
#endif

  if (pipe(notification_fds) == -1) uerror("pipe", Nothing);
  set_close_on_exec(notification_fds[0]);
  set_close_on_exec(notification_fds[1]);
  notification_mode = NOTIFICATION_MODE_PIPE;
  notification_send = pipe_notification_send;
  notification_recv = pipe_notification_recv;
  return Val_int(notification_fds[0]);
}

#endif /* defined(LWT_ON_WINDOWS) */

/* +-----------------------------------------------------------------+
   | Signals                                                         |
   +-----------------------------------------------------------------+ */

#ifndef NSIG
#define NSIG 64
#endif

/* Notifications id for each monitored signal. */
static intnat signal_notifications[NSIG];

CAMLextern int caml_convert_signal_number(int);

/* Send a notification when a signal is received. */
static void handle_signal(int signum) {
  if (signum >= 0 && signum < NSIG) {
    intnat id = signal_notifications[signum];
    if (id != -1) {
#if defined(LWT_ON_WINDOWS)
      /* The signal handler must be reinstalled if we use the signal
         function. */
      signal(signum, handle_signal);
#endif
      lwt_unix_send_notification(id);
    }
  }
}

#if defined(LWT_ON_WINDOWS)
/* Handle Ctrl+C on windows. */
static BOOL WINAPI handle_break(DWORD event) {
  intnat id = signal_notifications[SIGINT];
  if (id == -1 || (event != CTRL_C_EVENT && event != CTRL_BREAK_EVENT))
    return FALSE;
  lwt_unix_send_notification(id);
  return TRUE;
}
#endif

/* Install a signal handler. */
CAMLprim value lwt_unix_set_signal(value val_signum, value val_notification) {
#if !defined(LWT_ON_WINDOWS)
  struct sigaction sa;
#endif
  int signum = caml_convert_signal_number(Int_val(val_signum));
  intnat notification = Long_val(val_notification);

  if (signum < 0 || signum >= NSIG)
    caml_invalid_argument("Lwt_unix.on_signal: unavailable signal");

  signal_notifications[signum] = notification;

#if defined(LWT_ON_WINDOWS)
  if (signum == SIGINT) {
    if (!SetConsoleCtrlHandler(handle_break, TRUE)) {
      signal_notifications[signum] = -1;
      win32_maperr(GetLastError());
      uerror("SetConsoleCtrlHandler", Nothing);
    }
  } else {
    if (signal(signum, handle_signal) == SIG_ERR) {
      signal_notifications[signum] = -1;
      uerror("signal", Nothing);
    }
  }
#else
  sa.sa_handler = handle_signal;
  sa.sa_flags = 0;
  sigemptyset(&sa.sa_mask);
  if (sigaction(signum, &sa, NULL) == -1) {
    signal_notifications[signum] = -1;
    uerror("sigaction", Nothing);
  }
#endif
  return Val_unit;
}

/* Remove a signal handler. */
CAMLprim value lwt_unix_remove_signal(value val_signum) {
#if !defined(LWT_ON_WINDOWS)
  struct sigaction sa;
#endif
  /* The signal number is valid here since it was when we did the
     set_signal. */
  int signum = caml_convert_signal_number(Int_val(val_signum));
  signal_notifications[signum] = -1;
#if defined(LWT_ON_WINDOWS)
  if (signum == SIGINT)
    SetConsoleCtrlHandler(NULL, FALSE);
  else
    signal(signum, SIG_DFL);
#else
  sa.sa_handler = SIG_DFL;
  sa.sa_flags = 0;
  sigemptyset(&sa.sa_mask);
  sigaction(signum, &sa, NULL);
#endif
  return Val_unit;
}

/* Mark all signals as non-monitored. */
CAMLprim value lwt_unix_init_signals(value Unit) {
  int i;
  for (i = 0; i < NSIG; i++) signal_notifications[i] = -1;
  return Val_unit;
}

/* +-----------------------------------------------------------------+
   | Job execution                                                   |
   +-----------------------------------------------------------------+ */

/* Execute the given job. */
static void execute_job(lwt_unix_job job) {
  DEBUG("executing the job");

  lwt_unix_mutex_lock(&job->mutex);

  /* Mark the job as running. */
  job->state = LWT_UNIX_JOB_STATE_RUNNING;

  /* Set the thread of the job. */
  job->thread = lwt_unix_thread_self();

  lwt_unix_mutex_unlock(&job->mutex);

  /* Execute the job. */
  job->worker(job);

  DEBUG("job done");

  lwt_unix_mutex_lock(&job->mutex);

  DEBUG("marking the job has done");

  /* Job is done. If the main thread stopped until now, asynchronous
     notification is not necessary. */
  job->state = LWT_UNIX_JOB_STATE_DONE;

  /* Send a notification if the main thread continued its execution
     before the job terminated. */
  if (job->fast == 0) {
    lwt_unix_mutex_unlock(&job->mutex);
    DEBUG("notifying the main thread");
    lwt_unix_send_notification(job->notification_id);
  } else {
    lwt_unix_mutex_unlock(&job->mutex);
    DEBUG("not notifying the main thread");
  }
}

/* +-----------------------------------------------------------------+
   | Thread pool                                                     |
   +-----------------------------------------------------------------+ */

/* Number of thread waiting for a job in the pool. */
static int thread_waiting_count = 0;

/* Number of started threads. */
static int thread_count = 0;

/* Maximum number of system threads that can be started. */
static int pool_size = 1000;

/* Condition on which pool threads are waiting. */
static lwt_unix_condition pool_condition;

/* Queue of pending jobs. It points to the last enqueued job.  */
static lwt_unix_job pool_queue = NULL;

/* The mutex which protect access to [pool_queue], [pool_condition]
   and [thread_waiting_count]. */
static lwt_unix_mutex pool_mutex;

/* +-----------------------------------------------------------------+
   | Thread switching                                                |
   +-----------------------------------------------------------------+ */

#if defined(LWT_UNIX_HAVE_ASYNC_SWITCH)

#define STACK_SIZE (256 * 1024)

/* Possible states of the main thread (i.e. the one executing the
   ocaml code). */
enum main_state {
  /* The main thread is running. */
  STATE_RUNNING,

  /* The main thread is doing a blocking call that has not yet
     terminated. */
  STATE_BLOCKED,
};

/* State of the main thread. */
static enum main_state main_state = STATE_RUNNING;

/* The main thread. */
static lwt_unix_thread main_thread;

/* A node in a list of stack frames. */
struct stack_frame {
  /* The stack frame itself. */
  sigjmp_buf checkpoint;

  /* The next available one. */
  struct stack_frame *next;
};

/* Stack frames available to do a blocking call. */
static struct stack_frame *blocking_call_enter = NULL;

/* Mutex to protect access to [blocking_call_enter]. */
static lwt_unix_mutex blocking_call_enter_mutex;

/* Where to go when the blocking call is done, or when it get
   scheduled. */
static sigjmp_buf blocking_call_leave;

/* Where to go to become a worjer */
static struct stack_frame *become_worker = NULL;

/* Value returned to the main thread when a blocking call terminates
   without being scheduled. */
#define CALL_SUCCEEDED 1

/* Value returned to the old main thread when a blocking call
   terminates but has been scheduled. */
#define CALL_SCHEDULED 2

/* The job to be executed on the first available alternative stack. */
static lwt_unix_job blocking_call = NULL;

/* The stack frame used for the current blocking call. */
static struct stack_frame *blocking_call_frame = NULL;

/* Flag which become [1] once the stack has been allocated. */
static int stack_allocated;

/* Function executed on an alternative stack. */
static void altstack_worker() {
  struct stack_frame *node;
  sigjmp_buf buf;

  if (stack_allocated == 1) return;
  stack_allocated = 1;

  /* The first passage is to register a new stack frame. */
  node = lwt_unix_new(struct stack_frame);

  if (sigsetjmp(node->checkpoint, 1) == 0) {
    /* Add it to the list of available stack frames. */
    lwt_unix_mutex_lock(&blocking_call_enter_mutex);
    node->next = blocking_call_enter;
    blocking_call_enter = node;
    lwt_unix_mutex_unlock(&blocking_call_enter_mutex);

  } else {
    /* Save the job to execute and the current stack frame before
       another thread can become the main thread. */
    lwt_unix_job job = blocking_call;
    struct stack_frame *frame = blocking_call_frame;

    /* Mark the main thread as blocked. */
    main_state = STATE_BLOCKED;

    DEBUG("signaling the pool condition variable");

    /* Maybe wakeup a worker so it can become the main thread. */
    lwt_unix_mutex_lock(&pool_mutex);
    lwt_unix_condition_signal(&pool_condition);
    lwt_unix_mutex_unlock(&pool_mutex);

    DEBUG("executing the blocking call");

    /* Execute the blocking call. */
    execute_job(job);

    DEBUG("blocking call done");

    lwt_unix_mutex_lock(&pool_mutex);

    if (lwt_unix_thread_equal(main_thread, lwt_unix_thread_self())) {
      /* We stayed the main thread, continue the execution
         normally. */
      main_state = STATE_RUNNING;

      lwt_unix_mutex_unlock(&pool_mutex);

      DEBUG("blocing call terminated without blocking, resuming");

      /* Leave the blocking call. */
      siglongjmp(blocking_call_leave, CALL_SUCCEEDED);
    } else {
      /* We did not stayed the main thread, we now become a worker. */

      assert(become_worker != NULL);

      /* Take and remove the first worker checkpoint. */
      node = become_worker;
      become_worker = node->next;

      lwt_unix_mutex_unlock(&pool_mutex);

      DEBUG("blocking call terminated after blocking, becoming a worker");

      /* Add the stack frame used for this call to the list of
         available ones. */
      lwt_unix_mutex_lock(&blocking_call_enter_mutex);
      frame->next = blocking_call_enter;
      blocking_call_enter = frame;
      /* Release the mutex only after the jump. */

      memcpy(&buf, &(node->checkpoint), sizeof(sigjmp_buf));
      free(node);
      siglongjmp(buf, 1);
    }
  }
}

/* Allocate a new stack for doing blocking calls. */
void alloc_new_stack() {
  DEBUG("allocate a new stack");

  stack_t old_stack, new_stack;
  struct sigaction old_sa, new_sa;

  /* Create the new stack. */
  new_stack.ss_flags = 0;
  new_stack.ss_size = STACK_SIZE;
  new_stack.ss_sp = lwt_unix_malloc(STACK_SIZE);

  /* Change the stack used for signals. */
  sigaltstack(&new_stack, &old_stack);

  stack_allocated = 0;

  /* Set up the custom signal handler. */
  new_sa.sa_handler = altstack_worker;
  new_sa.sa_flags = SA_ONSTACK;
  sigemptyset(&new_sa.sa_mask);
  sigaction(LWT_UNIX_SIGNAL_ASYNC_SWITCH, &new_sa, &old_sa);

  /* Save the stack frame. */
  raise(LWT_UNIX_SIGNAL_ASYNC_SWITCH);

  /* Restore the old signal handler. */
  sigaction(LWT_UNIX_SIGNAL_ASYNC_SWITCH, &old_sa, NULL);

  /* Restore the old alternative stack. */
  sigaltstack(&old_stack, NULL);
}

#endif /* defined(LWT_UNIX_HAVE_ASYNC_SWITCH) */

/* +-----------------------------------------------------------------+
   | Threading stuff initialization                                  |
   +-----------------------------------------------------------------+ */

/* Whether threading has been initialized. */
static int threading_initialized = 0;

/* Initialize the pool of thread. */
void initialize_threading() {
  if (threading_initialized == 0) {
    lwt_unix_mutex_init(&pool_mutex);
    lwt_unix_condition_init(&pool_condition);

#if defined(LWT_UNIX_HAVE_ASYNC_SWITCH)
    lwt_unix_mutex_init(&blocking_call_enter_mutex);
    main_thread = lwt_unix_thread_self();
#endif

    threading_initialized = 1;
  }
}

/* +-----------------------------------------------------------------+
   | Worker loop                                                     |
   +-----------------------------------------------------------------+ */

/* Function executed by threads of the pool. */
static void *worker_loop(void *data) {
  lwt_unix_job job = (lwt_unix_job)data;
#if defined(LWT_UNIX_HAVE_ASYNC_SWITCH)
  struct stack_frame *node;
#endif

#if defined(HAVE_PTHREAD)
  /* Block all signals, otherwise ocaml handlers defined with the
     module Sys may be executed in this thread, oops... */
  sigset_t mask;
  sigfillset(&mask);
  pthread_sigmask(SIG_SETMASK, &mask, NULL);
#endif

  /* Execute the initial job if any. */
  if (job != NULL) execute_job(job);

  while (1) {
    DEBUG("entering waiting section");

    lwt_unix_mutex_lock(&pool_mutex);

    DEBUG("waiting for something to do");

/* Wait for something to do. */
#if defined(LWT_UNIX_HAVE_ASYNC_SWITCH)
    while (pool_queue == NULL && main_state == STATE_RUNNING) {
      ++thread_waiting_count;
      lwt_unix_condition_wait(&pool_condition, &pool_mutex);
    }
#else
    while (pool_queue == NULL) {
      ++thread_waiting_count;
      lwt_unix_condition_wait(&pool_condition, &pool_mutex);
    }
#endif

    DEBUG("received something to do");

#if defined(LWT_UNIX_HAVE_ASYNC_SWITCH)
    if (main_state == STATE_BLOCKED) {
      DEBUG("main thread is blocked");
      DEBUG("\e[1;31mswitching\e[0m");

      /* If the main thread is blocked, we become the main thread. */
      main_thread = lwt_unix_thread_self();

      /* The new main thread is running again. */
      main_state = STATE_RUNNING;

      node = lwt_unix_new(struct stack_frame);

      /* Save the stack frame so the old main thread can become a
         worker when the blocking call terminates. */
      if (sigsetjmp(node->checkpoint, 1) == 0) {
        DEBUG("checkpoint for future worker done");

        /* Save the stack frame in the list of worker checkpoints. */
        node->next = become_worker;
        become_worker = node;

        DEBUG("going back to the ocaml code");

        /* Go to before the blocking call. */
        siglongjmp(blocking_call_leave, CALL_SCHEDULED);
      }

      DEBUG("transformation to worker done");

      /* This thread is not running caml code anymore. */
      // caml_c_thread_unregister();

      /* Release this mutex. It was locked before the jump. */
      lwt_unix_mutex_unlock(&blocking_call_enter_mutex);

    } else {
#endif /* defined(LWT_UNIX_HAVE_ASYNC_SWITCH) */
      DEBUG("taking a job to execute");

      /* Take the first queued job. */
      job = pool_queue->next;

      /* Remove it from the queue. */
      if (job->next == job)
        pool_queue = NULL;
      else
        pool_queue->next = job->next;

      lwt_unix_mutex_unlock(&pool_mutex);

      /* Execute the job. */
      execute_job(job);
#if defined(LWT_UNIX_HAVE_ASYNC_SWITCH)
    }
#endif
  }

  return NULL;
}

/* +-----------------------------------------------------------------+
   | Jobs                                                            |
   +-----------------------------------------------------------------+ */

/* Description of jobs. */
struct custom_operations job_ops = {
    "lwt.unix.job",      custom_finalize_default,  custom_compare_default,
    custom_hash_default, custom_serialize_default, custom_deserialize_default};

/* Get the job structure contained in a custom value. */
#define Job_val(v) *(lwt_unix_job *)Data_custom_val(v)

value lwt_unix_alloc_job(lwt_unix_job job) {
  value val_job = caml_alloc_custom(&job_ops, sizeof(lwt_unix_job), 0, 1);
  Job_val(val_job) = job;
  return val_job;
}

void lwt_unix_free_job(lwt_unix_job job) {
  if (job->async_method != LWT_UNIX_ASYNC_METHOD_NONE)
    lwt_unix_mutex_destroy(&job->mutex);
  free(job);
}

CAMLprim value lwt_unix_start_job(value val_job, value val_async_method) {
  lwt_unix_job job = Job_val(val_job);
#if defined(LWT_UNIX_HAVE_ASYNC_SWITCH)
  struct stack_frame *node;
#endif
  lwt_unix_async_method async_method = Int_val(val_async_method);
  int done = 0;

  /* Fallback to synchronous call if there is no worker available and
     we can not launch more threads. */
  if (async_method != LWT_UNIX_ASYNC_METHOD_NONE && thread_waiting_count == 0 &&
      thread_count >= pool_size)
    async_method = LWT_UNIX_ASYNC_METHOD_NONE;

  /* Initialises job parameters. */
  job->state = LWT_UNIX_JOB_STATE_PENDING;
  job->fast = 1;
  job->async_method = async_method;

  switch (async_method) {
    case LWT_UNIX_ASYNC_METHOD_NONE:
      /* Execute the job synchronously. */
      caml_enter_blocking_section();
      job->worker(job);
      caml_leave_blocking_section();
      return Val_true;

    case LWT_UNIX_ASYNC_METHOD_DETACH:
      initialize_threading();

      lwt_unix_mutex_init(&job->mutex);

      lwt_unix_mutex_lock(&pool_mutex);
      if (thread_waiting_count == 0) {
        /* Try to start a new worker. */
        int zero_if_started_otherwise_errno =
            lwt_unix_launch_thread(worker_loop, (void *)job);

        /* Increment the worker thread count while still holding the mutex. */
        if (zero_if_started_otherwise_errno == 0)
            ++thread_count;

        lwt_unix_mutex_unlock(&pool_mutex);

        /* If the worker thread was not started, raise an exception. This must
           be done with the mutex unlocked, as it can involve a surprising
           control transfer. */
        if (zero_if_started_otherwise_errno != 0) {
            unix_error(
                zero_if_started_otherwise_errno, "launch_thread", Nothing);
        }
      } else {
        /* Add the job at the end of the queue. */
        if (pool_queue == NULL) {
          pool_queue = job;
          job->next = job;
        } else {
          job->next = pool_queue->next;
          pool_queue->next = job;
          pool_queue = job;
        }
        /* Wakeup one worker. */
        --thread_waiting_count;
        lwt_unix_condition_signal(&pool_condition);
        lwt_unix_mutex_unlock(&pool_mutex);
      }

      done = job->state == LWT_UNIX_JOB_STATE_DONE;
      if (done) {
        /* Wait for the mutex to be released because the job is going to
           be freed immediately. */
        lwt_unix_mutex_lock(&job->mutex);
        lwt_unix_mutex_unlock(&job->mutex);
      }

      return Val_bool(done);

    case LWT_UNIX_ASYNC_METHOD_SWITCH:
#if defined(LWT_UNIX_SIGNAL_ASYNC_SWITCH)
      if (SIGRTMIN > SIGRTMAX)
        caml_invalid_argument("the switch method is not supported");

      initialize_threading();

      lwt_unix_mutex_init(&job->mutex);
      job->thread = main_thread;

      /* Ensures there is at least one thread that can become the main
         thread. */
      if (thread_waiting_count == 0) {
        int zero_if_started_otherwise_errno =
            lwt_unix_launch_thread(worker_loop, NULL);

        if (zero_if_started_otherwise_errno == 0)
            ++thread_count;
        else
            unix_error(
                zero_if_started_otherwise_errno, "launch_thread", Nothing);
      }

      if (blocking_call_enter == NULL) alloc_new_stack();

      DEBUG("taking a stack frame for doing a blocking call");

      /* Take and remove the first available stack frame for system
         calls. */
      lwt_unix_mutex_lock(&blocking_call_enter_mutex);
      assert(blocking_call_enter != NULL);
      node = blocking_call_enter;
      blocking_call_enter = node->next;
      lwt_unix_mutex_unlock(&blocking_call_enter_mutex);

      /* Save the stack frame to leave the blocking call. */
      switch (sigsetjmp(blocking_call_leave, 1)) {
        case 0:
          /* Save the job to do. */
          blocking_call = job;

          /* Save the stack frame that will be used for this call in case
             it get scheduled. */
          blocking_call_frame = node;

          DEBUG("jumping to do a blocking call");

          /* Jump to an alternative stack and do the call. */
          siglongjmp(node->checkpoint, 1);

        case CALL_SUCCEEDED:
          DEBUG("resuming without being scheduled");

          /* Re-add the stack frame used for the call to the list of
             available ones. */
          lwt_unix_mutex_lock(&blocking_call_enter_mutex);
          node->next = blocking_call_enter;
          blocking_call_enter = node;
          lwt_unix_mutex_unlock(&blocking_call_enter_mutex);
          return Val_true;

        case CALL_SCHEDULED:
          DEBUG("resuming after being scheduled");

          /* This mutex was locked before we did the jump. */
          lwt_unix_mutex_unlock(&pool_mutex);

          /* This thread is now running caml code. */
          // caml_c_thread_register();

          done = job->state == LWT_UNIX_JOB_STATE_DONE;
          if (done) {
            lwt_unix_mutex_lock(&job->mutex);
            lwt_unix_mutex_unlock(&job->mutex);
          }

          return Val_bool(done);
      }

#else /* defined(LWT_UNIX_SIGNAL_ASYNC_SWITCH) */
      caml_invalid_argument("the switch method is not supported");
#endif
  }

  return Val_false;
}

CAMLprim value lwt_unix_check_job(value val_job, value val_notification_id) {
  lwt_unix_job job = Job_val(val_job);
  value result;

  DEBUG("checking job");

  switch (job->async_method) {
    case LWT_UNIX_ASYNC_METHOD_NONE:
      return Val_int(1);

    case LWT_UNIX_ASYNC_METHOD_DETACH:
    case LWT_UNIX_ASYNC_METHOD_SWITCH:
      lwt_unix_mutex_lock(&job->mutex);
      /* We are not waiting anymore. */
      job->fast = 0;
      /* Set the notification id for asynchronous wakeup. */
      job->notification_id = Long_val(val_notification_id);
      result = Val_bool(job->state == LWT_UNIX_JOB_STATE_DONE);
      lwt_unix_mutex_unlock(&job->mutex);

      DEBUG("job done: %d", Int_val(result));

      return result;
  }

  return Val_int(0);
}

CAMLprim value lwt_unix_self_result(value val_job) {
  lwt_unix_job job = Job_val(val_job);
  return job->result(job);
}

CAMLprim value lwt_unix_run_job_sync(value val_job) {
  lwt_unix_job job = Job_val(val_job);
  /* So lwt_unix_free_job won't try to destroy the mutex. */
  job->async_method = LWT_UNIX_ASYNC_METHOD_NONE;
  caml_enter_blocking_section();
  job->worker(job);
  caml_leave_blocking_section();
  return job->result(job);
}

CAMLprim value lwt_unix_reset_after_fork(value Unit) {
  if (threading_initialized) {
#if defined(LWT_UNIX_HAVE_ASYNC_SWITCH)
    /* Reset the main thread. */
    main_thread = lwt_unix_thread_self();
#endif

    /* There is no more waiting threads. */
    thread_waiting_count = 0;

    /* There is no more threads. */
    thread_count = 0;

    /* Empty the queue. */
    pool_queue = NULL;

    threading_initialized = 0;
  }

  return Val_unit;
}

/* +-----------------------------------------------------------------+
   | Statistics and control                                          |
   +-----------------------------------------------------------------+ */

CAMLprim value lwt_unix_pool_size(value Unit) { return Val_int(pool_size); }

CAMLprim value lwt_unix_set_pool_size(value val_size) {
  pool_size = Int_val(val_size);
  return Val_unit;
}

CAMLprim value lwt_unix_thread_count(value Unit) {
  return Val_int(thread_count);
}

CAMLprim value lwt_unix_thread_waiting_count(value Unit) {
  return Val_int(thread_waiting_count);
}
