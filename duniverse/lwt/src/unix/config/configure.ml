let use_libev = ref None
let use_pthread = ref None
let android_target = ref None
let libev_default = ref None
let default_configuration = ref None

let arg_bool r =
  Arg.Symbol (["true"; "false"],
              function
              | "true" -> r := Some true
              | "false" -> r := Some false
              | _ -> assert false)

let usage =
  "Enable lwt.unix features\n" ^
  "This script is deprecated, and will be removed in Lwt 5.0.0.\n" ^
  "See comment in src/unix/config/discover.ml.\n" ^
  "Options are:"

let args = [
  "-use-libev", arg_bool use_libev,
    " whether to check for libev";
  "-use-pthread", arg_bool use_pthread,
    " whether to use pthread";
  "-android-target", arg_bool android_target,
    " compile for Android";
  "-libev-default", arg_bool libev_default,
    " whether to use the libev backend by default";
  "-default-configuration", arg_bool default_configuration,
    " generate a default configuration for a typical opam install"
]

let () =
  Arg.parse args ignore usage;

  prerr_endline
    "configure.exe is deprecated, and will be removed in Lwt 5.0.0.";
  prerr_endline
    "See comment in src/unix/config/discover.ml for new usage.";

  let f = open_out "discover_arguments" in
  let print name var =
    match var with
    | None -> ()
    | Some var -> Printf.fprintf f "--%s %b " name var
  in
  print "use-libev" !use_libev;
  print "use-pthread" !use_pthread;
  print "android-target" !android_target;
  print "libev-default" !libev_default;
  close_out f
