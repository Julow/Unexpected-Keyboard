# Lwt &nbsp;&nbsp; [![version 4.4.0][version]][releases] [![Travis status][travis-img]][travis] [![AppVeyor status][appveyor-img]][appveyor]

[version]:      https://img.shields.io/badge/version-4.4.0-blue.svg
[releases]:     https://github.com/ocsigen/lwt/releases
[travis]:       https://travis-ci.org/ocsigen/lwt/branches
[travis-img]:   https://img.shields.io/travis/ocsigen/lwt/master.svg?label=travis
[appveyor]:     https://ci.appveyor.com/project/aantron/lwt/branch/master
[appveyor-img]: https://img.shields.io/appveyor/ci/aantron/lwt/master.svg?label=appveyor

Lwt is OCaml's concurrent programming library. It provides a single data type:
the *promise*, which is a value that will become determined in the future.
Creating a promise spawns a computation. When that computation is I/O, Lwt runs
it in parallel with your OCaml code.

OCaml code, including creating and waiting on promises, is run in a single
thread by default, so you don't have to worry about locking or preemption. You
can detach code to be run in separate threads on an opt-in basis.

Here is a simplistic Lwt program which requests the Google front page, and fails
if the request is not completed in five seconds:

```ocaml
let () =
  let request =
    let%lwt addresses = Lwt_unix.getaddrinfo "google.com" "80" [] in
    let google = Lwt_unix.((List.hd addresses).ai_addr) in

    Lwt_io.(with_connection google (fun (incoming, outgoing) ->
      let%lwt () = write outgoing "GET / HTTP/1.1\r\n" in
      let%lwt () = write outgoing "Connection: close\r\n\r\n" in
      let%lwt response = read incoming in
      Lwt.return (Some response)))
  in

  let timeout =
    let%lwt () = Lwt_unix.sleep 5. in
    Lwt.return None
  in

  match Lwt_main.run (Lwt.pick [request; timeout]) with
  | Some response -> print_string response
  | None -> prerr_endline "Request timed out"; exit 1

(* ocamlfind opt -package lwt.unix,lwt_ppx -linkpkg -o request example.ml
   ./request *)
```

In the program, functions such as `Lwt_io.write` create promises. The
`let%lwt ... in` construct is used to wait for a promise to become determined;
the code after `in` is scheduled to run in a "callback." `Lwt.pick` races
promises against each other, and behaves as the first one to complete.
`Lwt_main.run` forces the whole promise-computation network to be executed. All
the visible OCaml code is run in a single thread, but Lwt internally uses a
combination of worker threads and non-blocking file descriptors to resolve in
parallel the promises that do I/O.


<br/>

### Overview

Lwt compiles to native code on Linux, macOS, Windows, and other systems. It's
also routinely compiled to JavaScript for the front end and Node, by js_of_ocaml
and BuckleScript.

In Lwt,

- The [core library `Lwt`][core] provides promises...
- ...and a few pure-OCaml helpers, such as promise-friendly [mutexes][mutex],
  [condition variables][cond], and [mvars][mvar].
- There is a big Unix binding, [`Lwt_unix`][unix] that binds almost every Unix
  system call. A higher-level module [`Lwt_io`][io] provides nice I/O channels.
- [`Lwt_process`][process] is for subprocess handling.
- [`Lwt_preemptive`][preemptive] spawns system threads.
- The [PPX syntax][ppx] allows using all of the above without going crazy!
- There are also some other helpers, such as [`Lwt_react`][react] for reactive
  programming. See the table of contents on the linked manual pages!

[core]: https://ocsigen.org/lwt/api/Lwt
[cond]: https://ocsigen.org/lwt/api/Lwt_condition
[mutex]: https://ocsigen.org/lwt/api/Lwt_mutex
[mvar]: https://ocsigen.org/lwt/api/Lwt_mvar
[unix]: https://ocsigen.org/lwt/api/Lwt_unix
[io]: https://ocsigen.org/lwt/api/Lwt_io
[process]: https://ocsigen.org/lwt/api/Lwt_process
[preemptive]: https://ocsigen.org/lwt/api/Lwt_preemptive
[ppx]: https://ocsigen.org/lwt/api/Ppx_lwt
[react]: https://ocsigen.org/lwt/api/Lwt_react


<br/>

## Installing

1. Use your system package manager to install a development libev package.
   It is often called `libev-dev` or `libev-devel`.
2. `opam install conf-libev lwt`


<br/>

## Documentation

We are currently working on improving the Lwt documentation (drastically; we are
rewriting the manual). In the meantime:

- The current manual can be found [here][manual].
- Mirage has a nicely-written [Lwt tutorial][mirage-tutorial].
- An example of a [simple server][counter-server] written in Lwt.
- [Concurrent Programming with Lwt][rwo-lwt] is a nice source of Lwt examples.
  They are translations of code from the excellent Real World OCaml, but are
  just as useful if you are not reading the book.

*Note: much of the current manual refers to `'a Lwt.t` as "lightweight threads"
or just "threads." This will be fixed in the new manual. `'a Lwt.t` is a
promise, and has nothing to do with system or preemptive threads.*

[manual]:   http://ocsigen.org/lwt/
[rwo-lwt]:  https://github.com/dkim/rwo-lwt#readme
[mirage-tutorial]: https://mirage.io/wiki/tutorial-lwt
[counter-server]: http://www.baturin.org/code/lwt-counter-server/


<br/>

## Contact

Open an [issue][issues], visit [Discord][discord] chat, ask on
[discuss.ocaml.org][discourse], or on [Stack Overflow][so].

Release announcements are made in [/r/ocaml][reddit], and on
[discuss.ocaml.org][discourse]. Watching the repo for "Releases only" is also an
option.

[irc]:    http://webchat.freenode.net/?channels=#ocaml
[so]:     http://stackoverflow.com/questions/ask?tags=ocaml,lwt,ocaml-lwt
[announcements]: https://github.com/ocsigen/lwt/issues/309
[reddit]: https://www.reddit.com/r/ocaml/
[caml-list]: https://sympa.inria.fr/sympa/arc/caml-list
[discourse]: https://discuss.ocaml.org/c/lwt
[issues]: https://github.com/ocsigen/lwt/issues/new
[discord]: https://discordapp.com/invite/cCYQbqN


<br/>

## Contributing

- [`CONTRIBUTING.md`][contributing-md] contains tips for working on the code,
  such as how to check the code out, how review works, etc. There is also a
  high-level outline of the code base.
- [Ask](#contact) us anything, whether it's about working on Lwt, or any
  question at all about it :)
- The [documentation](#documentation) always needs proofreading and fixes.
- You are welcome to pick up any other [issue][issues-and-prs], review a PR, add
  your opinion, etc.
- Any feedback is welcome, including how to make contributing easier!

[issues-and-prs]: https://github.com/ocsigen/lwt/issues?utf8=%E2%9C%93&q=is%3Aopen
[all-issues]: https://github.com/ocsigen/lwt/issues
[contributing-md]: https://github.com/ocsigen/lwt/blob/master/docs/CONTRIBUTING.md#readme


<br/>

## Libraries to use with Lwt

- [alcotest](https://github.com/mirage/alcotest/) —
unit testing
- [angstrom](https://github.com/inhabitedtype/angstrom) —
parser combinators
- [cohttp](https://github.com/mirage/ocaml-cohttp) — HTTP client and server
- [cstruct](https://github.com/mirage/ocaml-cstruct) —
interop with C-like structures
- [ezjsonm](https://github.com/mirage/ezjsonm) —
JSON parsing and output
- [faraday](https://github.com/inhabitedtype/faraday) —
serialization combinators
- [logs](https://github.com/dbuenzli/logs) —
logging
- [lwt-parallel](https://github.com/ivg/parallel) —
distributed computing
- [mwt](https://github.com/hcarty/mwt) — preemptive (system) thread pools
- [opium](https://github.com/rgrinberg/opium) —
web framework
