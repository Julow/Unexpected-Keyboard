/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



/* Stubs for libev */

#include "lwt_config.h"
#include "lwt_unix.h"

#if defined(HAVE_LIBEV)

#define CAML_NAME_SPACE

#include <assert.h>

#include <caml/alloc.h>
#include <caml/callback.h>
#include <caml/custom.h>
#include <caml/fail.h>
#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/signals.h>
#include <ev.h>

/* +-----------------------------------------------------------------+
   | Backend types                                                   |
   +-----------------------------------------------------------------+ */
enum {
  val_EVBACKEND_DEFAULT,
  val_EVBACKEND_SELECT,
  val_EVBACKEND_POLL,
  val_EVBACKEND_EPOLL,
  val_EVBACKEND_KQUEUE,
  val_EVBACKEND_DEVPOLL,
  val_EVBACKEND_PORT
};

static unsigned int backend_val(value v) {
  switch (Int_val(v)) {
    case val_EVBACKEND_DEFAULT:
      return 0;
    case val_EVBACKEND_SELECT:
      return EVBACKEND_SELECT;
    case val_EVBACKEND_POLL:
      return EVBACKEND_POLL;
    case val_EVBACKEND_EPOLL:
      return EVBACKEND_EPOLL;
    case val_EVBACKEND_KQUEUE:
      return EVBACKEND_KQUEUE;
    case val_EVBACKEND_DEVPOLL:
      return EVBACKEND_DEVPOLL;
    case val_EVBACKEND_PORT:
      return EVBACKEND_PORT;
    default:
      assert(0);
  }
}

/* +-----------------------------------------------------------------+
   | Loops                                                           |
   +-----------------------------------------------------------------+ */

static int compare_loops(value a, value b) {
  return (int)((char *)Ev_loop_val(a) - (char *)Ev_loop_val(b));
}

static long hash_loop(value loop) { return (long)Ev_loop_val(loop); }

static struct custom_operations loop_ops = {
    "lwt.libev.loop", custom_finalize_default,  compare_loops,
    hash_loop,        custom_serialize_default, custom_deserialize_default};

/* Do nothing.

   We replace the invoke_pending callback of the event loop, so when
   events are ready, they can be executed after ev_loop has returned:
   it is executed in a blocking section and callbacks must be executed
   outside.
 */
static void nop(struct ev_loop *loop) {}

CAMLprim value lwt_libev_init(value backend) {
  struct ev_loop *loop = ev_loop_new(EVFLAG_FORKCHECK | backend_val(backend));
  if (!loop) caml_failwith("lwt_libev_init");
  /* Remove the invoke_pending callback. */
  ev_set_invoke_pending_cb(loop, nop);
  value result = caml_alloc_custom(&loop_ops, sizeof(struct ev_loop *), 0, 1);
  Ev_loop_val(result) = loop;
  return result;
}

CAMLprim value lwt_libev_stop(value loop) {
  ev_loop_destroy(Ev_loop_val(loop));
  return Val_unit;
}

CAMLprim value lwt_libev_loop(value val_loop, value val_block) {
  struct ev_loop *loop = Ev_loop_val(val_loop);
  /* Call the event loop inside a blocking section. */
  caml_enter_blocking_section();
  ev_loop(loop, Bool_val(val_block) ? EVLOOP_ONESHOT
                                    : EVLOOP_ONESHOT | EVLOOP_NONBLOCK);
  caml_leave_blocking_section();
  /* Invoke callbacks now, i.e. outside the blocking section. */
  ev_invoke_pending(loop);
  return Val_unit;
}

CAMLprim value lwt_libev_unloop(value loop) {
  ev_unloop(Ev_loop_val(loop), EVUNLOOP_ONE);
  return Val_unit;
}

/* +-----------------------------------------------------------------+
   | Watchers                                                        |
   +-----------------------------------------------------------------+ */

#define Ev_io_val(v) *(struct ev_io **)Data_custom_val(v)
#define Ev_timer_val(v) *(struct ev_timer **)Data_custom_val(v)

static int compare_watchers(value a, value b) {
  return (int)((char *)Ev_io_val(a) - (char *)Ev_io_val(b));
}

static long hash_watcher(value watcher) { return (long)Ev_io_val(watcher); }

static struct custom_operations watcher_ops = {
    "lwt.libev.watcher", custom_finalize_default,  compare_watchers,
    hash_watcher,        custom_serialize_default, custom_deserialize_default};

/* +-----------------------------------------------------------------+
   | IO watchers                                                     |
   +-----------------------------------------------------------------+ */

static void handle_io(struct ev_loop *loop, ev_io *watcher, int revents) {
  caml_callback((value)watcher->data, Val_unit);
}

static value lwt_libev_io_init(struct ev_loop *loop, int fd, int event,
                               value callback) {
  CAMLparam1(callback);
  CAMLlocal1(result);
  /* Create and initialise the watcher */
  struct ev_io *watcher = lwt_unix_new(struct ev_io);
  ev_io_init(watcher, handle_io, fd, event);
  /* Wrap the watcher into a custom caml value */
  result = caml_alloc_custom(&watcher_ops, sizeof(struct ev_io *), 0, 1);
  Ev_io_val(result) = watcher;
  /* Store the callback in the watcher, and register it as a root */
  watcher->data = (void *)callback;
  caml_register_generational_global_root((value *)(&(watcher->data)));
  /* Start the event */
  ev_io_start(loop, watcher);
  CAMLreturn(result);
}

CAMLprim value lwt_libev_readable_init(value loop, value fd, value callback) {
  return lwt_libev_io_init(Ev_loop_val(loop), FD_val(fd), EV_READ, callback);
}

CAMLprim value lwt_libev_writable_init(value loop, value fd, value callback) {
  return lwt_libev_io_init(Ev_loop_val(loop), FD_val(fd), EV_WRITE, callback);
}

CAMLprim value lwt_libev_io_stop(value loop, value val_watcher) {
  CAMLparam2(loop, val_watcher);
  struct ev_io *watcher = Ev_io_val(val_watcher);
  caml_remove_generational_global_root((value *)(&(watcher->data)));
  ev_io_stop(Ev_loop_val(loop), watcher);
  free(watcher);
  CAMLreturn(Val_unit);
}

/* +-----------------------------------------------------------------+
   | Timer watchers                                                  |
   +-----------------------------------------------------------------+ */

static void handle_timer(struct ev_loop *loop, ev_timer *watcher, int revents) {
  caml_callback((value)watcher->data, Val_unit);
}

CAMLprim value lwt_libev_timer_init(value loop, value delay, value repeat,
                                    value callback) {
  CAMLparam4(loop, delay, repeat, callback);
  CAMLlocal1(result);

  struct ev_loop* ev_loop = Ev_loop_val(loop);
  /* Create and initialise the watcher */
  struct ev_timer *watcher = lwt_unix_new(struct ev_timer);

  ev_tstamp adjusted_delay = Double_val(delay) + ev_time() - ev_now(ev_loop);
  if (Bool_val(repeat))
    ev_timer_init(watcher, handle_timer, adjusted_delay, Double_val(delay));
  else
    ev_timer_init(watcher, handle_timer, adjusted_delay, 0.0);

  /* Wrap the watcher into a custom caml value */
  result = caml_alloc_custom(&watcher_ops, sizeof(struct ev_timer *), 0, 1);
  Ev_timer_val(result) = watcher;
  /* Store the callback in the watcher, and register it as a root */
  watcher->data = (void *)callback;
  caml_register_generational_global_root((value *)(&(watcher->data)));
  /* Start the event */
  ev_timer_start(ev_loop, watcher);
  CAMLreturn(result);
}

CAMLprim value lwt_libev_timer_stop(value loop, value val_watcher) {
  CAMLparam2(loop, val_watcher);
  struct ev_timer *watcher = Ev_timer_val(val_watcher);
  caml_remove_generational_global_root((value *)(&(watcher->data)));
  ev_timer_stop(Ev_loop_val(loop), watcher);
  free(watcher);
  CAMLreturn(Val_unit);
}

#else

LWT_NOT_AVAILABLE1(libev_init)
LWT_NOT_AVAILABLE1(libev_stop)
LWT_NOT_AVAILABLE2(libev_loop)
LWT_NOT_AVAILABLE1(libev_unloop)
LWT_NOT_AVAILABLE3(libev_readable_init)
LWT_NOT_AVAILABLE3(libev_writable_init)
LWT_NOT_AVAILABLE2(libev_io_stop)
LWT_NOT_AVAILABLE4(libev_timer_init)
LWT_NOT_AVAILABLE2(libev_timer_stop)

#endif
