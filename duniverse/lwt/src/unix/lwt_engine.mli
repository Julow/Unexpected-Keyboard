(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



(** Lwt unix main loop engine *)

(** {2 Events} *)

type event
  (** Type of events. An event represent a callback registered to be
      called when some event occurs. *)

val stop_event : event -> unit
  (** [stop_event event] stops the given event. *)

val fake_event : event
  (** Event which does nothing when stopped. *)

(** {2 Event loop functions} *)

val iter : bool -> unit
  (** [iter block] performs one iteration of the main loop. If [block]
      is [true] the function must block until one event becomes
      available, otherwise it should just check for available events
      and return immediately. *)

val on_readable : Unix.file_descr -> (event -> unit) -> event
  (** [on_readable fd f] calls [f] each time [fd] becomes readable. *)

val on_writable : Unix.file_descr -> (event -> unit) -> event
  (** [on_readable fd f] calls [f] each time [fd] becomes writable. *)

val on_timer : float -> bool -> (event -> unit) -> event
  (** [on_timer delay repeat f] calls [f] one time after [delay]
      seconds. If [repeat] is [true] then [f] is called each [delay]
      seconds, otherwise it is called only one time. *)

val readable_count : unit -> int
  (** Returns the number of events waiting for a file descriptor to
      become readable. *)

val writable_count : unit -> int
  (** Returns the number of events waiting for a file descriptor to
      become writable. *)

val timer_count : unit -> int
  (** Returns the number of registered timers. *)

val fake_io : Unix.file_descr -> unit
  (** Simulates activity on the given file descriptor. *)

(** {2 Engines} *)

(** An engine represents a set of functions used to register different
    kinds of callbacks for different kinds of events. *)

(** Abstract class for engines. *)
class virtual abstract : object
  method destroy : unit
    (** Destroy the engine, remove all its events and free its
        associated resources. *)

  method transfer : abstract -> unit
    (** [transfer engine] moves all events from the current engine to
        [engine]. Note that timers are reset in the destination
        engine, i.e. if a timer with a delay of 2 seconds was
        registered 1 second ago it will occur in 2 seconds in the
        destination engine. *)

  (** {2 Event loop methods} *)

  method virtual iter : bool -> unit
  method on_readable : Unix.file_descr -> (event -> unit) -> event
  method on_writable : Unix.file_descr -> (event -> unit) -> event
  method on_timer : float -> bool -> (event -> unit) -> event
  method fake_io : Unix.file_descr -> unit
  method readable_count : int
  method writable_count : int
  method timer_count : int

  (** {2 Backend methods} *)

  (** Notes:

      - the callback passed to register methods is of type [unit -> unit]
      and not [event -> unit]
      - register methods return a lazy value which unregisters the
      event when forced
  *)

  method virtual private cleanup : unit
    (** Cleanup resources associated with the engine. *)

  method virtual private register_readable : Unix.file_descr -> (unit -> unit) -> unit Lazy.t
  method virtual private register_writable : Unix.file_descr -> (unit -> unit) -> unit Lazy.t
  method virtual private register_timer : float -> bool -> (unit -> unit) -> unit Lazy.t
end

(** Type of engines. *)
class type t = object
  inherit abstract

  method iter : bool -> unit
  method private cleanup : unit
  method private register_readable : Unix.file_descr -> (unit -> unit) -> unit Lazy.t
  method private register_writable : Unix.file_descr -> (unit -> unit) -> unit Lazy.t
  method private register_timer : float -> bool -> (unit -> unit) -> unit Lazy.t
end

(** {2 Predefined engines} *)

type ev_loop

module Ev_backend :
sig
  type t
  val default : t
  val select : t
  val poll : t
  val epoll : t
  val kqueue : t
  val devpoll : t
  val port : t

  val pp : Format.formatter -> t -> unit
end

  (** Type of libev loops. *)

(** Engine based on libev. If not compiled with libev support, the
    creation of the class will raise {!Lwt_sys.Not_available}. *)
class libev : ?backend:Ev_backend.t -> unit -> object
  inherit t

  val loop : ev_loop
    (** The libev loop used for this engine. *)

  method loop : ev_loop
    (** Returns [loop]. *)
end

(** Engine based on [Unix.select]. *)
class select : t

(** Abstract class for engines based on a select-like function. *)
class virtual select_based : object
  inherit t

  method private virtual select : Unix.file_descr list -> Unix.file_descr list -> float -> Unix.file_descr list * Unix.file_descr list
    (** [select fds_r fds_w timeout] waits for either:

        - one of the file descriptor of [fds_r] to become readable
        - one of the file descriptor of [fds_w] to become writable
        - timeout to expire

        and returns the list of readable file descriptor and the list
        of writable file descriptors. *)
end

(** Abstract class for engines based on a poll-like function. *)
class virtual poll_based : object
  inherit t

  method private virtual poll : (Unix.file_descr * bool * bool) list -> float -> (Unix.file_descr * bool * bool) list
    (** [poll fds tiomeout], where [fds] is a list of tuples of the
        form [(fd, check_readable, check_writable)], waits for either:

        - one of the file descriptor with [check_readable] set to
          [true] to become readable
        - one of the file descriptor with [check_writable] set to
          [true] to become writable
        - timeout to expire

        and returns the list of file descriptors with their readable
        and writable status. *)
end

(** {2 The current engine} *)

val get : unit -> t
  (** [get ()] returns the engine currently in use. *)

val set : ?transfer : bool -> ?destroy : bool -> #t -> unit
  (** [set ?transfer ?destroy engine] replaces the current engine by
      the given one.

      If [transfer] is [true] (the default) all events from the
      current engine are transferred to the new one.

      If [destroy] is [true] (the default) then the current engine is
      destroyed before being replaced. *)

module Versioned :
sig
  class libev_1 : object
    inherit t
    val loop : ev_loop
    method loop : ev_loop
  end
  [@@ocaml.deprecated
" Deprecated in favor of Lwt_engine.libev. See
  https://github.com/ocsigen/lwt/pull/269"]
  (** Old version of {!Lwt_engine.libev}. The current {!Lwt_engine.libev} allows
      selecting the libev back end.

      @deprecated Use {!Lwt_engine.libev}.
      @since 2.7.0 *)

  class libev_2 : ?backend:Ev_backend.t -> unit -> object
    inherit t
    val loop : ev_loop
    method loop : ev_loop
  end
  [@@ocaml.deprecated
" In Lwt >= 3.0.0, this is an alias for Lwt_engine.libev."]
  (** Since Lwt 3.0.0, this is just an alias for {!Lwt_engine.libev}.

      @deprecated Use {!Lwt_engine.libev}.
      @since 2.7.0 *)
end
