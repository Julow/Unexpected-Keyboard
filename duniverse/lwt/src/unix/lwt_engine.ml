(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



(* [Lwt_sequence] is deprecated – we don't want users outside Lwt using it.
   However, it is still used internally by Lwt. So, briefly disable warning 3
   ("deprecated"), and create a local, non-deprecated alias for
   [Lwt_sequence] that can be referred to by the rest of the code in this
   module without triggering any more warnings. *)
[@@@ocaml.warning "-3"]
module Lwt_sequence = Lwt_sequence
[@@@ocaml.warning "+3"]

(* +-----------------------------------------------------------------+
   | Events                                                          |
   +-----------------------------------------------------------------+ *)

type _event = {
  stop : unit Lazy.t;
  (* The stop method of the event. *)
  node : Obj.t Lwt_sequence.node;
  (* The node in the sequence of registered events. *)
}

type event = _event ref

external cast_node : 'a Lwt_sequence.node -> Obj.t Lwt_sequence.node = "%identity"

let stop_event ev =
  let ev = !ev in
  Lwt_sequence.remove ev.node;
  Lazy.force ev.stop

let _fake_event = {
  stop = lazy ();
  node = Lwt_sequence.add_l (Obj.repr ()) (Lwt_sequence.create ());
}

let fake_event = ref _fake_event

(* +-----------------------------------------------------------------+
   | Engines                                                         |
   +-----------------------------------------------------------------+ *)

class virtual abstract = object(self)
  method virtual iter : bool -> unit
  method virtual private cleanup : unit
  method virtual private register_readable : Unix.file_descr -> (unit -> unit) -> unit Lazy.t
  method virtual private register_writable : Unix.file_descr -> (unit -> unit) -> unit Lazy.t
  method virtual private register_timer : float -> bool -> (unit -> unit) -> unit Lazy.t

  val readables = Lwt_sequence.create ()
  (* Sequence of callbacks waiting for a file descriptor to become
     readable. *)

  val writables = Lwt_sequence.create ()
  (* Sequence of callbacks waiting for a file descriptor to become
     writable. *)

  val timers = Lwt_sequence.create ()
  (* Sequence of timers. *)

  method destroy =
    Lwt_sequence.iter_l (fun (_fd, _f, _g, ev) -> stop_event ev) readables;
    Lwt_sequence.iter_l (fun (_fd, _f, _g, ev) -> stop_event ev) writables;
    Lwt_sequence.iter_l (fun (_delay, _repeat, _f, _g, ev) -> stop_event ev)
      timers;
    self#cleanup

  method transfer (engine : abstract) =
    Lwt_sequence.iter_l (fun (fd, f, _g, ev) ->
      stop_event ev; ev := !(engine#on_readable fd f)) readables;
    Lwt_sequence.iter_l (fun (fd, f, _g, ev) ->
      stop_event ev; ev := !(engine#on_writable fd f)) writables;
    Lwt_sequence.iter_l (fun (delay, repeat, f, _g, ev) ->
      stop_event ev; ev := !(engine#on_timer delay repeat f)) timers

  method fake_io fd =
    Lwt_sequence.iter_l (fun (fd', _f, g, _stop) ->
      if fd = fd' then g ()) readables;
    Lwt_sequence.iter_l (fun (fd', _f, g, _stop) ->
      if fd = fd' then g ()) writables

  method on_readable fd f =
    let ev = ref _fake_event in
    let g () = f ev in
    let stop = self#register_readable fd g in
    ev := { stop = stop; node = cast_node (Lwt_sequence.add_r (fd, f, g, ev) readables) };
    ev

  method on_writable fd f =
    let ev = ref _fake_event in
    let g () = f ev in
    let stop = self#register_writable fd g in
    ev := { stop = stop; node = cast_node (Lwt_sequence.add_r (fd, f, g, ev) writables) } ;
    ev

  method on_timer delay repeat f =
    let ev = ref _fake_event in
    let g () = f ev in
    let stop = self#register_timer delay repeat g in
    ev := { stop = stop; node = cast_node (Lwt_sequence.add_r (delay, repeat, f, g, ev) timers) };
    ev

  method readable_count = Lwt_sequence.length readables
  method writable_count = Lwt_sequence.length writables
  method timer_count = Lwt_sequence.length timers
end

class type t = object
  inherit abstract

  method iter : bool -> unit
  method private cleanup : unit
  method private register_readable : Unix.file_descr -> (unit -> unit) -> unit Lazy.t
  method private register_writable : Unix.file_descr -> (unit -> unit) -> unit Lazy.t
  method private register_timer : float -> bool -> (unit -> unit) -> unit Lazy.t
end

(* +-----------------------------------------------------------------+
   | The libev engine                                                |
   +-----------------------------------------------------------------+ *)

type ev_loop
type ev_io
type ev_timer

module Ev_backend =
struct
  type t =
    | EV_DEFAULT
    | EV_SELECT
    | EV_POLL
    | EV_EPOLL
    | EV_KQUEUE
    | EV_DEVPOLL
    | EV_PORT

  let default = EV_DEFAULT
  let select = EV_SELECT
  let poll = EV_POLL
  let epoll = EV_EPOLL
  let kqueue = EV_KQUEUE
  let devpoll = EV_DEVPOLL
  let port = EV_PORT

  let name = function
    | EV_DEFAULT -> "EV_DEFAULT"
    | EV_SELECT -> "EV_SELECT"
    | EV_POLL -> "EV_POLL"
    | EV_EPOLL -> "EV_EPOLL"
    | EV_KQUEUE -> "EV_KQUEUE"
    | EV_DEVPOLL -> "EV_DEVPOLL"
    | EV_PORT -> "EV_PORT"

  let pp fmt t = Format.pp_print_string fmt (name t)
end

external ev_init : Ev_backend.t -> ev_loop = "lwt_libev_init"
external ev_stop : ev_loop -> unit = "lwt_libev_stop"
external ev_loop : ev_loop -> bool -> unit = "lwt_libev_loop"
external ev_unloop : ev_loop -> unit = "lwt_libev_unloop"
external ev_readable_init : ev_loop -> Unix.file_descr -> (unit -> unit) -> ev_io = "lwt_libev_readable_init"
external ev_writable_init : ev_loop -> Unix.file_descr -> (unit -> unit) -> ev_io = "lwt_libev_writable_init"
external ev_io_stop : ev_loop -> ev_io -> unit = "lwt_libev_io_stop"
external ev_timer_init : ev_loop -> float -> bool -> (unit -> unit) -> ev_timer = "lwt_libev_timer_init"
external ev_timer_stop : ev_loop -> ev_timer -> unit  = "lwt_libev_timer_stop"

class libev ?(backend=Ev_backend.default) () = object
  inherit abstract

  val loop = ev_init backend
  method loop = loop

  method private cleanup = ev_stop loop

  method iter block =
    try
      ev_loop loop block
    with exn ->
      ev_unloop loop;
      raise exn

  method private register_readable fd f =
    let ev = ev_readable_init loop fd f in
    lazy(ev_io_stop loop ev)

  method private register_writable fd f =
    let ev = ev_writable_init loop fd f in
    lazy(ev_io_stop loop ev)

  method private register_timer delay repeat f =
    let ev = ev_timer_init loop delay repeat f in
    lazy(ev_timer_stop loop ev)
end

class libev_deprecated = libev ()

(* +-----------------------------------------------------------------+
   | Select/poll based engines                                       |
   +-----------------------------------------------------------------+ *)

(* Type of a sleeper for the select engine. *)
type sleeper = {
  mutable time : float;
  (* The time at which the sleeper should be wakeup. *)

  mutable stopped : bool;
  (* [true] iff the event has been stopped. *)

  action : unit -> unit;
  (* The action for the sleeper. *)
}

module Sleep_queue =
  Lwt_pqueue.Make(struct
    type t = sleeper
    let compare {time = t1; _} {time = t2; _} = compare t1 t2
  end)
  [@@ocaml.warning "-3"]

module Fd_map = Map.Make(struct type t = Unix.file_descr let compare = compare end)

let rec restart_actions sleep_queue now =
  match Sleep_queue.lookup_min sleep_queue with
  | Some{ stopped = true; _ } ->
    restart_actions (Sleep_queue.remove_min sleep_queue) now
  | Some{ time = time; action = action; _ } when time <= now ->
    (* We have to remove the sleeper to the queue before performing
       the action. The action can change the sleeper's time, and this
       might break the priority queue invariant if the sleeper is
       still in the queue. *)
    let q = Sleep_queue.remove_min sleep_queue in
    action ();
    restart_actions q now
  | _ ->
    sleep_queue

let rec get_next_timeout sleep_queue =
  match Sleep_queue.lookup_min sleep_queue with
  | Some{ stopped = true; _ } ->
    get_next_timeout (Sleep_queue.remove_min sleep_queue)
  | Some{ time = time; _ } ->
    max 0. (time -. Unix.gettimeofday ())
  | None ->
    -1.

let bad_fd fd =
  try
    let _ = Unix.fstat fd in
    false
  with Unix.Unix_error (_, _, _) ->
    true

let invoke_actions fd map =
  match try Some(Fd_map.find fd map) with Not_found -> None with
  | Some actions -> Lwt_sequence.iter_l (fun f -> f ()) actions
  | None -> ()

class virtual select_or_poll_based = object
  inherit abstract

  val mutable sleep_queue = Sleep_queue.empty
  (* Threads waiting for a timeout to expire. *)

  val mutable new_sleeps = []
  (* Sleepers added since the last iteration of the main loop:

     They are not added immediately to the main sleep queue in order
     to prevent them from being wakeup immediately.  *)

  val mutable wait_readable = Fd_map.empty
  (* Sequences of actions waiting for file descriptors to become
     readable. *)

  val mutable wait_writable = Fd_map.empty
  (* Sequences of actions waiting for file descriptors to become
     writable. *)

  method private cleanup = ()

  method private register_timer delay repeat f =
    if repeat then begin
      let rec sleeper = { time = Unix.gettimeofday () +. delay; stopped = false; action = g }
      and g () =
        sleeper.time <- Unix.gettimeofday () +. delay;
        new_sleeps <- sleeper :: new_sleeps;
        f ()
      in
      new_sleeps <- sleeper :: new_sleeps;
      lazy(sleeper.stopped <- true)
    end else begin
      let sleeper = { time = Unix.gettimeofday () +. delay; stopped = false; action = f } in
      new_sleeps <- sleeper :: new_sleeps;
      lazy(sleeper.stopped <- true)
    end

  method private register_readable fd f =
    let actions =
      try
        Fd_map.find fd wait_readable
      with Not_found ->
        let actions = Lwt_sequence.create () in
        wait_readable <- Fd_map.add fd actions wait_readable;
        actions
    in
    let node = Lwt_sequence.add_l f actions in
    lazy(Lwt_sequence.remove node;
         if Lwt_sequence.is_empty actions then wait_readable <- Fd_map.remove fd wait_readable)

  method private register_writable fd f =
    let actions =
      try
        Fd_map.find fd wait_writable
      with Not_found ->
        let actions = Lwt_sequence.create () in
        wait_writable <- Fd_map.add fd actions wait_writable;
        actions
    in
    let node = Lwt_sequence.add_l f actions in
    lazy(Lwt_sequence.remove node;
         if Lwt_sequence.is_empty actions then wait_writable <- Fd_map.remove fd wait_writable)
end

class virtual select_based = object(self)
  inherit select_or_poll_based

  method private virtual select : Unix.file_descr list -> Unix.file_descr list -> float -> Unix.file_descr list * Unix.file_descr list

  method iter block =
    (* Transfer all sleepers added since the last iteration to the
       main sleep queue: *)
    sleep_queue <- List.fold_left (fun q e -> Sleep_queue.add e q) sleep_queue new_sleeps;
    new_sleeps <- [];
    (* Collect file descriptors. *)
    let fds_r = Fd_map.fold (fun fd _ l -> fd :: l) wait_readable [] in
    let fds_w = Fd_map.fold (fun fd _ l -> fd :: l) wait_writable [] in
    (* Compute the timeout. *)
    let timeout = if block then get_next_timeout sleep_queue else 0. in
    (* Do the blocking call *)
    let fds_r, fds_w =
      try
        self#select fds_r fds_w timeout
      with
      | Unix.Unix_error (Unix.EINTR, _, _) ->
        ([], [])
      | Unix.Unix_error (Unix.EBADF, _, _) ->
        (* Keeps only bad file descriptors. Actions registered on
           them have to handle the error: *)
        (List.filter bad_fd fds_r,
         List.filter bad_fd fds_w)
    in
    (* Restart threads waiting for a timeout: *)
    sleep_queue <- restart_actions sleep_queue (Unix.gettimeofday ());
    (* Restart threads waiting on a file descriptors: *)
    List.iter (fun fd -> invoke_actions fd wait_readable) fds_r;
    List.iter (fun fd -> invoke_actions fd wait_writable) fds_w
end

class virtual poll_based = object(self)
  inherit select_or_poll_based

  method private virtual poll : (Unix.file_descr * bool * bool) list -> float -> (Unix.file_descr * bool * bool) list

  method iter block =
    (* Transfer all sleepers added since the last iteration to the
       main sleep queue: *)
    sleep_queue <- List.fold_left (fun q e -> Sleep_queue.add e q) sleep_queue new_sleeps;
    new_sleeps <- [];
    (* Collect file descriptors. *)
    let fds = [] in
    let fds = Fd_map.fold (fun fd _ l -> (fd, true, false) :: l) wait_readable fds in
    let fds = Fd_map.fold (fun fd _ l -> (fd, false, true) :: l) wait_writable fds in
    (* Compute the timeout. *)
    let timeout = if block then get_next_timeout sleep_queue else 0. in
    (* Do the blocking call *)
    let fds =
      try
        self#poll fds timeout
      with
      | Unix.Unix_error (Unix.EINTR, _, _) ->
        []
      | Unix.Unix_error (Unix.EBADF, _, _) ->
        (* Keeps only bad file descriptors. Actions registered on
           them have to handle the error: *)
        List.filter (fun (fd, _, _) -> bad_fd fd) fds
    in
    (* Restart threads waiting for a timeout: *)
    sleep_queue <- restart_actions sleep_queue (Unix.gettimeofday ());
    (* Restart threads waiting on a file descriptors: *)
    List.iter
      (fun (fd, readable, writable) ->
         if readable then invoke_actions fd wait_readable;
         if writable then invoke_actions fd wait_writable)
      fds
end

class select = object
  inherit select_based

  method private select fds_r fds_w timeout =
    let fds_r, fds_w, _ = Unix.select fds_r fds_w [] timeout in
    (fds_r, fds_w)
end

(* +-----------------------------------------------------------------+
   | The current engine                                              |
   +-----------------------------------------------------------------+ *)

let current =
  if Lwt_config._HAVE_LIBEV && Lwt_config.libev_default then
    ref (new libev () :> t)
  else
    ref (new select :> t)

let get () =
  !current

let set ?(transfer=true) ?(destroy=true) engine =
  if transfer then !current#transfer (engine : #t :> abstract);
  if destroy then !current#destroy;
  current := (engine : #t :> t)

let iter block = !current#iter block
let on_readable fd f = !current#on_readable fd f
let on_writable fd f = !current#on_writable fd f
let on_timer delay repeat f = !current#on_timer delay repeat f
let fake_io fd = !current#fake_io fd
let readable_count () = !current#readable_count
let writable_count () = !current#writable_count
let timer_count () = !current#timer_count

module Versioned =
struct
  class libev_1 = libev_deprecated
  class libev_2 = libev
end
