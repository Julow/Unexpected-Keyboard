(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



(** Lwt_unix feature discovery script.

    This program tests system features, and outputs four files:

    - [src/unix/lwt_features.h]: feature test results for consumption by C code.
    - [src/unix/lwt_features.ml]: test results for consumption by OCaml code.
    - [src/unix/unix_c_flags.sexp]: C compiler flags for Lwt_unix C sources.
    - [src/unix/unix_c_library_flags.sexp]: C linker flags for Lwt_unix.

    [src/unix/lwt_features.h] contains only basic [#define] macros. It is
    included in [src/unix/lwt_config.h], which computes a few more useful
    macros. [src/unix/lwt_config.h] is the file that is then directly included
    by all the C sources.

    [src/unix/lwt_features.ml] is included by [src/unix/lwt_config.ml] in the
    same way.

    You can examine the generated [lwt_features.h] by running [dune build] and
    looking in [_build/default/src/unix/lwt_features.h], and similarly for
    [lwt_features.ml].

    This program tries to detect everything automatically. If it is not behaving
    correctly, its behavior can be tweaked by passing it arguments. There are
    three ways to do so:

    - By editing [src/unix/dune], to pass arguments to [discover.exe] on the
      command line.
    - By setting the environment variable [LWT_DISCOVER_ARGUMENTS]. The syntax
      is the same as the command line.
    - By writing a file [src/unix/discover_arguments]. The syntax is again the
      same as the command line.

    The possible arguments can be found by running

    {v
      dune exec src/unix/config/discover.exe -- --help
    v}

    In addition, the environment variables [LIBEV_CFLAGS], [LIBEV_LIBS],
    [PTHREAD_CFLAGS], and [PTHREAD_LIBS] can be used to override the flags used
    for compiling with libev and pthreads.

    This [discover.ml] was added in Lwt 4.3.0, so if you pass arguments to
    [discover.ml], 4.3.0 is the minimal required version of Lwt.


    The code is broken up into sections, each of which is an OCaml module. If
    your text editor supports code folding, it will make reading this file much
    easier if you fold the structures.

    Add feature tests at the end of module [Features]. For most cases, what to
    do should be clear from the feature tests that are already in that
    module. *)



module Configurator = Configurator.V1
let split = Configurator.Flags.extract_blank_separated_words
let uppercase = String.uppercase [@ocaml.warning "-3"]



(* Command-line arguments and environment variables. *)
module Arguments :
sig
  val use_libev : bool option ref
  val use_pthread : bool option ref
  val android_target : bool option ref
  val libev_default : bool option ref
  val verbose : bool ref

  val args : (Arg.key * Arg.spec * Arg.doc) list

  val parse_environment_variable : unit -> unit
  val parse_arguments_file : unit -> unit
end =
struct
  let use_libev = ref None
  let use_pthread = ref None
  let android_target = ref None
  let libev_default = ref None
  let verbose = ref false

  let set reference =
    Arg.Bool (fun value -> reference := Some value)

  let args = [
    "--use-libev", set use_libev,
      "BOOLEAN whether to check for libev";

    "--use-pthread", set use_pthread,
      "BOOLEAN whether to check for libpthread";

    "--android-target", set android_target,
      "BOOLEAN whether to compile for Android";

    "--libev-default", set libev_default,
      "BOOLEAN whether to use the libev backend by default";
  ]

  let environment_variable = "LWT_DISCOVER_ARGUMENTS"
  let arguments_file = "discover_arguments"

  let parse arguments =
    try
      Arg.parse_argv
        ~current:(ref 0)
        (Array.of_list ((Filename.basename Sys.argv.(0))::(split arguments)))
        (Arg.align args)
        (fun s ->
          raise (Arg.Bad (Printf.sprintf "Unrecognized argument '%s'" s)))
        (Printf.sprintf
          "Environment variable usage: %s=[OPTIONS]" environment_variable)
    with
    | Arg.Bad s ->
      prerr_string s;
      exit 2
    | Arg.Help s ->
      print_string s;
      exit 0

  let parse_environment_variable () =
    match Sys.getenv environment_variable with
    | exception Not_found ->
      ()
    | arguments ->
      parse arguments

  let parse_arguments_file () =
    try
      let channel = open_in arguments_file in
      parse (input_line channel);
      close_in channel
    with _ ->
      ()
end



module C_library_flags :
sig
  val detect :
    ?env_var:string ->
    ?package:string ->
    ?header:string ->
    Configurator.t ->
    library:string ->
      unit

  val ws2_32_lib : Configurator.t -> unit

  val c_flags : unit -> string list
  val link_flags : unit -> string list
  val add_link_flags : string list -> unit
end =
struct
  let c_flags = ref []
  let link_flags = ref []

  let extend c_flags' link_flags' =
    c_flags := !c_flags @ c_flags';
    link_flags := !link_flags @ link_flags'

  let add_link_flags flags =
    extend [] flags

  let (//) = Filename.concat

  let default_search_paths = [
    "/usr";
    "/usr/local";
    "/usr/pkg";
    "/opt";
    "/opt/local";
    "/sw";
    "/mingw";
  ]

  let path_separator =
    if Sys.win32 then
      ';'
    else
      ':'

  let paths_from_environment_variable variable =
    match Sys.getenv variable with
    | exception Not_found ->
      []
    | paths ->
      Configurator.Flags.extract_words paths ~is_word_char:((<>) path_separator)
      |> List.map Filename.dirname

  let search_paths =
    lazy begin
      paths_from_environment_variable "C_INCLUDE_PATH" @
      paths_from_environment_variable "LIBRARY_PATH" @
      default_search_paths
    end

  let default argument fallback =
    match argument with
    | Some value -> value
    | None -> fallback

  let detect ?env_var ?package ?header context ~library =
    let env_var = default env_var ("LIB" ^ uppercase library) in
    let package = default package ("lib" ^ library) in
    let header = default header (library ^ ".h") in

    let flags_from_env_var =
      let c_flags_var = env_var ^ "_CFLAGS" in
      let link_flags_var = env_var ^ "_LIBS" in
      match Sys.getenv c_flags_var, Sys.getenv link_flags_var with
      | exception Not_found ->
        None
      | "", "" ->
        None
      | values ->
        Some values
    in

    match flags_from_env_var with
    | Some (c_flags', link_flags') ->
      extend (split c_flags') (split link_flags')

    | None ->
      let flags_from_pkg_config =
        match Configurator.Pkg_config.get context with
        | None ->
          None
        | Some pkg_config ->
          Configurator.Pkg_config.query pkg_config ~package
      in

      match flags_from_pkg_config with
      | Some flags ->
        extend flags.cflags flags.libs

      | None ->
        try
          let path =
            List.find
              (fun path -> Sys.file_exists (path // "include" // header))
              (Lazy.force search_paths)
          in
          extend
            ["-I" ^ (path // "include")]
            ["-L" ^ (path // "lib"); "-l" ^ library]
        with Not_found ->
          extend [] ["-l" ^ library]

  let ws2_32_lib context =
    if Configurator.ocaml_config_var_exn context "os_type" = "Win32" then
      if Configurator.ocaml_config_var_exn context "ccomp_type" = "msvc" then
        extend [] ["ws2_32.lib"]
      else
        extend [] ["-lws2_32"]

  let c_flags () =
    !c_flags

  let link_flags () =
    !link_flags
end



module Output :
sig
  type t = {
    name : string;
    found : bool;
  }

  val write_c_header : ?extra:string list -> Configurator.t -> t list -> unit
  val write_ml_file : ?extra:t list -> t list -> unit
  val write_flags_files : unit -> unit
end =
struct
  type t = {
    name : string;
    found : bool;
  }

  module C_define = Configurator.C_define

  let c_header = "lwt_features.h"
  let ml_file = "lwt_features.ml"
  let c_flags_file = "unix_c_flags.sexp"
  let link_flags_file = "unix_c_library_flags.sexp"

  let write_c_header ?(extra = []) context macros =
    macros
    |> List.filter (fun {found; _} -> found)
    |> List.map (fun {name; _} -> name, C_define.Value.Switch true)
    |> (@) (List.map (fun s -> s, C_define.Value.Switch true) extra)
    |> C_define.gen_header_file context ~fname:c_header

  let write_ml_file ?(extra = []) macros =
    macros
    |> List.map (fun {name; found} -> Printf.sprintf "let _%s = %b" name found)
    |> (@) (List.map
      (fun {name; found} -> Printf.sprintf "let %s = %b" name found) extra)
    |> Configurator.Flags.write_lines ml_file

  let write_flags_files () =
    Configurator.Flags.write_sexp
      c_flags_file (C_library_flags.c_flags ());
    Configurator.Flags.write_sexp
      link_flags_file (C_library_flags.link_flags ());
end



module Features :
sig
  val detect : Configurator.t -> Output.t list
end =
struct
  type t = {
    pretty_name : string;
    macro_name : string;
    detect : Configurator.t -> bool option;
  }

  let features = ref []

  let feature the_feature =
    features := !features @ [the_feature]

  let verbose =
    Printf.ksprintf (fun s ->
      if !Arguments.verbose then
        print_string s)

  let dots feature to_column =
    String.make (to_column - String.length feature.pretty_name) '.'

  let right_column = 40

  let detect context =
    !features
    |> List.map begin fun feature ->
      verbose "%s " feature.pretty_name;
      match feature.detect context with
      | None ->
        verbose "%s skipped\n" (dots feature right_column);
        Output.{name = feature.macro_name; found = false}
      | Some found ->
        begin
          if found then
            verbose "%s available\n" (dots feature (right_column - 2))
          else
            verbose "%s unavailable\n" (dots feature (right_column - 4))
        end;
        Output.{name = feature.macro_name; found}
    end

  let compiles ?(werror = false) ?(link_flags = []) context code =
    let c_flags = C_library_flags.c_flags () in
    let c_flags =
      if werror then
        "-Werror"::c_flags
      else
        c_flags
    in
    let link_flags = link_flags @ (C_library_flags.link_flags ()) in
    Configurator.c_test context ~c_flags ~link_flags code
    |> fun result -> Some result

  let skip_if_windows context k =
    match Configurator.ocaml_config_var_exn context "os_type" with
    | "Win32" -> None
    | _ -> k ()

  let skip_if_android _context k =
    match !Arguments.android_target with
    | Some true -> None
    | _ -> k ()

  let () = feature {
    pretty_name = "libev";
    macro_name = "HAVE_LIBEV";
    detect = fun context ->
      let detect_opam_conf_libev () =
        let result =
          Configurator.Process.run context
            "opam" ["config"; "var"; "conf-libev:installed"]
        in
        match result.stdout with
        | "true\n" -> true
        | _ -> false
      in

      let detect_esy_wants_libev () =
        match Sys.getenv "LIBEV_CFLAGS", Sys.getenv "LIBEV_LIBS" with
        | exception Not_found -> false
        | "", "" -> false
        | _ -> true
      in

      let should_look_for_libev =
        match !Arguments.use_libev with
        | Some argument ->
          argument
        | None ->
          try detect_opam_conf_libev ()
          with _ ->
            if detect_esy_wants_libev () then
              true
            else begin
              let os = Configurator.ocaml_config_var_exn context "os_type" in
              os <> "Win32" && !Arguments.android_target <> Some true
            end
      in

      if not should_look_for_libev then
        None
      else begin
        let code = {|
          #include <ev.h>

          int main()
          {
              ev_default_loop(0);
              return 0;
          }
        |}
        in
        match compiles context code ~link_flags:["-lev"] with
        | Some true ->
          C_library_flags.add_link_flags ["-lev"];
          Some true
        | _ ->
          C_library_flags.detect context ~library:"ev";
          compiles context code
      end
  }

  let () = feature {
    pretty_name = "pthread";
    macro_name = "HAVE_PTHREAD";
    detect = fun context ->
      if !Arguments.use_pthread = Some false then
        None
      else begin
        skip_if_windows context @@ fun () ->
        let code = {|
          #include <pthread.h>

          int main()
          {
              pthread_create(0, 0, 0, 0);
              return 0;
          }
        |}
        in
        match compiles context code with
        | Some true -> Some true
        | no ->
          if !Arguments.android_target = Some true then
            no
          else begin
            C_library_flags.detect context ~library:"pthread";
            compiles context code
          end
      end
  }

  let () = feature {
    pretty_name = "eventfd";
    macro_name = "HAVE_EVENTFD";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      compiles context {|
        #include <sys/eventfd.h>

        int main()
        {
            eventfd(0, 0);
            return 0;
        }
      |}
  }

  let () = feature {
    pretty_name = "fd passing";
    macro_name = "HAVE_FD_PASSING";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      compiles context {|
        #include <sys/types.h>
        #include <sys/socket.h>

        int main()
        {
            struct msghdr msg;
            msg.msg_controllen = 0;
            msg.msg_control = 0;
            return 0;
        }
      |}
  }

  let () = feature {
    pretty_name = "sched_getcpu";
    macro_name = "HAVE_GETCPU";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      skip_if_android context @@ fun () ->
      compiles context {|
        #define _GNU_SOURCE
        #include <sched.h>

        int main()
        {
            sched_getcpu();
            return 0;
        }
      |}
  }

  let () = feature {
    pretty_name = "affinity getting/setting";
    macro_name = "HAVE_AFFINITY";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      skip_if_android context @@ fun () ->
      compiles context {|
        #define _GNU_SOURCE
        #include <sched.h>

        int main()
        {
            sched_getaffinity(0, 0, 0);
            return 0;
        }
      |}
  }

  let get_credentials struct_name = {|
    #define _GNU_SOURCE
    #include <sys/types.h>
    #include <sys/socket.h>

    int main()
    {
        struct |} ^ struct_name ^ {| cred;
        socklen_t cred_len = sizeof(cred);
        getsockopt(0, SOL_SOCKET, SO_PEERCRED, &cred, &cred_len);
        return 0;
    }
  |}

  let () = feature {
    pretty_name = "credentials getting (Linux)";
    macro_name = "HAVE_GET_CREDENTIALS_LINUX";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      compiles context (get_credentials "ucred")
  }

  let () = feature {
    pretty_name = "credentials getting (NetBSD)";
    macro_name = "HAVE_GET_CREDENTIALS_NETBSD";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      compiles context (get_credentials "sockcred")
  }

  let () = feature {
    pretty_name = "credentials getting (OpenBSD)";
    macro_name = "HAVE_GET_CREDENTIALS_OPENBSD";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      compiles context (get_credentials "sockpeercred")
  }

  let () = feature {
    pretty_name = "credentials getting (FreeBSD)";
    macro_name = "HAVE_GET_CREDENTIALS_FREEBSD";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      compiles context (get_credentials "cmsgcred")
  }

  let () = feature {
    pretty_name = "getpeereid";
    macro_name = "HAVE_GETPEEREID";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      compiles context {|
        #include <sys/types.h>
        #include <unistd.h>

        int main()
        {
            uid_t euid;
            gid_t egid;
            getpeereid(0, &euid, &egid);
            return 0;
        }
      |}
  }

  let () = feature {
    pretty_name = "fdatasync";
    macro_name = "HAVE_FDATASYNC";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      compiles context {|
        #include <unistd.h>

        int main()
        {
            int (*fdatasyncp)(int) = fdatasync;
            fdatasyncp(0);
            return 0;
        }
      |}
  }

  let () = feature {
    pretty_name = "netdb_reentrant";
    macro_name = "HAVE_NETDB_REENTRANT";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      skip_if_android context @@ fun () ->
      compiles context {|
        #define _POSIX_PTHREAD_SEMANTICS
        #include <netdb.h>
        #include <stddef.h>

        int main()
        {
            struct hostent *he;
            struct servent *se;
            he =
              gethostbyname_r(
                (const char*)NULL,
                (struct hostent*)NULL,
                (char*)NULL,
                (int)0,
                (struct hostent**)NULL,
                (int*)NULL);
            he =
              gethostbyaddr_r(
                (const char*)NULL,
                (int)0,
                (int)0,
                (struct hostent*)NULL,
                (char*)NULL,
                (int)0,
                (struct hostent**)NULL,
                (int*)NULL);
            se =
              getservbyname_r(
                (const char*)NULL,
                (const char*)NULL,
                (struct servent*)NULL,
                (char*)NULL,
                (int)0,
                (struct servent**)NULL);
            se =
              getservbyport_r(
                (int)0,
                (const char*)NULL,
                (struct servent*)NULL,
                (char*)NULL,
                (int)0,
                (struct servent**)NULL);
            pr =
              getprotoent_r(
                (struct protoent*)NULL,
                (char*)NULL,
                (int)0,
                (struct protoent**)NULL);
            pr =
              getprotobyname_r(
                (const char*)NULL,
                (struct protoent*)NULL,
                (char*)NULL,
                (int)0,
                (struct protoent**)NULL);
            pr =
              getprotobynumber_r(
                (int)0,
                (struct protoent*)NULL,
                (char*)NULL,
                (int)0,
                (struct protoent**)NULL);

            return 0;
        }
      |}
  }

  let () = feature {
    pretty_name = "reentrant gethost*";
    macro_name = "HAVE_REENTRANT_HOSTENT";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      compiles context {|
        #define _GNU_SOURCE
        #include <stddef.h>
        #include <caml/config.h>
        /* Helper functions for not re-entrant functions */
        #if !defined(HAS_GETHOSTBYADDR_R) || \
            (HAS_GETHOSTBYADDR_R != 7 && HAS_GETHOSTBYADDR_R != 8)
        #define NON_R_GETHOSTBYADDR 1
        #endif

        #if !defined(HAS_GETHOSTBYNAME_R) || \
            (HAS_GETHOSTBYNAME_R != 5 && HAS_GETHOSTBYNAME_R != 6)
        #define NON_R_GETHOSTBYNAME 1
        #endif

        int main()
        {
        #if defined(NON_R_GETHOSTBYNAME) || defined(NON_R_GETHOSTBYNAME)
        #error \"not available\"
        #else
            return 0;
        #endif
        }
      |}
  }

  let nanosecond_stat projection = {|
    #define _GNU_SOURCE
    #include <sys/types.h>
    #include <sys/stat.h>
    #include <unistd.h>

    #define NANOSEC" ^ conversion ^ "

    int main() {
        struct stat *buf;
        double a, m, c;
        a = (double)buf->st_a|} ^ projection ^ {|;
        m = (double)buf->st_m|} ^ projection ^ {|;
        c = (double)buf->st_c|} ^ projection ^ {|;
        return 0;
    }
  |}

  let () = feature {
    pretty_name = "st_mtim.tv_nsec";
    macro_name = "HAVE_ST_MTIM_TV_NSEC";
    detect = fun context ->
      compiles context (nanosecond_stat "tim.tv_nsec")
  }

  let () = feature {
    pretty_name = "st_mtimespec.tv_nsec";
    macro_name = "HAVE_ST_MTIMESPEC_TV_NSEC";
    detect = fun context ->
      compiles context (nanosecond_stat "timespec.tv_nsec")
  }

  let () = feature {
    pretty_name = "st_mtimensec";
    macro_name = "HAVE_ST_MTIMENSEC";
    detect = fun context ->
      compiles context (nanosecond_stat "timensec")
  }

  let () = feature {
    pretty_name = "BSD mincore";
    macro_name = "HAVE_BSD_MINCORE";
    detect = fun context ->
      skip_if_windows context @@ fun () ->
      compiles ~werror:true context {|
        #include <unistd.h>
        #include <sys/mman.h>

        int main()
        {
            int (*mincore_ptr)(const void*, size_t, char*) = mincore;
            return (int)(mincore_ptr == NULL);
        }
      |}
  }
end



let () =
  Configurator.main ~args:Arguments.args ~name:"lwt" begin fun context ->
    (* Parse arguments from additional sources. *)
    Arguments.parse_environment_variable ();
    Arguments.parse_arguments_file ();

    (* Detect features. *)
    let macros = Features.detect context in

    (* Link with ws2_32.lib on Windows. *)
    C_library_flags.ws2_32_lib context;

    (* Write lwt_features.h. *)
    let extra =
      match Configurator.ocaml_config_var_exn context "os_type" with
      | "Win32" -> ["LWT_ON_WINDOWS"]
      | _ -> []
    in
    Output.write_c_header ~extra context macros;

    (* Write lwt_features.ml. *)
    let libev_default =
      try
        Sys.getenv "LWT_FORCE_LIBEV_BY_DEFAULT" = "yes"
      with Not_found ->
        match !Arguments.libev_default with
        | Some argument ->
          argument
        | None ->
          match Configurator.ocaml_config_var_exn context "system" with
          | "linux"
          | "linux_elf"
          | "linux_aout"
          | "linux_eabi"
          | "linux_eabihf" ->
            true
          | _ ->
            false
    in
    Output.write_ml_file
      ~extra:[
        {
          name = "android";
          found = !Arguments.android_target = Some true;
        };
        {
          name = "libev_default";
          found = libev_default;
        };
      ] macros;

    (* Write unix_c_flags.sexp and unix_c_library_flags.sexp. *)
    Output.write_flags_files ()
  end
